package com.uofantarctica.dsync.syncdata;

import com.uofantarctica.dsync.model.SyncState;
import com.uofantarctica.dsync.model.SyncStateBox;
import com.uofantarctica.dsync.model.SyncStates;
import com.uofantarctica.dsync.utils.SerializeUtils;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContactDataRequester implements OnInterestCallback, OnRegisterFailed, OnRegisterSuccess {
	private static final String TAG = ContactDataRequester.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private final SyncStateBox outbox;

	public ContactDataRequester(SyncStateBox outbox) {
		this.outbox = outbox;
	}

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
		if (outbox.canSatisfy(interest)) {
			SyncStates ss = outbox.getSyncStatesFromInterest(interest);
			byte[] bytes;
			try {
				bytes = new SerializeUtils<SyncStates>().serialize(ss);
				Blob content = new Blob(bytes);
				Data data = new Data(interest.getName());
				data.setContent(content);
				try {
					face.putData(data);
				} catch (IOException e) {
					log.log(Level.SEVERE, "Failed to place message on interest." + interest.toUri());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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
