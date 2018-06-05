package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.ChatbufProto;
import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import com.uofantarctica.dsync.model.ChatMessageBox;
import com.uofantarctica.dsync.syncdata.ContactDataReceiver;
import com.uofantarctica.dsync.syncdata.ContactDataResponder;
import com.uofantarctica.dsync.utils.SerializeUtils;
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
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DSync implements OnInterestCallback, OnData, OnTimeout, OnRegisterFailed, OnRegisterSuccess {
	private static final String TAG = DSync.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final OnData onData;
	private final ChronoSync2013.OnInitialized onInitialized;
	private final String theDataPrefix;
	private final String myDataPrefix;
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
	private final boolean useExclusions = false;

	public DSync(OnData onData, ChronoSync2013.OnInitialized onInitialized, String theDataPrefix, String theBroadcastPrefix,
							 long sessionNo, Face face, KeyChain keyChain, String chatRoom, String screenName) {
		this.onData = onData;
		this.onInitialized = onInitialized;
		this.theDataPrefix = theDataPrefix;
		this.theBroadcastPrefix = theBroadcastPrefix;
		this.sessionNo = sessionNo;
		this.face = face;
		this.keyChain = keyChain;
		this.id = UUID.randomUUID().toString();
		this.myInitialSyncState = new SyncState(id, sessionNo, myInitialSeqNo);
		this.myDataPrefix = SyncState.makeRegisterPrefixName(theDataPrefix, myInitialSyncState);
		this.dSyncReporting = new DSyncReporting(screenName, id);
		this.rolodex = new Rolodex(myInitialSyncState, theDataPrefix, dSyncReporting);
		this.chatRoom = chatRoom;
		this.screenName = screenName;
		this.exclusionManager = new ExclusionManager();

		this.outbox = new ChatMessageBox(chatRoom, screenName, myInitialSyncState);

		registerBroadcastPrefix();
		registerDataPrefix();
		expressInterestInRolodex();
	}

	private String makeDataName(String dataPrefix, long sessionNo, String id) {
		return dataPrefix + "/" + sessionNo + "/" + id;
	}

	public void publishNextMessage(long seqNo, ChatbufProto.ChatMessage.ChatMessageType messageType, String message, double time) {
		outbox.publishNextMessage(seqNo, messageType, message, time);
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
			ContactDataResponder cdr = new ContactDataResponder(outbox, dSyncReporting);
		face.registerPrefix(new Name(myDataPrefix),
			(OnInterestCallback) cdr,
			(OnRegisterFailed)cdr,
			(OnRegisterSuccess)cdr);
			dSyncReporting.onRegisterDataPrefix(myDataPrefix);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		} catch (SecurityException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
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
		Name name = SyncState.makeSyncStateName(theDataPrefix, s);
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
			log.log(Level.SEVERE, "Error thrown in onInitialized.");
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
			byte[] rolodexSer = new SerializeUtils<Rolodex>().serialize(rolodex);
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
		if (useExclusions) {
			exclusionManager.recordInterestOnData(interest, data);
		}
		Blob content = data.getContent();
		try {
			byte[] rolodexSer = content.getImmutableArray();
			Rolodex otherRolodex = Rolodex.deserialize(rolodexSer);
			List<SyncState> newContacts = rolodex.merge(otherRolodex);
			for (SyncState s : newContacts) {
				onContactAdded(s);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error merging rolodexes.", e);
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
