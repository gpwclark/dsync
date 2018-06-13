package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.ChatbufProto;
import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import com.uofantarctica.dsync.model.ChatMessageBox;
import com.uofantarctica.dsync.syncdata.MyContactDataInterestResponder;
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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DSync implements OnInterestCallback, OnData, OnTimeout, OnRegisterFailed, OnRegisterSuccess {
	private static final String TAG = DSync.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final OnData onData;
	private final ChronoSync2013.OnInitialized onInitialized;
	private final String theDataPrefix;
	private final String myProducerPrefix;
	private final String theBroadcastPrefix;
	private final long sessionNo;
	private final Face face;
	private final KeyChain keyChain;
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
	private boolean enabled = true;
	boolean haveSharedNewRolodexWithoutMySyncState = false;

	public DSync(OnData onData, ChronoSync2013.OnInitialized onInitialized, String theDataPrefix, String theBroadcastPrefix,
							 long sessionNo, Face face, KeyChain keyChain, String chatRoom, String screenName) {
		this.onData = onData;
		this.onInitialized = onInitialized;
		this.theDataPrefix = theDataPrefix;
		this.theBroadcastPrefix = theBroadcastPrefix;
		this.sessionNo = sessionNo;
		this.face = face;
		this.keyChain = keyChain;
		this.chatRoom = chatRoom;
		this.screenName = screenName;

		this.id = UUID.randomUUID().toString();
		this.myProducerPrefix = theDataPrefix + "/" + id;

		this.myInitialSyncState = new SyncState(myProducerPrefix, sessionNo, myInitialSeqNo);
		this.dSyncReporting = new DSyncReporting(screenName, id);

		this.exclusionManager = new ExclusionManager();
		this.rolodex = new Rolodex(myInitialSyncState, this, onData, dSyncReporting);
		this.outbox = new ChatMessageBox(chatRoom, screenName, myInitialSyncState);

		registerBroadcastPrefix();
		registerDataPrefix();
		expressInterestInRolodex();
	}

	public void publishNextMessage(long seqNo, String messageType, String message, double time) {
		ChatbufProto.ChatMessage.ChatMessageType actualMessageType = getActualMessageTypeFromString(messageType);
		outbox.publishNextMessage(seqNo, actualMessageType, message, time);
	}


	public void shutdown() {
		enabled = false;
		rolodex.removeSelf(myInitialSyncState);
	}

	private ChatbufProto.ChatMessage.ChatMessageType getActualMessageTypeFromString(String messageType) {
		return ChatbufProto.ChatMessage.ChatMessageType.valueOf(messageType);
	}

	private void registerBroadcastPrefix() {
		try {
			face.registerPrefix(new Name(theBroadcastPrefix),
				(OnInterestCallback) this,
				(OnRegisterFailed)this,
				(OnRegisterSuccess)this);
			dSyncReporting.onRegisterBroadcastPrefix(theBroadcastPrefix);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		} catch (SecurityException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		}
	}

	private void registerDataPrefix() {
		try {
			MyContactDataInterestResponder cdr = new MyContactDataInterestResponder(outbox, dSyncReporting);
		face.registerPrefix(new Name(myProducerPrefix),
			(OnInterestCallback) cdr,
			(OnRegisterFailed)cdr,
			(OnRegisterSuccess)cdr);
			dSyncReporting.onRegisterDataPrefix(myProducerPrefix);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		} catch (SecurityException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		}
	}

	private void expressInterestInRolodex() {
		Name name = new Name(theBroadcastPrefix);
		name.append(rolodex.getRolodexHashString());
		Interest interest = new Interest(name);
		if (useExclusions) {
			exclusionManager.addExclusionsIfNecessary(rolodex.getRolodexHashString(), interest);
		}
		interest.setInterestLifetimeMilliseconds(lifetime);
		expressInterest(interest, this, this);
	}

	private void expressInterest(Interest interest, OnData onData, OnTimeout onTimeout) {
		try {
			face.expressInterest(interest, onData, onTimeout);
		} catch (IOException e) {
			log.log(Level.SEVERE, "failed to express interest", e);
		}
	}

	@Override
	public void onRegisterFailed(Name prefix) {
		log.log(Level.SEVERE, "Failed to register prefix: " + prefix.toUri());
		throw new RuntimeException("Failed to register prefix.");
	}

	@Override
	public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
		log.log(Level.INFO, "Registered prefix: " + prefix.toUri());
		try {
			onInitialized.onInitialized();
		}
		catch(Exception e) {
			log.log(Level.SEVERE, "Error thrown in onInitialized.", e);
		}
	}

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
		if (!enabled  && haveSharedNewRolodexWithoutMySyncState) {
			return;
		}
		else if (!haveSharedNewRolodexWithoutMySyncState) {
			haveSharedNewRolodexWithoutMySyncState = true;
		}
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
			log.log(Level.SEVERE, "failed to serialize rolodex.");
		}
	}

	@Override
	public void onData(Interest interest, Data data) {
		if (!enabled) {
			return;
		}
        processData(interest, data);
	}

	private void processData(Interest interest, Data data) {
		if (useExclusions) {
			exclusionManager.recordInterestOnData(interest, data);
		}
		Blob content = data.getContent();
		try {
			byte[] rolodexSer = content.getImmutableArray();
			Rolodex otherRolodex = Rolodex.deserialize(rolodexSer);
			rolodex.merge(otherRolodex);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error merging rolodexes.", e);
		}
		expressInterestInRolodex();
	}

	@Override
	public void onTimeout(Interest interest) {
		if (!enabled) {
			return;
		}
		expressInterestInRolodex();
	}

	public boolean onContactRemoved(SyncState syncState) {
		return rolodex.remove(syncState);
	}

	public double getLifetime() {
		return lifetime;
	}
}
