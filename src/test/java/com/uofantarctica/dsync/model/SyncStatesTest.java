package com.uofantarctica.dsync.model;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SyncStatesTest {
	SyncState tripletSyncState1;
	SyncState tripletSyncState2;
	SyncState tripletSyncState3;

	SyncState uniqueSyncState;

	SyncStates ss1;
	SyncStates ss2;
	SyncStates ss3;

	@BeforeEach
	void setUp() {
		long session = 1232;
		long seq = 1;
		String dataPrefix = "/data/prefix";
		String id = "myid";
		tripletSyncState1 = new SyncState(id, session, seq);
		tripletSyncState2 = new SyncState(id, session, seq);

		uniqueSyncState = new SyncState("otherid", 9123, 12);
		Name name = new Name(dataPrefix)
			.append(id)
			.append(Long.toString(session))
				.append(Long.toString(seq));
		Interest interest = new Interest(name);
		tripletSyncState3 = new SyncState(interest);

		ss1 = new SyncStates(uniqueSyncState, dataPrefix);
		ss1.add(tripletSyncState1);
		ss1.add(tripletSyncState3);

		ss2 = new SyncStates(tripletSyncState2, dataPrefix);
		ss2.add(uniqueSyncState);
		ss2.add(tripletSyncState3);

		ss3 = new SyncStates(tripletSyncState3, dataPrefix);
		ss3.add(tripletSyncState2);
		ss3.add(uniqueSyncState);
	}

	@AfterEach
	void tearDown() {

	}

	@Test
	void testSyncStateHashCodes() {
		assertEquals(tripletSyncState1, tripletSyncState2);
		assertEquals(tripletSyncState1, tripletSyncState3);
		assertEquals(tripletSyncState2, tripletSyncState3);
	}

	@Test
	void testTwoIdenticalSyncStatesEquivalent() {
		String data = "/data";
		SyncStates s1 = new SyncStates(tripletSyncState1, data);
		SyncStates s2 = new SyncStates(tripletSyncState2, data);
		assertEquals(s1, s2);
	}

	@Test
	void testSyncStatesInVariousOrders() {
		assertEquals(ss1, ss2);
		assertEquals(ss1, ss3);
		assertEquals(ss2, ss3);
	}
}