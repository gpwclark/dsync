package com.uofantarctica;

import net.named_data.jndn.sync.ChronoSync2013;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class SyncStates implements Serializable, Iterable<SyncState> {
	List<SyncState> syncStates = new ArrayList<>();

	public SyncStates(SyncState s) {
		syncStates.add(s);
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

	public List<SyncState> getSyncStates() {
		return syncStates;
	}
}
