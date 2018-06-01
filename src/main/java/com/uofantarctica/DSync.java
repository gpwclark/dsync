package com.uofantarctica;

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

import java.io.IOException;
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

	public DSync(OnReceivedSyncStates onReceivedSyncStates, OnInitialized onInitialized, String dataPrefix, String broadcastPrefix, long sessionNo, Face face, KeyChain keyChain) {
		this.onReceivedSyncStates = onReceivedSyncStates;
		this.onInitialized = onInitialized;
		this.dataPrefix = dataPrefix;
		this.broadcastPrefix = broadcastPrefix;
		this.sessionNo = sessionNo;
		this.face = face;
		this.keyChain = keyChain;

		registerBroadcastPrefix();
		initRolodexInterests();
		Rolodex rolodex = new Rolodex(dataPrefix);
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

	private void initRolodexInterests() {
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

	}

	@Override
	public void onData(Interest interest, Data data) {

	}

	@Override
	public void onTimeout(Interest interest) {

	}
}
