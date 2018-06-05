package com.uofantarctica.dsync.model;

import net.named_data.jndn.Data;
import net.named_data.jndn.util.Blob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatMessageInbox {
	private static final String TAG = ChatMessageInbox.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final boolean isBlocking;
	private int count;
	private List<Data> dataBuffer;

	ChatMessageInbox(boolean isBlocking) throws Exception {
		if (isBlocking == true)
			throw new Exception("Can't instantiate class with isBlocking true only, must use other Ctr.");
		this.isBlocking = isBlocking;
	}

	ChatMessageInbox(boolean isBlocking, int count) throws Exception {
		if (isBlocking == false)
			throw new Exception("Can't instantiate class with isBlocking false and count, must use other Ctr.");
		this.isBlocking = isBlocking;
		this.count = count;
		dataBuffer = new ArrayList<>();
	}

	public static ChatMessageInbox makeBlockingInbox(int count) throws Exception {
		boolean isBlocking = true;
		try {
			return new ChatMessageInbox(isBlocking, count);
		} catch (Exception e) {
			throw e;
		}
	}

	public static ChatMessageInbox makeNonBlockingInbox() throws Exception {
		boolean isBlocking = false;
		try {
			return new ChatMessageInbox(isBlocking);
		} catch (Exception e) {
			throw e;
		}
	}

	public boolean isBlocking() {
		return isBlocking;
	}

	public boolean add(Data data) {
		dataBuffer.add(data);
		return dataBuffer.size() == count;
	}

	//TODO many easy optimizations if it ends up being necessary.
	public List<Data> getSorted() {
		List<SortableChatMessage> chatMessages = new ArrayList<>();
		List<Data> orderedData = new ArrayList<>();

		int j = 0;
		for (Data data : dataBuffer) {
			try {
				ChatbufProto.ChatMessageList rawMessageList =
					ChatbufProto.ChatMessageList.parseFrom(data.getContent().getImmutableArray());

				for (ChatbufProto.ChatMessage message : rawMessageList.getMessageListList()) {
					chatMessages.add(new SortableChatMessage(message, data.getName()));
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Failed sorting list at data #: " + j);
			}
			++j;
		}

		Collections.sort(chatMessages, Collections.reverseOrder());

		for (SortableChatMessage chatMessage : chatMessages) {
			Data newData = new Data(chatMessage.getName());
			newData.setContent(new Blob(chatMessage.getSerializedMessage()));
			orderedData.add(newData);
		}
		return orderedData;
	}
}
