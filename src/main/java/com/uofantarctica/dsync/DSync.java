package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.ChatbufProto;
import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import com.uofantarctica.dsync.model.MessageOutbox;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.named_data.jndn.util.Common.getNowMilliseconds;

public class DSync implements OnInterestCallback, OnData, OnTimeout,
	OnRegisterFailed, OnRegisterSuccess, SyncAdapter {
	private static final String TAG = DSync.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private ChronoSync2013.OnInitialized onInitialized;
	private final long sessionNo;
	private final Face face;
	private final KeyChain keyChain;
	private final String id;
	private static final double lifetime = 5000d;
	private final long myInitialSeqNo = -1l;
	private final boolean useExclusions = true;
	private final String hubPrefix;
	private final String baseBroadcastPrefix;

	private OnData onData;
	private String broadcastPrefix;
	private String dataPrefix;
	private String myProducerPrefix;
	private Rolodex rolodex;
	private MessageOutbox outbox;
	private String dataSetName;
	private String screenName;
	private DSyncReporting dSyncReporting;
	private SyncState mySyncState;
	private ExclusionManager exclusionManager;

	public DSync(String hubPrefix, String baseBroadcastPrefix, Face face, KeyChain keyChain) {
		this.hubPrefix = hubPrefix;
		this.baseBroadcastPrefix = baseBroadcastPrefix;
		this.sessionNo = (int)Math.round(getNowMilliseconds() / 1000.0);
		this.face = face;
		this.keyChain = keyChain;
		this.id = UUID.randomUUID().toString();
	}

	public void initSyncForDataSet(OnData onData, ChronoSync2013.OnInitialized onInitialized, String dataSetName, String
		screenName) {
		this.onData = onData;
		this.onInitialized = onInitialized;
		this.dataSetName = dataSetName;
		this.screenName = screenName;
		this.dataPrefix = hubPrefix + "/" + dataSetName;
		this.broadcastPrefix = baseBroadcastPrefix + "/" + dataSetName;
		this.myProducerPrefix = dataPrefix + "/" + id + "/";
		this.mySyncState = new SyncState(myProducerPrefix, sessionNo, myInitialSeqNo);
		this.dSyncReporting = new DSyncReporting(screenName, id);
		this.rolodex = new Rolodex(mySyncState, dSyncReporting);
		this.exclusionManager = new ExclusionManager();
		this.outbox = new MessageOutbox(dataSetName, screenName, mySyncState);

		registerBroadcastPrefix();
		registerDataPrefix();
		expressInterestInRolodex();
	}

	public long getSessionNo() {
		return sessionNo;
	}

	public long getSequenceNo() {
		return mySyncState.getSeq();
	}

	public void publishNextMessage(long seqNo, String messageType, String message, double time) {
		ChatbufProto.ChatMessage.ChatMessageType actualMessageType = getActualMessageTypeFromString(messageType);
		outbox.publishNextMessage(seqNo, actualMessageType, message, time);
	}

//	@Override
	public long getProducerSequenceNo(String prefix_, long sessionNo_) {
		return mySyncState.getSeq();
	}

//	@Override
	public void publishNextSequenceNo() {
	}

	private ChatbufProto.ChatMessage.ChatMessageType getActualMessageTypeFromString(String messageType) {
		return ChatbufProto.ChatMessage.ChatMessageType.valueOf(messageType);
	}

	private void registerBroadcastPrefix() {
		try {
			face.registerPrefix(new Name(broadcastPrefix),
				(OnInterestCallback) this,
				(OnRegisterFailed)this,
				(OnRegisterSuccess)this);
			dSyncReporting.onRegisterBroadcastPrefix(broadcastPrefix);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		} catch (SecurityException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		}
	}

	private void registerDataPrefix() {
		try {
			ContactDataResponder cdr = new ContactDataResponder(outbox, dSyncReporting);
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

	public void expressInterestInRolodex() {
		Name name = new Name(broadcastPrefix);
		name.append(rolodex.getRolodexHashString());
		Interest interest = new Interest(name);
		if (useExclusions) {
			exclusionManager.addExclusionsIfNecessary(rolodex.getRolodexHashString(), interest);
		}
		interest.setInterestLifetimeMilliseconds(lifetime);
		expressInterest(interest, this, this);
	}

	public void expressInterestInDataSuffix(SyncState s) {
		Name name = SyncState.makeSyncStateName(s);
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
