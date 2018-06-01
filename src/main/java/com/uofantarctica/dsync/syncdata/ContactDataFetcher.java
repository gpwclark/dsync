package com.uofantarctica.dsync.syncdata;

import com.uofantarctica.dsync.DSync;
import com.uofantarctica.dsync.OnReceivedSyncStates;
import com.uofantarctica.dsync.model.SyncStates;
import com.uofantarctica.dsync.utils.SerializeUtils;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactDataFetcher implements OnData, OnTimeout {
	private static final String TAG = ContactDataFetcher.class.getName();
	private static final Logger log = Logger.getLogger(TAG);
	private final long seq;
	private final String contact;
	private final DSync dsync;
	private final OnReceivedSyncStates onReceivedSyncStates;

	public ContactDataFetcher(DSync dsync, OnReceivedSyncStates onReceivedSyncStates, long seq, String contact) {
		this.dsync = dsync;
		this.onReceivedSyncStates = onReceivedSyncStates;
		this.seq = seq;
		this.contact = contact;
	}

	@Override
	public void onData(Interest interest, Data data) {
		Blob content = data.getContent();
		byte[] bytes = content.getImmutableArray();
		try {
			SyncStates ss = new SerializeUtils<SyncStates>().deserialize(bytes);
			onReceivedSyncStates.onReceivedSyncStates(ss.getSyncStates());
			dsync.expressInterestInDataSuffix(contact, seq + 1);
		} catch (IOException e) {
			log.log(Level.SEVERE, "failed to deserialize sync states.", e);
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE, "failed to deserialize sync states.", e);
		}
	}

	@Override
	public void onTimeout(Interest interest) {
		dsync.expressInterestInDataSuffix(contact, seq);
	}
}
