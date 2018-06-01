package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.SyncState;

import java.util.List;

public interface OnReceivedSyncStates {
	void onReceivedSyncStates(List<SyncState> syncStates);
}
