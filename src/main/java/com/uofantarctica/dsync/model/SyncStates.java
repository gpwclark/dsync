package com.uofantarctica.dsync.model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	private final String dataPrefix;
	private String digest;

	public SyncStates(SyncState s, String dataPrefix) {
		this.add(s);
		this.dataPrefix = dataPrefix;
	}

	public void add(SyncState s) {
		try {
			digest = createHash(digest, s.getDigest());
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "failed to create new sync digest on add to syncStateList.", e);
		}
		syncStateList.add(s);
		syncStateMap.put(s.getId(), s);
	}

	private String createHash(String digest, String digest1) throws Exception {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			if (digest != null) {
				sha256.update(digest.getBytes());
			}
			sha256.update(digest1.getBytes());
			byte[] bytes = sha256.digest();
			return new String(bytes);
		}
		catch (Exception e) {
			throw e;
		}
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
		SyncState syncState = syncStateMap.get(s.getId());
		return syncState != null;
	}

	public String getDataPrefix() {
		return dataPrefix;
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

	public String getDigest() {
		return this.digest;
	}

	@Override
	public String toString() {
		return "SyncStates{" +
			"digest='" + digest + '\'' +
			", syncStateList=" + syncStateList +
			", dataPrefix='" + dataPrefix + '\'' +
			'}';
	}

	public Map<String, SyncState> getSyncStateMap() {
		return syncStateMap;
	}
}
