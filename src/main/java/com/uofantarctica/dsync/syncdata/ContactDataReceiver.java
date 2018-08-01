package com.uofantarctica.dsync.syncdata;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uofantarctica.dsync.DSync;
import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.model.ChatMessageBox;
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
	private final SyncState s;
	private final DSyncReporting dSyncReporting;
	private final List<Integer> skippedSyncStates = new ArrayList<>();
	private long highestReceivedDataSegment = 0;

	public ContactDataReceiver(DSync dsync, OnData onData, SyncState s, DSyncReporting dSyncReporting) {
		this.dsync = dsync;
		this.onData = onData;
		this.s = s;
		this.dSyncReporting = dSyncReporting;
	}

	@Override
	public void onData(Interest interest, Data data) {
		try {
			dSyncReporting.onDataPrefixOnData(interest, data);
			incSyncStateToHighestReceived(interest);
			dsync.expressInterestInDataSuffix(s);
			//passToOnData(interest, data);
			onData.onData(interest, data);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onData");
		}
	}

	private void incSyncStateToHighestReceived(Interest interest) {
			/*
			ChatbufProto.ChatMessageList list
				= ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());
			long currMax = list.getHighestSeq();
			if (currMax > highestReceivedDataSegment) {
				highestReceivedDataSegment = currMax;
				s.setSeq(highestReceivedDataSegment + 1);
			}
			*/
			long seqNo = Long.parseLong(interest.getName().get(-1).toEscapedString());
			s.setSeq(seqNo + 1);
	}

	/*
	private void passToOnData(Interest interest, Data data) {
		try {
			ChatbufProto.ChatMessageList messages
				= ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());

			for(ChatbufProto.ChatMessage m : messages.getMessageListList()) {
				Data newData = new Data(data.getName());
				ChatbufProto.ChatMessage.Builder builder = ChatbufProto.ChatMessage.newBuilder();
				ChatMessageBox.buildMessage(builder, m.getFrom(), m.getTo(), m.getType(), m.getData(), m.getTimestamp());
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
	*/

	@Override
	public void onTimeout(Interest interest) {
		try {
			dsync.expressInterestInDataSuffix(s);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onTimeout");
		}
	}
}
