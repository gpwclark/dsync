package com.uofantarctica.dsync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Common;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncState implements Serializable {
	private static final String TAG = SyncState.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private String id;
	private long session;
	private long seq;
	private String digest;

	public SyncState(String id, long session, long seq) {
		setId(id);
		setSession(session);
		setSeq(seq);
		setDigest();
	}

	public SyncState(Interest interest) {
		Name name = interest.getName();
		setId(name.get(-3).toEscapedString());
		setSession(Long.valueOf(name.get(-2).toEscapedString()));
		setSeq(Long.valueOf(name.get(-1).toEscapedString()));
		setDigest();
	}

	private void setDigest() {
		try {
			digest = createHash(id.getBytes(), longToBytes(session), longToBytes(seq));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create hash of this SyncState.", e);
		}
	}

	public static String createHash(byte[] ...bytes) throws Exception {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			for (byte[] b : bytes) {
				sha256.update(b);
			}
			return new String(sha256.digest());
		}
		catch (Exception e) {
			throw e;
		}
	}

	public byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
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
		int thatDigest = syncState.hashCode();
		int thisDigest = this.hashCode();
		return thisDigest == thatDigest;
	}

	public String getDigest() {
		return this.digest;
	}

	@Override
	public int hashCode() {
		return getDigest().hashCode();
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
