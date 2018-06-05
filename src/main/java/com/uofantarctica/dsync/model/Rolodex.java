package com.uofantarctica.dsync.model;

import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.OnContactAdded;
import com.uofantarctica.dsync.syncdata.ContactDataReceiver;
import com.uofantarctica.dsync.utils.SerializeUtils;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Rolodex implements Serializable, Iterable<SyncState> {
	private static final String TAG = Rolodex.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private SyncStates syncStates;
	private transient DSyncReporting dSyncReporting;
	private transient Map<String, ContactDataReceiver> dataReceivers = new HashMap<>();

	public Rolodex(SyncState myInitialSyncState, String theDataPrefix, DSyncReporting dSyncReporting) {
		syncStates = new SyncStates(myInitialSyncState, theDataPrefix);
		this.dSyncReporting = dSyncReporting;
	}

	public boolean matchesCurrentRolodex(Interest interest) {
		Name name = interest.getName();
		String otherRolodexHash = name.get(-1).toEscapedString();
		String thisRolodexHash = getRolodexHashString();
		return thisRolodexHash.equals(otherRolodexHash);
	}

	public String getRolodexHashString() {
		int hashCode = this.hashCode();
		String hash = String.valueOf(hashCode);
		return hash;
	}


	public String getDataPrefix() {
		return syncStates.getDataPrefix();
	}

	public void add(SyncState s) {
		syncStates.add(s);
		dSyncReporting.onContactAdditionInRolodex(s, this.hashCode());
	}

	public byte[] serialize() throws IOException {
		return new SerializeUtils<Rolodex>().serialize(this);
	}

	public static Rolodex deserialize(byte[] rolodexSer) throws IOException, ClassNotFoundException {
		return new SerializeUtils<Rolodex>().deserialize(rolodexSer);
	}

	public void merge(Rolodex newRolodex, OnContactAdded dSync) {
		List<SyncState> newContacts = new ArrayList<>();
		List<SyncState> existingContacts = new ArrayList<>();
		for (SyncState s : newRolodex) {
			//VERY important that we are passing the actual sync state objects the rolodex has in it.
			if (!syncStates.contains(s)) {
				this.add(s);
				newContacts.add(s);
			}
			else {
				SyncState syncStateFromRolodex = this.get(s);
				existingContacts.add(syncStateFromRolodex);
			}
		}
		if (newContacts.size() > 0 && existingContacts.size() > 0) {
			dSync.onContacts(newContacts, existingContacts);
		}
		else if (newContacts.size() > 0) {
			dSync.onNewContactsAdded(newContacts);
		}
		else if (existingContacts.size() > 0) {
			dSync.onExistingContacts(existingContacts);
		}
	}

	@Override
	public Iterator<SyncState> iterator() {
		return syncStates.iterator();
	}

	@Override
	public void forEach(Consumer<? super SyncState> action) {
		syncStates.forEach(action);
	}

	@Override
	public Spliterator<SyncState> spliterator() {
		return syncStates.spliterator();
	}

	public int size() {
		return syncStates.size();
	}

	public SyncState get(SyncState syncState) {
		return syncStates.get(syncState);
	}

	public SyncState get(int i) {
		return syncStates.get(i);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Rolodex that = (Rolodex) o;
		return this.syncStates.equals(that.syncStates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.syncStates.getSyncStateMap());
	}

	@Override
	public String toString() {
		return "Rolodex{" +
			"syncStates=" + syncStates +
			'}';
	}

	public void registerExpressed(SyncState syncState, ContactDataReceiver cdr) {
		dataReceivers.put(syncState.getId(), cdr);
	}

	public void silenceExistingSyncStateCallback(SyncState syncState) throws Exception {
		ContactDataReceiver cdr = dataReceivers.get(syncState.getId());
		if (cdr == null)
			throw new Exception("Data receiver was null for id of given sync state.");

		if (!cdr.isBlocking()) {
			cdr.silence();
		}
	}
}

