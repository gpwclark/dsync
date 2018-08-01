package com.uofantarctica.dsync.model;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.util.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatMessageBox {
	private static final Logger log = LoggerFactory.getLogger(ChatMessageBox.class);
	private final Map<Long, Data> myMessages = new HashMap<>();
	private final String chatRoom;
	private final String screenName;
	private final SyncState mySyncState;

	public ChatMessageBox(String chatRoom, String screenName, SyncState mySyncState) {
		this.chatRoom = chatRoom;
		this.screenName = screenName;
		this.mySyncState = mySyncState;
	}

	public long getCurrSeqNo() {
		return mySyncState.getSeq();
	}

	//TODO putting the id on there is redundant.
	public Data getDataForInterest(Interest interest) {
		SyncState syncState = new SyncState(interest);
		ReturnStrategy strategy = syncState.getReturnStrategy();
		Data data = buildMessage(strategy, syncState);
		data.setName(interest.getName());
		return data;
	}

	private Data buildMessage(ReturnStrategy strategy, SyncState syncState) {
		switch (strategy) {
			/*
			case ALL: return fetchMessagesFromRequestedToEnd(syncState);
				break;
		*/
			case EXACT: return fetchExactMessageFromRequest(syncState);
			case MOST_RECENT: return fetchMostRecent();
		}
		return fetchExactMessageFromRequest(syncState);
	}

	private Data fetchExactMessageFromRequest(SyncState syncState) {
		long reqSeq = syncState.getSeq();
		return getMessage(reqSeq);
	}

	private Data fetchMostRecent() {
		long mostRecent = mySyncState.getSeq();
		return getMessage(mostRecent);
	}

	/*
	private void fetchMessagesFromRequestedToEnd(SyncState syncState) {
		long reqSeq = syncState.getSeq();
		do {
			builder.addMessageList(getMessage(reqSeq));
			builder.setHighestSeq(reqSeq);
			++reqSeq;
		} while (currRequestIsValid(reqSeq));
	}
	*/

	private Data getMessage(long reqSeq) {
		return myMessages.get(reqSeq);
	}

	public boolean shouldRespond(Interest interest) {
		SyncState s = SyncState.getSyncStateFromInterest(interest);
		return myMessages.get(s.getSeq()) != null;
	}

	private boolean currRequestIsValid(long seqNo) {
		return seqNo <= mySyncState.getSeq();
	}

	/*
	public void publishNextMessage(ChatbufProto.ChatMessage.ChatMessageType messageType, String message, double time) {
		ChatbufProto.ChatMessage.Builder builder
			= ChatbufProto.ChatMessage.newBuilder();
		buildMessage(builder, screenName, chatRoom, messageType, message, time);
		publishNextMessage(builder.build());
	}
	*/

	public void publishNextMessage(Data data) {
		myMessages.put(mySyncState.getNextSeq(), data);
	}

	public static void buildMessage(ChatbufProto.ChatMessage.Builder builder,
																	String screenName, String chatRoom,
																	ChatbufProto.ChatMessage.ChatMessageType messageType,
																	String message,
																	double time) {
		builder.setFrom(screenName)
			.setTo(chatRoom)
			.setType(messageType)
			.setData(message)
			.setTimestamp((int)Math.round(time / 1000.0));
	}

	/*
	private void publishNextMessage(ChatbufProto.ChatMessage message) {
		myMessages.put(mySyncState.getNextSeq(), message);
	}
	*/
}
