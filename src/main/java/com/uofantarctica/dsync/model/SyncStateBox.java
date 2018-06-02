package com.uofantarctica.dsync.model;

import net.named_data.jndn.Interest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SyncStateBox {
	private static final String TAG = SyncStateBox.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private long currSeqNo = -1;
	public SyncStateBox() {
	}

	public long getCurrSeqNo() {
		return currSeqNo;
	}

	public void publishNextSyncState() {
		++currSeqNo;
	}

	//TODO putting the id on there is redundant.
	public SyncStates getSyncStatesFromInterest(Interest interest) {
		SyncState s = SyncState.getSyncStateFromInterest(interest);
		SyncStates ss = null;
		//If their request is lower than the current seqNo, we want to give them
		//up to that.
		long reqSeq = s.getSeq();
		while (reqSeq <= currSeqNo) {
			if (ss == null) {
				ss = new SyncStates(new SyncState(s.getId(), s.getSeq()));
			}
			else {
				ss.add(new SyncState(s.getId(), s.getSeq()));
			}
			++reqSeq;
		}
		return ss;
	}

	public boolean canSatisfy(Interest interest) {
		SyncState s = SyncState.getSyncStateFromInterest(interest);
		return s.getSeq() <= currSeqNo;
	}
}
