package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import com.uofantarctica.dsync.model.SyncStateBox;
import com.uofantarctica.dsync.syncdata.ContactDataReceiver;
import com.uofantarctica.dsync.syncdata.ContactDataRequester;
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
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DSync implements OnInterestCallback, OnData, OnTimeout, OnRegisterFailed, OnRegisterSuccess {
	private static final String TAG = DSync.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final OnReceivedSyncStates onReceivedSyncStates;
	private final OnInitialized onInitialized;
	private final String dataPrefix;
	private final String broadcastPrefix;
	private final long sessionNo;
	private final Face face;
	private final KeyChain keyChain;
	private final String id;
	private final Rolodex rolodex;
	private static final double lifetime = 5000d;
	private final SyncStateBox outbox;

	public DSync(OnReceivedSyncStates onReceivedSyncStates, OnInitialized onInitialized, String dataPrefix, String broadcastPrefix, long sessionNo, Face face, KeyChain keyChain) {
		this.onReceivedSyncStates = onReceivedSyncStates;
		this.onInitialized = onInitialized;
		this.dataPrefix = dataPrefix;
		this.broadcastPrefix = broadcastPrefix;
		this.sessionNo = sessionNo;
		this.face = face;
		this.keyChain = keyChain;
		this.id = UUID.randomUUID().toString();

		this.outbox = new SyncStateBox();

		registerBroadcastPrefix();
		registerDataPrefix();
		expressInterestInRolodex();
		rolodex = new Rolodex(id);
	}

	public void publishNextSeqNo() {
		outbox.publishNextSyncState();
	}

	private void registerBroadcastPrefix() {
		try {
			face.registerPrefix(new Name(broadcastPrefix),
				(OnInterestCallback) this,
				(OnRegisterFailed)this,
				(OnRegisterSuccess)this);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		} catch (SecurityException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		}
	}

	private void registerDataPrefix() {
		try {
			ContactDataRequester cdr = new ContactDataRequester(outbox);
		face.registerPrefix(new Name(dataPrefix + "/" + sessionNo + "/" + id),
			(OnInterestCallback) cdr,
			(OnRegisterFailed)cdr,
			(OnRegisterSuccess)cdr);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		} catch (SecurityException e) {
			log.log(Level.SEVERE, "Failed to register prefix.", e);
		}
	}

	public void expressInterestInRolodex() {
		Name name = new Name(broadcastPrefix);
		name.append(getRolodexHash());
		Interest interest = new Interest(name);
		interest.setInterestLifetimeMilliseconds(lifetime);
		expressInterest(interest, this, this);
	}

	public String getRolodexHash() {
		int hashCode = rolodex.hashCode();
		String hash = String.valueOf(hashCode);
		return hash;
	}

	public void expressInterestInDataSuffix(String contact, long seq) {
		Name name = SyncState.makeSyncStateName(dataPrefix + "/" + sessionNo, contact, seq);
		Interest interest = new Interest(name);
		interest.setInterestLifetimeMilliseconds(lifetime);
		ContactDataReceiver cdp = new ContactDataReceiver(this, onReceivedSyncStates, seq, contact);
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
		log.log(Level.SEVERE, "Registered prefix: " + prefix.toUri());
		try {
			onInitialized.onInitialized();
		}
		catch(Exception e) {
			log.log(Level.SEVERE, "Error thrown in onInitialized.");
		}
	}

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
		if (!matchesCurrentRolodex(interest)) {
			sendRolodex(interest);
		}
	}

	private void sendRolodex(Interest interest) {
		try {
			byte[] rolodexSer = new SerializeUtils<Rolodex>().serialize(rolodex);
			Data data = new Data(interest.getName());
			Blob content = new Blob(rolodexSer);
			data.setContent(content);
			face.putData(data);
		} catch (IOException e) {
			log.log(Level.SEVERE, "failed to serialize rolodex.");
		}
	}

	private boolean matchesCurrentRolodex(Interest interest) {
		Name name = interest.getName();
		String otherRolodexHash = name.get(-1).toEscapedString();
		return getRolodexHash().equals(otherRolodexHash);
	}

	@Override
	public void onData(Interest interest, Data data) {
		Blob content = data.getContent();
		try {
			byte[] rolodexSer = content.getImmutableArray();
			Rolodex newRolodex = Rolodex.deserialize(rolodexSer);
			List<String> newContacts = rolodex.merge(newRolodex);
			for (String c : newContacts) {
				onContactAdded(c);
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error merging rolodexes.", e);
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE, "Error merging rolodexes.", e);
		}
	}

	@Override
	public void onTimeout(Interest interest) {
		expressInterestInRolodex();
	}

	public void onContactAdded(String contact) {
		expressInterestInDataSuffix(contact, 0l);
	}
}
