package com.uofantarctica.dsync;

import com.uofantarctica.dsync.model.SyncState;

import java.util.List;

public interface OnContactAdded {
	void onNewContactsAdded(List<SyncState> syncStates);
	void onExistingContacts(List<SyncState> syncStates);
	void onContacts(List<SyncState> newContacts, List<SyncState> existingContacts);
}
