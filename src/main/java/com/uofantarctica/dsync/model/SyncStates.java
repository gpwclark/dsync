package com.uofantarctica.dsync.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

public class SyncStates implements Serializable, Iterable<SyncState> {
	private final List<SyncState> syncStates = new ArrayList<>();
	private final String dataPrefix;

	public SyncStates(String dataPrefix) {
		this.dataPrefix = dataPrefix;
	}

	public SyncStates(SyncState s, String dataPrefix) {
		syncStates.add(s);
		this.dataPrefix = dataPrefix;
	}

	public void add(SyncState s) {
		syncStates.add(s);
	}

	public int size() {
		return syncStates.size();
	}

	public SyncState get(int i) {
		return syncStates.get(i);
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

	public boolean contains(SyncState s) {
		return syncStates.contains(s);
	}

	public String getDataPrefix() {
		return dataPrefix;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SyncStates that = (SyncStates) o;
		return Objects.equals(syncStates, that.syncStates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(syncStates);
	}
}
