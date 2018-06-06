package com.uofantarctica.dsync.model;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

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
	SyncStates uniqSs;

	long seq1, session1;
	long seq2, session2;
	long seq3, session3;
	long uniqSeq, uniqSession;
	String producerPrefix1;
	String producerPrefix2;
	String producerPrefix3;

	@BeforeEach
	void setUp() {
		String dataPrefix = "/data/prefix";
		producerPrefix1 = dataPrefix + "/an-id/" + ReturnStrategy.ALL.toString();
		producerPrefix2 = dataPrefix + "/other-id/" + ReturnStrategy.MOST_RECENT.toString();
		producerPrefix3 = dataPrefix + "/yet-another-id/" + ReturnStrategy.MOST_RECENT.toString();
		seq1 = session1 = 9823l;
		seq2 = session2 = 923l;
		seq3 = session3 = 983l;

		uniqSeq = uniqSession = 823l;
		uniqueSyncState = new SyncState(producerPrefix2, uniqSession, uniqSeq);
		uniqSeq = uniqSession = 823l;
		uniqueSyncState = new SyncState(producerPrefix2, uniqSession, uniqSeq);

		tripletSyncState1 = new SyncState(producerPrefix1, session1, seq1);
		tripletSyncState2 = new SyncState(producerPrefix1, session2,seq2);
		Name name = new Name(producerPrefix1)
			.append(Long.toString(uniqSeq))
				.append(Long.toString(session2));
		Interest interest = new Interest(name);
		tripletSyncState3 = new SyncState(interest);

		ss1 = new SyncStates(uniqueSyncState);
		ss1.add(tripletSyncState1);

		ss2 = new SyncStates(tripletSyncState2);
		ss2.add(uniqueSyncState);

		ss3 = new SyncStates(tripletSyncState3);
		ss3.add(uniqueSyncState);

		uniqSs = new SyncStates(uniqueSyncState);
	}

	@AfterEach
	void tearDown(){
		ss1 = ss2 = uniqSs = ss3 = null;
	}

	@Test
	void gettersAndSetters() {
		assertEquals(seq1, tripletSyncState1.getSeq());
		assertEquals(uniqSession, uniqueSyncState.getSession());
		assertEquals(tripletSyncState2.getProducerPrefix(), tripletSyncState3.getProducerPrefix());
		assertEquals(producerPrefix1, tripletSyncState3.getProducerPrefix());
	}

	@Test
	void parsingReturnStrategies() {
		assertEquals(tripletSyncState1.getReturnStrategy(), ReturnStrategy.ALL);
		assertEquals(uniqueSyncState.getReturnStrategy(), ReturnStrategy.MOST_RECENT);
	}

	@Test
	void testSyncStateHashCodes() {
		assertEquals(tripletSyncState1, tripletSyncState2);
		assertEquals(tripletSyncState1, tripletSyncState3);
		assertEquals(tripletSyncState2, tripletSyncState3);
		assertNotEquals(tripletSyncState2, uniqueSyncState);
	}

	@Test
	void testTwoIdenticalSyncStatesEquivalent() {
		SyncStates s1 = new SyncStates(tripletSyncState1);
		SyncStates s2 = new SyncStates(tripletSyncState2);
		assertEquals(s1, s2);
	}

	@Test
	void testSyncStatesInVariousOrdersEquivalent() {
		testSomeGeneralStuff();
		assertNotEquals(ss1, uniqSs);
		assertEquals(ss1, ss2);
		assertEquals(ss1, ss3);
		assertEquals(ss2, ss3);
	}

	@Test
	void testSomeGeneralStuff() {
		Map<String, String> map1 = new HashMap<>();
		map1.put(producerPrefix1, producerPrefix1);
		map1.put(producerPrefix2, producerPrefix2);
		Map<String, String> map2 = new HashMap<>();
		map2.put(producerPrefix2, producerPrefix2);
		map2.put(producerPrefix1, producerPrefix1);
		assertEquals(map1, map2);
		map2.put(producerPrefix3, producerPrefix3);
		assertNotEquals(map1, map2);
	}
}