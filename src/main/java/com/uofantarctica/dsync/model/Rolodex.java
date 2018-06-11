package com.uofantarctica.dsync.model;

import com.google.protobuf.InvalidProtocolBufferException;
import com.uofantarctica.dsync.DSync;
import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.syncdata.ContactDataReceiver;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Rolodex implements Serializable, Iterable<SyncState> {
	private static final String TAG = Rolodex.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private SyncStates syncStates;
	private transient DSync dSync;
	private transient OnData onData;
	private transient DSyncReporting dSyncReporting;

	public Rolodex(SyncState myInitialSyncState, DSync dSync, OnData onData,
								 DSyncReporting dSyncReporting) {
		syncStates = new SyncStates(myInitialSyncState);
		this.dSync = dSync;
		this.onData = onData;
		this.dSyncReporting = dSyncReporting;
	}

	public Rolodex(SyncState syncState) {
		syncStates = new SyncStates(syncState);
		this.onData = dummyOnData;
		this.dSyncReporting = new DSyncReporting("foreignRolodex", "foreignRolodex");
	}

	public boolean matchesCurrentRolodex(Interest interest) {
		Name name = interest.getName();
		String otherRolodexHash = name.get(-1).toEscapedString();
		String thisRolodexHash = getRolodexHashString();
		return thisRolodexHash.equals(otherRolodexHash);
	}

	public String getRolodexHashString() {
		return Integer.toString(this.hashCode());
	}

	public void add(SyncState s) {
		ContactDataReceiver cdr = createContactDataReceiver(s);
		syncStates.add(s, cdr);
		dSyncReporting.onContactAdditionInRolodex(s, this.hashCode());
	}

	private ContactDataReceiver createContactDataReceiver(SyncState s) {
		return new ContactDataReceiver(dSync, onData, s, dSyncReporting);
	}

	public boolean remove(SyncState syncState) {
		boolean removed = syncStates.remove(syncState);
		dSyncReporting.reportRolodexAfterRemove(this);
		return removed;
	}

	public boolean removeSelf(SyncState myInitialSyncState) {
		boolean removed = syncStates.removeSelf(myInitialSyncState);
		dSyncReporting.reportRolodexAfterRemove(this);
		return removed;
	}

	public static Rolodex deserialize(byte[] rolodexSer) throws Exception {
		return deserializeWithProtocolBuffers(rolodexSer);
	}

	private static Rolodex deserializeWithProtocolBuffers(byte[] rolodexSer) throws Exception {
		try {
			SyncStateProto.SyncStateMsg messages = SyncStateProto.SyncStateMsg.parseFrom(rolodexSer);
			Rolodex rol = null;
			for (int i = 0; i < messages.getSsCount(); i++) {
				SyncState syncState = convertToAppSyncState(messages.getSs(i));
				if (rol == null)
					rol = new Rolodex(syncState);
				else
					rol.add(syncState);
			}
			return rol;
		} catch (InvalidProtocolBufferException e) {
			throw new Exception(e);
		}
	}

	private static SyncState convertToAppSyncState(SyncStateProto.SyncState ss) {
		SyncState syncState = new SyncState(ss.getName(), ss.getSeqno().getSession(), ss.getSeqno().getSeq());
		return syncState;
	}

	public byte[] serialize() throws IOException {
		return serializeWithProtocolBuffers();
	}

	private byte[] serializeWithProtocolBuffers() {
		SyncStateProto.SyncStateMsg.Builder builder = SyncStateProto.SyncStateMsg.newBuilder();
		for (SyncState syncState : syncStates) {
			addMessage(builder,
				(String) syncState.getProducerPrefix(),
				SyncStateProto.SyncState.ActionType.UPDATE,
				(Long) syncState.getSeq(),
				(Long) syncState.getSession());
		}
		return builder.build().toByteArray();
	}

	private void addMessage(SyncStateProto.SyncStateMsg.Builder builder, String producerPrefix,
													SyncStateProto.SyncState.ActionType update, Long seq, Long session) {
		builder.addSsBuilder()
			.setName(producerPrefix)
			.setType(update)
			.getSeqnoBuilder().setSeq(seq)
			.setSession(session);
	}

	public void merge(Rolodex newRolodex) {
		List<SyncState> newContacts = new ArrayList<>();
		for (SyncState s : newRolodex) {
			if (!syncStates.contains(s)) {
				s.setSeq(0l);
				this.add(s);
				newContacts.add(s);
			}
		}
		dSyncReporting.reportRolodexAfterMerge(this);
		expressInterestInDataOfNewContact(newContacts);
	}

	public void expressInterestInDataOfNewContact(List<SyncState> newContacts) {
		for (SyncState s : newContacts) {
			Interest interest = s.getInterest();
			interest.setInterestLifetimeMilliseconds(dSync.getLifetime());
			ContactDataReceiver cdr;
			try {
				cdr = getContactDataReceiver(s);
				dSync.expressInterest(interest, cdr, cdr);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Failed to place interest in new contact: " + s.toString(), e);
			}
		}
	}

	private ContactDataReceiver getContactDataReceiver(SyncState s) throws Exception {
		return syncStates.getContactDataReceiver(s);
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
		return Objects.hash(this.syncStates);
	}

	@Override
	public String toString() {
		return "Rolodex{" +
			"syncStates=" + syncStates +
			'}';
	}

	private static class DummyOnData implements OnData {
		public final void
		onData(Interest interest, Data data) {
			log.log(Level.INFO, "Dummy on data fired.");
		}
	}
	public final static OnData dummyOnData = new DummyOnData();
}

