package com.uofantarctica.dsync;

import net.named_data.jndn.Data;
import net.named_data.jndn.Exclude;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExclusionManager {
	private static final String TAG = ExclusionManager.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	Map<String, Exclude> exclusionsMap = new HashMap<>();

	public void recordInterestOnData(Interest interest, Data data) {
		String hash = interest.getName().get(-1).toEscapedString();
		Exclude exclude = exclusionsMap.get(hash);
		if (exclude == null) {
			exclude = new Exclude();
			addExcludeIfNotAlreadyPresent(exclude, data);
		}
		else {
			addExcludeIfNotAlreadyPresent(exclude, data);
		}
		exclusionsMap.put(hash, exclude);
	}

	private void addExcludeIfNotAlreadyPresent(Exclude exclude, Data data) {
		try {
			Name.Component newExcludeComp = getDataHash(data);
			if (!exclude.matches(newExcludeComp)) {
				exclude.appendComponent(newExcludeComp);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "Failed to add exclude for data.", e);
		}
	}

	private Name.Component getDataHash(Data data) throws Exception {
		try {
			Name.Component newExcludeComp = data.getFullName().get(-1);
			return newExcludeComp;
		} catch (EncodingException e) {
			throw new Exception(e);
		}
	}

	public void addExclusionsIfNecessary(String rolodexHashString, Interest interest) {
		Exclude exclude = exclusionsMap.get(rolodexHashString);
		if (exclude != null) {
			interest.setExclude(new Exclude(exclude));
		}
	}

	public boolean canSatisfy(Interest interest, Data data) {
		boolean canSatisfy = true;
		try {
			canSatisfy = interest.matchesData(data);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to check if canSatisfy will work.", e);
		}
		return canSatisfy;
	}
}
