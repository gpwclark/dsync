package com.uofantarctica.dsync.syncdata;

import com.uofantarctica.dsync.DSync;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;

public class ContactDataProvider implements OnInterestCallback, OnRegisterFailed, OnRegisterSuccess {
	private final DSync dsync;

	public ContactDataProvider(DSync dsync) {
		this.dsync = dsync;
	}

	@Override
	public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
	}

	@Override
	public void onRegisterFailed(Name prefix) {

	}

	@Override
	public void onRegisterSuccess(Name prefix, long registeredPrefixId) {

	}
}
