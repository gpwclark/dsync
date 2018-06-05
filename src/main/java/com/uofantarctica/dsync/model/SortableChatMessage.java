package com.uofantarctica.dsync.model;

import net.named_data.jndn.Name;

public class SortableChatMessage implements Comparable<Integer> {
	private final Integer time;
	private final Name name;
	private final ChatbufProto.ChatMessage chatMessage;

	public SortableChatMessage(ChatbufProto.ChatMessage chatMessage, Name name) {
		this.time = chatMessage.getTimestamp();
		this.chatMessage = chatMessage;
		this.name = name;
	}

	public ChatbufProto.ChatMessage getMessage() {
		return chatMessage;
	}

	public byte[] getSerializedMessage() {
		ChatbufProto.ChatMessage.Builder builder = ChatbufProto.ChatMessage.newBuilder();
		ChatMessageOutbox.buildMessage(builder, chatMessage.getFrom(), chatMessage.getTo(), chatMessage.getType(),
			chatMessage.getData(), chatMessage.getTimestamp());
		return builder.build().toByteArray();
	}

	public Name getName() {
		return name;
	}
	@Override
	public int compareTo(Integer o) {
		return this.time.compareTo(o);
	}
}
