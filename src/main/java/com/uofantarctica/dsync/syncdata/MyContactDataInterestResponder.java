package com.uofantarctica.dsync.syncdata;

import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.model.ChatMessageBox;
import com.uofantarctica.dsync.model.SyncState;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyContactDataInterestResponder implements OnInterestCallback, OnRegisterFailed, OnRegisterSuccess {
	private static final String TAG = MyContactDataInterestResponder.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final ChatMessageBox outbox;
	private final DSyncReporting dSyncReporting;

	public MyContactDataInterestResponder(ChatMessageBox outbox, DSyncReporting dSyncReporting) {
		this.outbox = outbox;
		this.dSyncReporting = dSyncReporting;
	}

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
		if (outbox.shouldRespond(interest)) {
			dSyncReporting.onDataPrefixShouldRespondInterest(interest);
			Data data = outbox.getDataForInterest(interest);
			try {
				face.putData(data);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Failed to place message on interest." + interest.toUri(), e);
			}
		}
		else {
			dSyncReporting.onDataPrefixShouldNotRespondInterest(interest);
		}
	}

	@Override
	public void onRegisterFailed(Name prefix) {
		log.log(Level.SEVERE, "Failed to place register data prefix, this is bad: " + prefix.toUri());
	}

	@Override
	public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
		log.log(Level.INFO, "Registered prefix: " + prefix.toUri());
	}
}
