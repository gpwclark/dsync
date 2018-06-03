package com.uofantarctica.dsync.model;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.tests.ChatbufProto;
import net.named_data.jndn.util.Blob;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ChatMessageBox {
	private static final String TAG = ChatMessageBox.class.getName();
	private static final Logger log = Logger.getLogger(TAG);
	private final Map<Long, ChatbufProto.ChatMessage> myMessages = new HashMap<>();
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
		SyncState s = new SyncState(interest);
		Data data = new Data(interest.getName());
		ChatbufProto.ChatMessage.Builder builder = ChatbufProto.ChatMessage.newBuilder();
		//If their request is lower than the current seqNo, we want to give them
		//up to that.
		long reqSeq = s.getSeq();
		//TODO only ends up returning last message... how to serialize array of messages...
		//while (currRequestIsValid(reqSeq)) {
			ChatbufProto.ChatMessage message = getMessage(reqSeq);
			builder.setFrom(message.getFrom());
			builder.setTo(message.getTo());
			builder.setType(message.getType());
			builder.setData(message.getData());
			builder.setTimestamp((int)Math.round(message.getTimestamp() / 1000.0));
			//++reqSeq;
		//}
		ChatbufProto.ChatMessage content = builder.build();
		byte[] array = content.toByteArray();
		Blob blob = new Blob(array);
		data.setContent(blob);
		return data;
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
		builder.setFrom(screenName);
		builder.setTo(chatRoom);
		builder.setType(messageType);
		builder.setData(message);
		builder.setTimestamp((int)Math.round(time / 1000.0));
		publishNextMessage(builder.build());
	}

	private void publishNextMessage(ChatbufProto.ChatMessage message) {
		myMessages.put(mySyncState.getNextSeq(), message);
	}
}
