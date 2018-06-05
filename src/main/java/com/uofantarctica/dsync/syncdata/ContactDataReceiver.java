package com.uofantarctica.dsync.syncdata;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uofantarctica.dsync.DSync;
import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.model.ChatMessageInbox;
import com.uofantarctica.dsync.model.ChatMessageOutbox;
import com.uofantarctica.dsync.model.ChatbufProto;
import com.uofantarctica.dsync.model.SyncState;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.util.Blob;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactDataReceiver implements OnData, OnTimeout {
	private static final String TAG = ContactDataReceiver.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final DSync dsync;
	private final OnData onData;
	private final SyncState syncState;
	private final DSyncReporting dSyncReporting;
	private final ChatMessageInbox inbox;
	private boolean silenced = false;
	private long highestReceivedDataSegment = 0;

	public ContactDataReceiver(DSync dsync, OnData onData, SyncState syncState, DSyncReporting dSyncReporting, ChatMessageInbox inbox) {
		this.dsync = dsync;
		this.onData = onData;
		this.syncState = syncState;
		this.dSyncReporting = dSyncReporting;
		this.inbox = inbox;
	}

	@Override
	public void onData(Interest interest, Data data) {
		if (silenced)
			return;

		try {
			dSyncReporting.onDataPrefixOnData(interest, data);
			if (inbox.isBlocking()) {
				if (inbox.add(data)) {
					for (Data orderedData : inbox.getSorted()) {
						//TODO is this the right interest, does it matter?
						onData.onData(interest, orderedData);
					}
				}
			}
			else {
				passToOnData(interest, data);
                incSyncStateToHighestReceived(data);
                dsync.expressInterestInDataSuffix(syncState);
                passToOnData(interest, data);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onData");
		}
	}

	private void incSyncStateToHighestReceived(Data data) {
		try {
			ChatbufProto.ChatMessageList list
				= ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());
			long currMax = list.getHighestSeq();

			if (currMax > highestReceivedDataSegment)
				highestReceivedDataSegment = currMax;
				syncState.setSeq(highestReceivedDataSegment + 1);

		} catch (InvalidProtocolBufferException e) {
			log.log(Level.SEVERE, "Failed to properly increment sync state.");
		}
	}

	private void passToOnData(Interest interest, Data data) {
		try {
			ChatbufProto.ChatMessageList messages
				= ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());

			for(ChatbufProto.ChatMessage m : messages.getMessageListList()) {
				Data newData = new Data(data.getName());
				ChatbufProto.ChatMessage.Builder builder = ChatbufProto.ChatMessage.newBuilder();
				ChatMessageOutbox.buildMessage(builder, m.getFrom(), m.getTo(), m.getType(), m.getData(), m.getTimestamp());
				newData.setContent(new Blob(builder.build().toByteArray()));
				onData.onData(interest, newData);
			}
		} catch (InvalidProtocolBufferException e) {
			log.log(Level.SEVERE, "failed to decode protocol buffer.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "failed to pass chat message list to on data.");
		}
	}

	@Override
	public void onTimeout(Interest interest) {
		if (silenced)
			return;

		try {
			dsync.expressInterestInDataSuffix(syncState);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onTimeout");
		}
	}

	public boolean isBlocking() {
		return inbox.isBlocking();
	}

	public void silence() throws Exception {
		if (isBlocking()){
			throw new Exception("Can not silence a blocking contact data receiver.");
			//TODO here there is the potential to fail to get newer data. Situation arises if amount of recovery
			// is high and we are waiting on the network for all of the different packets, theoretically, this should
			// be mitigated if when we issue blocking data calls to backfill old/missed data, we also issue non blocking
			// data calls for the most recent data available.
		}
		else {
			silenced = true;
		}
	}
}
