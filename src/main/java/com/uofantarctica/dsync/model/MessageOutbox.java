package com.uofantarctica.dsync.model;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.util.Blob;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MessageOutbox {
	private static final String TAG = MessageOutbox.class.getName();
	private static final Logger log = Logger.getLogger(TAG);
	private final Map<Long, ChatbufProto.ChatMessage> myMessages = new HashMap<>();
	private final String chatRoom;
	private final String screenName;
	private final SyncState mySyncState;

	public MessageOutbox(String chatRoom, String screenName, SyncState mySyncState) {
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
		Data data = new Data(interest.getName());
		ChatbufProto.ChatMessageList.Builder builder = ChatbufProto.ChatMessageList.newBuilder();
		//If their request is lower than the current seqNo, we want to give them
		//up to that.
		buildMessageList(builder, syncState);

		ChatbufProto.ChatMessageList messageList = builder.build();
		byte[] array = messageList.toByteArray();
		Blob blob = new Blob(array);
		data.setContent(blob);
		return data;
	}

	private void buildMessageList(ChatbufProto.ChatMessageList.Builder builder, SyncState syncState) {
        fetchFromRequestedToEnd(builder, syncState);
	}

	//TODO the idea that there is something other than fetchFromRequestedToEnd is
	// important even if it has not yet been implemented.
	private void fetchMostRecent(ChatbufProto.ChatMessageList.Builder builder) {
		long mostRecent = mySyncState.getSeq();
		builder.addMessageList(getMessage(mostRecent));
		builder.setHighestSeq(mostRecent);
	}

	private void fetchFromRequestedToEnd(ChatbufProto.ChatMessageList.Builder builder, SyncState syncState) {
		long reqSeq = syncState.getSeq();
		do {
			builder.addMessageList(getMessage(reqSeq));
			builder.setHighestSeq(reqSeq);
			++reqSeq;
		} while (currRequestIsValid(reqSeq));
	}


	private ChatbufProto.ChatMessage getMessage(long reqSeq) {
		return myMessages.get(reqSeq);
	}

	public boolean shouldRespond(Interest interest) {
		SyncState s = SyncState.getSyncStateFromInterest(interest);
		return myMessages.get(s.getSeq()) != null;
	}

	private boolean currRequestIsValid(long seqNo) {
		return seqNo <= mySyncState.getSeq();
	}

	public void publishNextMessage(long seqNo, ChatbufProto.ChatMessage.ChatMessageType messageType, String message, double time) {
		ChatbufProto.ChatMessage.Builder builder
			= ChatbufProto.ChatMessage.newBuilder();
		buildMessage(builder, screenName, chatRoom, messageType, message, time);
		publishNextMessage(builder.build());
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

	private void publishNextMessage(ChatbufProto.ChatMessage message) {
		myMessages.put(mySyncState.getNextSeq(), message);
	}
}