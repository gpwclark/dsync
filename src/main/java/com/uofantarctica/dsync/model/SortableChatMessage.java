package com.uofantarctica.dsync.model;

public class SortableChatMessage implements Comparable<Integer> {
	private final Integer time;
	private final ChatbufProto.ChatMessage chatMessage;

	public SortableChatMessage(ChatbufProto.ChatMessage chatMessage) {
		this.time = chatMessage.getTimestamp();
		this.chatMessage = chatMessage;
	}

	public ChatbufProto.ChatMessage getMessage() {
		return chatMessage;
	}

	@Override
	public int compareTo(Integer o) {
		return this.time.compareTo(o);
	}
}
