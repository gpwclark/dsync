package com.uofantarctica.dsync;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.tests.ChatbufProto;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DSyncReporting {
	private static final String TAG = DSyncReporting.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	public final String tag;
	public DSyncReporting(String screenName, String id) {
		tag = TAG + screenName + " " + id;
	}

	public void log(String msg) {
		log(tag, msg);
	}

	public static void log(String tag, String msg) {
		log.log(Level.INFO, tag + ": " + msg);
	}

	public void onInterestDoesNotMatchRolodex(Interest interest) {
		log("onInterestDoesNotMatchRolodex: " + interest.toUri());
	}

	public void onInterestMatchesRolodex(Interest interest) {
		log("onInterestMatchesRolodex: " + interest.toUri());
	}

	public void onRegisterBroadcastPrefix(String broadcastPrefix) {
		log("onRegisterBroadcastPrefix: " + broadcastPrefix);
	}

	public void onRegisterDataPrefix(String dataPrefix) {
		log("onRegisterDataPrefix: " + dataPrefix);
	}

	public void onContactAdded(SyncState s) {
		log("onContactAdded: " + s.toString());
	}

	public void onDataPrefixShouldNotRespondInterest(Interest interest) {
		log("onDataPrefixShouldNotRespondInterest: "  + interest.toUri());
	}

	public void onDataPrefixShouldRespondInterest(Interest interest) {
		log("onDataPrefixShouldRespondInterest: "  + interest.toUri());
	}

	public void onDataPrefixOnData(Interest interest, Data data) {
		log ("onDataPrefixOnData: " + interest.toUri());
		ChatbufProto.ChatMessage message = null;
		try {
			message = ChatbufProto.ChatMessage.parseFrom(data.getContent().getImmutableArray());
			log ("onDataPrefixOnData message: " + message.getData());
		} catch (InvalidProtocolBufferException e) {
			log.log(Level.SEVERE, "Failed to get message from data for reporting.");
		}
	}

	public void onContactAdditionInRolodex(SyncState s, int rolodexHash) {
		log("newContact added in rolodex: " + s);
		log("new rolodex hashCode: " + rolodexHash);
	}

	public void reportSendData(Interest interest) {
		log("face.putData onInterest: " + interest.toUri());
	}

	public void reportNotSendingDataDueToExcludes(Interest interest) {
		log("skipping, face.putData onInterest: " + interest.toUri());
	}
}
