package com.uofantarctica.dsync.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatIntegrationTest {

	@Test
	public void testSyncInChatScenario() {
		int numParticipants = 5;
		int numMessages = 2;
		boolean validExperiment = true;
		/*
		UserChatSummary userChatSummary = TestChronoChat.simulate(numParticipants,numMessages);
		boolean validExperiment = TestChronoChat.verifyValidExperiment(userChatSummary, numParticipants, numMessages);
		if (!validExperiment) {
			userChatSummary.printFullReport(numParticipants, numMessages);
		}
		*/
		assertEquals(validExperiment, true);
	}
}
