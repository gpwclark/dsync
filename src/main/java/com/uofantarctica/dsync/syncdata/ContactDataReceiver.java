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
	private final SyncState currSyncStateOfContact;
	private final DSyncReporting dSyncReporting;
	private final List<Integer> skippedSyncStates = new ArrayList<>();
	private long highestReceivedDataSegment = 0;
	private boolean enabled;

	public ContactDataReceiver(DSync dsync, OnData onData, SyncState currSyncStateOfContact, DSyncReporting dSyncReporting) {
		this.dsync = dsync;
		this.onData = onData;
		this.currSyncStateOfContact = currSyncStateOfContact;
		this.dSyncReporting = dSyncReporting;
		this.enabled = true;
	}

	public void stopExpressingInterestInContactData() {
		enabled = false;
	}

	@Override
	public void onData(Interest interest, Data data) {
		try {
			dSyncReporting.onDataPrefixOnData(interest, data);
			incSyncStateToHighestReceived(data);
			getNextDataSegmentFromContact();
			passToOnData(interest, data);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onData", e);
		}
	}

	private void getNextDataSegmentFromContact() {
		if (enabled) {
			Interest interest = this.currSyncStateOfContact.getInterest();
			dsync.expressInterest(interest, this, this);
		}
	}

	private void incSyncStateToHighestReceived(Data data) {
		try {
			ChatbufProto.ChatMessageList list
				= ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());
			long receivedMax = list.getHighestSeq();

			if (receivedMax >= highestReceivedDataSegment) {
				highestReceivedDataSegment = receivedMax;
				currSyncStateOfContact.setSeq(highestReceivedDataSegment + 1);
			}

		} catch (InvalidProtocolBufferException e) {
			log.log(Level.SEVERE, "Failed to properly increment sync state.", e);
		}
	}

	private void passToOnData(Interest interest, Data data) {
		try {
			ChatbufProto.ChatMessageList messages
				= ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());

			for(ChatbufProto.ChatMessage m : messages.getMessageListList()) {
				Data newData = new Data(data.getName());
				ChatbufProto.ChatMessage.Builder builder = ChatbufProto.ChatMessage.newBuilder();
				ChatMessageBox.buildMessage(builder, m.getFrom(), m.getTo(), m.getType(), m.getData(), m.getTimestamp());
				if (m.getType() == ChatbufProto.ChatMessage.ChatMessageType.LEAVE) {
					SyncState syncState = new SyncState(interest);
					boolean removed = dsync.onContactRemoved(syncState);
					if (removed) {
						log.log(Level.INFO, "Removed contact: " + m.getFrom() + ", on producer prefix: " + data.getName().toUri());
					}
					else {
						log.log(Level.INFO, "Failed to remove contact, " + m.getFrom() + ", on producer prefix: " + data.getName().toUri());
					}
				}
				newData.setContent(new Blob(builder.build().toByteArray()));
				onData.onData(interest, newData);
			}
		} catch (InvalidProtocolBufferException e) {
			log.log(Level.SEVERE, "failed to decode protocol buffer.", e);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "failed to pass chat message list to on data.", e);
		}
	}

	@Override
	public void onTimeout(Interest interest) {
		try {
			getNextDataSegmentFromContact();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onTimeout", e);
		}
	}
}
