package com.uofantarctica.dsync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncState implements Serializable {
	private String id;
	private long seq;

	public static Name makeSyncStateName(String dataPrefix, String contact, long seq) {
		Name name = new Name(dataPrefix)
			.append(contact)
			.append(Name.Component.fromNumber(seq));
		return name;
	}

	public static SyncState getSyncStateFromInterest(Interest interest) {
		Name name = interest.getName();
		SyncState syncState = new SyncState();
		syncState.setId(name.get(-2).toEscapedString());
		syncState.setSeq(Long.valueOf(name.get(-1).toEscapedString()));
		return syncState;
	}
}
