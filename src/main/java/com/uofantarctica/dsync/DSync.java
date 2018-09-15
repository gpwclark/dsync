package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.ChatbufProto;
import com.uofantarctica.dsync.model.ReturnStrategy;
import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import com.uofantarctica.dsync.model.ChatMessageBox;
import com.uofantarctica.dsync.syncdata.ContactDataReceiver;
import com.uofantarctica.dsync.syncdata.ContactDataResponder;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.sync.ChronoSync2013;
import net.named_data.jndn.util.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class DSync implements OnInterestCallback, OnData, OnTimeout, OnRegisterFailed, OnRegisterSuccess {
	private static final Logger log = LoggerFactory.getLogger(DSync.class);

	private final OnData onData;
	private final ChronoSync2013.OnInitialized onInitialized;
	private final String myProducerPrefix;
	private final String theBroadcastPrefix;
	private final Face face;
	private final String id;
	private final Rolodex rolodex;
	private static final double lifetime = 5000d;
	private final ChatMessageBox outbox;
	private final String chatRoom;
	private final String screenName;
	private final DSyncReporting dSyncReporting;
	private final SyncState myInitialSyncState;
	private final long myInitialSeqNo = -1l;
	private final ExclusionManager exclusionManager;
	private final boolean useExclusions = true;
	private final ReturnStrategy strategy;
	public final static String DSYNC = "DSYNC"; //TODO change to allow for matching of DSync.class.getSimpleName();

	public DSync(OnData onData, ChronoSync2013.OnInitialized onInitialized, String theDataPrefix, String theBroadcastPrefix,
							 long sessionNo, Face face, KeyChain keyChain, String chatRoom, String screenName,
							 ReturnStrategy strategy) {
		this.onData = onData;
		this.onInitialized = onInitialized;
		this.theBroadcastPrefix = theBroadcastPrefix;
		this.face = face;
		this.id = UUID.randomUUID().toString();
		this.strategy = strategy;
		this.myProducerPrefix = theDataPrefix + "/" + id;
		this.myInitialSyncState = new SyncState(myProducerPrefix, sessionNo, myInitialSeqNo);
		this.dSyncReporting = new DSyncReporting(screenName, id);
		this.rolodex = new Rolodex(myInitialSyncState, dSyncReporting);
		this.chatRoom = chatRoom;
		this.screenName = screenName;
		this.exclusionManager = new ExclusionManager();

		this.outbox = new ChatMessageBox(chatRoom, screenName, myInitialSyncState);

		registerBroadcastPrefix();
		registerDataPrefix();
		expressInterestInRolodex();
	}

	/*
	public void publishNextMessage(long seqNo, String messageType, String message, double time) {
		ChatbufProto.ChatMessage.ChatMessageType actualMessageType = getActualMessageTypeFromString(messageType);
		outbox.publishNextMessage(seqNo, actualMessageType, message, time);
	}
	*/

	public void publishNextMessage(Data data) {
		outbox.publishNextMessage(data);
	}

	private ChatbufProto.ChatMessage.ChatMessageType getActualMessageTypeFromString(String messageType) {
		return ChatbufProto.ChatMessage.ChatMessageType.valueOf(messageType);
	}

	private void registerBroadcastPrefix() {
			registerPrefix(theBroadcastPrefix,
					(OnInterestCallback) this,
					(OnRegisterFailed)this,
					(OnRegisterSuccess)this);
			dSyncReporting.onRegisterBroadcastPrefix(theBroadcastPrefix);
	}

	private void registerDataPrefix() {
		ContactDataResponder cdr = new ContactDataResponder(outbox, dSyncReporting);
		registerPrefix(myProducerPrefix,
			(OnInterestCallback) cdr,
			(OnRegisterFailed)cdr,
			(OnRegisterSuccess)cdr);
			dSyncReporting.onRegisterDataPrefix(myProducerPrefix);
	}

	private void registerPrefix(String prefix, OnInterestCallback onInterestCallback, OnRegisterFailed
			onRegisterFailed, OnRegisterSuccess onRegisterSuccess) {
		try {
			face.registerPrefix(new Name(prefix), onInterestCallback, onRegisterFailed, onRegisterSuccess);
		} catch (IOException e) {
			log.error("Failed to call face.registerPrefix().", e);
		} catch (SecurityException e) {
			log.error("Failed to call face.registerPrefix().", e);
		}
	}

	public void expressInterestInRolodex() {
		Name name = new Name(theBroadcastPrefix);
		name.append(rolodex.getRolodexHashString());
		Interest interest = new Interest(name);
		if (useExclusions) {
			exclusionManager.addExclusionsIfNecessary(rolodex.getRolodexHashString(), interest);
		}
		interest.setInterestLifetimeMilliseconds(lifetime);
		expressInterest(interest, this, this);
	}

	public void expressInterestInDataSuffix(SyncState s) {
		Name name = SyncState.makeSyncStateName(s, strategy);
		Interest interest = new Interest(name);
		interest.setInterestLifetimeMilliseconds(lifetime);
		ContactDataReceiver cdp = new ContactDataReceiver(this, onData, s, dSyncReporting);
		expressInterest(interest, cdp, cdp);
	}

	//TODO i think we want some sort of intelligent backoff here and around register prefix.
	public void expressInterest(Interest interest, OnData onData, OnTimeout onTimeout) {
		try {
			face.expressInterest(interest, onData, onTimeout);
		} catch (IOException e) {
			log.error("failed to express interest", e);
		}
	}

	@Override
	public void onRegisterFailed(Name prefix) {
		log.error("Failed to register prefix: " + prefix.toUri());
		throw new RuntimeException("Failed to register prefix.");
	}

	@Override
	public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
		log.debug("Registered prefix: " + prefix.toUri());
		try {
			onInitialized.onInitialized();
		}
		catch(Exception e) {
			log.error("Error thrown in onInitialized.", e);
		}
	}

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
		if (!rolodex.matchesCurrentRolodex(interest)) {
			dSyncReporting.onInterestDoesNotMatchRolodex(interest);
			sendRolodex(interest);
		}
		else {
			dSyncReporting.onInterestMatchesRolodex(interest);
		}
	}

	private void sendRolodex(Interest interest) {
		try {
			byte[] rolodexSer = rolodex.serialize();
			Data data = new Data(interest.getName());
			Blob content = new Blob(rolodexSer);
			data.setContent(content);
			data.getMetaInfo().setFreshnessPeriod(0d);
			if (useExclusions || exclusionManager.canSatisfy(interest, data)) {
				dSyncReporting.reportSendData(interest);
				face.putData(data);
			}
			else {
				dSyncReporting.reportNotSendingDataDueToExcludes(interest);
			}
		} catch (IOException e) {
			log.error("failed to serialize rolodex.");
		}
	}

	@Override
	public void onData(Interest interest, Data data) {
		if (useExclusions) {
			exclusionManager.recordInterestOnData(interest, data);
		}
		List<SyncState> newContacts = rolodex.mergeContacts(data);
		for (SyncState s : newContacts) {
			onContactAdded(s);
		}
		expressInterestInRolodex();
	}

	@Override
	public void onTimeout(Interest interest) {
		expressInterestInRolodex();
	}

	public void onContactAdded(SyncState s) {
		dSyncReporting.onContactAdded(s);
		expressInterestInDataSuffix(s);
	}
}
