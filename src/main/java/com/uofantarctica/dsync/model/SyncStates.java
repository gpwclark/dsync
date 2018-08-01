package com.uofantarctica.dsync.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

public class SyncStates implements Serializable, Iterable<SyncState> {
	private static final Logger log = LoggerFactory.getLogger(SyncStates.class);

	private final List<SyncState> syncStateList = new ArrayList<>();
	private final Map<String, SyncState> syncStateMap = new HashMap<>();

	public SyncStates() {
	}

	public SyncStates(SyncState s) {
		this.add(s);
	}

	public void add(SyncState s) {
		syncStateList.add(s);
		syncStateMap.put(s.getProducerPrefix(), s);
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
}
