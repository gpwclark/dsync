package com.uofantarctica.dsync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
public class SyncState implements Serializable {
	private String id;
	private long session;
	private long seq;

	public SyncState(Interest interest) {
		Name name = interest.getName();
		setId(name.get(-3).toEscapedString());
		setSession(Long.valueOf(name.get(-2).toEscapedString()));
		setSeq(Long.valueOf(name.get(-1).toEscapedString()));
	}

	public static String makeRegisterPrefixName(String dataPrefix, SyncState s) {
		Name name = new Name(dataPrefix)
			.append(s.getId())
			.append(Long.toString(s.getSession()));
		return name.toUri();
	}

	public static Name makeSyncStateName(String dataPrefix, SyncState s) {
		Name name = new Name(dataPrefix)
			.append(s.getId())
			.append(Long.toString(s.getSession()))
			.append(Long.toString(s.getSeq()));
		return name;
	}

	public static SyncState getSyncStateFromInterest(Interest interest) {
		SyncState syncState = new SyncState(interest);
		return syncState;
	}

	public void incSyncState() {
		this.setSeq(this.getSeq() + 1);
	}

	public Long getNextSeq() {
		this.incSyncState();
		return this.getSeq();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getSession() {
		return session;
	}

	public void setSession(long session) {
		this.session = session;
	}

	public long getSeq() {
		return seq;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SyncState syncState = (SyncState) o;
		return session == syncState.session &&
			Objects.equals(id, syncState.id);
	}

	@Override
	public int hashCode() {

		return Objects.hash(id, session);
	}

	@Override
	public String toString() {
		return "SyncState{" +
			"id='" + id + '\'' +
			", session=" + session +
			", seq=" + seq +
			'}';
	}
}
