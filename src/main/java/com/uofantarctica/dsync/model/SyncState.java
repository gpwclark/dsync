package com.uofantarctica.dsync.model;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncState implements Serializable {
	private static final String TAG = SyncState.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private String producerPrefix;
	private long session;
	private long seq;
	private ReturnStrategy strategy;

	public SyncState(String producerPrefix, long session, long seq) {
		setProducerPrefix(producerPrefix);
		this.strategy = ReturnStrategy.EXACT;
		setReturnStrategy(strategy);
		setSession(session);
		setSeq(seq);
	}

	public SyncState(Interest interest) {
		Name name = interest.getName();
		Name subName = name.getSubName(0, name.size() - 3);
		setProducerPrefix(subName.toUri());
		setReturnStrategy(name.get(-3).toEscapedString());
		setSession(Long.valueOf(name.get(-2).toEscapedString()));
		setSeq(Long.valueOf(name.get(-1).toEscapedString()));
	}

	public static Name makeSyncStateName(SyncState s, ReturnStrategy strategy) {
		Name name = new Name(s.getProducerPrefix())
			.append(strategy.toString())
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

	private void setReturnStrategy(String s) {
		setReturnStrategy(ReturnStrategy.valueOf(s));
	}

	private void setReturnStrategy(ReturnStrategy strategy) {
		this.strategy = strategy;
	}

	public ReturnStrategy getReturnStrategy() {
		return strategy;
	}

	public String getProducerPrefix() {
		return producerPrefix;
	}

	public void setProducerPrefix(String producerPrefix) {
		this.producerPrefix = producerPrefix;
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
		return Objects.equals(hashCode(), syncState.hashCode());
	}

	@Override
	public int hashCode() {

		return Objects.hash(producerPrefix, session);
	}

	@Override
	public String toString() {
		return "SyncState{" +
			"producerPrefix='" + producerPrefix + '\'' +
			", session=" + session +
			", seq=" + seq +
			'}';
	}
}
