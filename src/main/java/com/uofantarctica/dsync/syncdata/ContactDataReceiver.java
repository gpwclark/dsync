package com.uofantarctica.dsync.syncdata;

import com.uofantarctica.dsync.DSync;
import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.model.SyncState;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactDataReceiver implements OnData, OnTimeout {
	private static final String TAG = ContactDataReceiver.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final DSync dsync;
	private final OnData onData;
	private final SyncState s;
	private final DSyncReporting dSyncReporting;

	public ContactDataReceiver(DSync dsync, OnData onData, SyncState s, DSyncReporting dSyncReporting) {
		this.dsync = dsync;
		this.onData = onData;
		this.s = s;
		this.dSyncReporting = dSyncReporting;
	}

	@Override
	public void onData(Interest interest, Data data) {
		try {
			dSyncReporting.onDataPrefixOnData(interest, data);
			incSyncStateToHighestReceived(interest);
			dsync.expressInterestInDataSuffix(s);
			//passToOnData(interest, data);
			onData.onData(interest, data);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onData");
		}
	}

	private void incSyncStateToHighestReceived(Interest interest) {
			long seqNo = Long.parseLong(interest.getName().get(-1).toEscapedString());
			s.setSeq(seqNo + 1);
	}

	@Override
	public void onTimeout(Interest interest) {
		try {
			dsync.expressInterestInDataSuffix(s);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Error in ContactDataReceiver onTimeout");
		}
	}
}
