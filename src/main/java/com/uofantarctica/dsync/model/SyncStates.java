package com.uofantarctica.dsync.model;

import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import com.uofantarctica.dsync.syncdata.ContactDataReceiver;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncStates implements Serializable, Iterable<SyncState> {
	private static final String TAG = SyncStates.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final List<SyncState> syncStateList = new ArrayList<>();
	private final Map<String, SyncState> syncStateMap = new HashMap<>();
	private transient final Map<String, ContactDataReceiver> cdrMap = new HashMap<>();

	public SyncStates(SyncState s) {
		this.initialAdd(s);
	}
	public void initialAdd(SyncState s) {
		syncStateList.add(s);
		syncStateMap.put(s.getProducerPrefix(), s);
	}

	public void add(SyncState s, ContactDataReceiver cdr) {
		syncStateList.add(s);
		syncStateMap.put(s.getProducerPrefix(), s);
		cdrMap.put(s.getProducerPrefix(), cdr);
	}

	public int size() {
		return syncStateList.size();
	}

	public SyncState get(int i) {
		return syncStateList.get(i);
	}

	@Override
	public Iterator<SyncState> iterator() {
		return syncStateList.iterator();
	}

	@Override
	public void forEach(Consumer<? super SyncState> action) {
		syncStateList.forEach(action);
	}

	@Override
	public Spliterator<SyncState> spliterator() {
		return syncStateList.spliterator();
	}

	public boolean contains(SyncState s) {
		SyncState syncState = syncStateMap.get(s.getProducerPrefix());
		return syncState != null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SyncStates that = (SyncStates) o;
		return Objects.equals(syncStateMap, that.syncStateMap);
	}

	@Override
	public int hashCode() {

		return Objects.hash(syncStateMap);
	}

	@Override
	public String toString() {
		return "SyncStates{" +
			", syncStateList=" + syncStateList +
			'}';
	}

	public boolean removeSelf(SyncState myInitialSyncState) {
		SyncState syncState = syncStateMap.get(myInitialSyncState.getProducerPrefix());
		if (syncState == null) {
			log.log(Level.SEVERE, "sync state for self was not found in map, should have been found.");
			return false;
		}
		boolean mapRemove = syncStateMap.remove(syncState.getProducerPrefix(), syncState);
		boolean listRemove = syncStateList.remove(syncState);
		return mapRemove && listRemove;
	}

	public boolean remove(SyncState syncStateKey) {
		SyncState syncState = syncStateMap.get(syncStateKey.getProducerPrefix());
		if (syncState == null) {
			log.log(Level.SEVERE, "sync state was not found in map, should have been found.");
			return false;
		}
		boolean mapRemove = syncStateMap.remove(syncState.getProducerPrefix(), syncState);
		boolean listRemove = syncStateList.remove(syncState);
		ContactDataReceiver cdr = cdrMap.get(syncState.getProducerPrefix());
		cdr.stopExpressingInterestInContactData();
		boolean cdrRemoved = cdrMap.remove(syncState.getProducerPrefix(), cdr);
		return mapRemove && listRemove && cdrRemoved;
	}

	public ContactDataReceiver getContactDataReceiver(SyncState s) throws Exception {
		ContactDataReceiver cdr = cdrMap.get(s.getProducerPrefix());
		if (cdr == null)
			throw new Exception("No such contact.");

		return cdr;
	}
}
