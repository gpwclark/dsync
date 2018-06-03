package com.uofantarctica;

import com.uofantarctica.dsync.DSyncReporting;
import com.uofantarctica.dsync.model.Rolodex;
import com.uofantarctica.dsync.model.SyncState;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RolodexTest {
	Rolodex rolodex;
	String dataPrefix = "/ndn/com/uofantarctica/dsync";
	long defaultSeq = 0l;
	Name name1 = new Name(dataPrefix)
		.append(UUID.randomUUID().toString())
		.append(Long.toString(1l))
		.append(Long.toString(defaultSeq));
	Interest interest1 = new Interest(name1);

	Name name2 = new Name(dataPrefix)
		.append(UUID.randomUUID().toString())
		.append(Long.toString(2l))
		.append(Long.toString(defaultSeq));
	Interest interest2 = new Interest(name2);

	Name name3 = new Name(dataPrefix)
		.append(UUID.randomUUID().toString())
		.append(Long.toString(3l))
		.append(Long.toString(defaultSeq));
	Interest interest3 = new Interest(name3);

	public Rolodex makeNewRolodexWithSeededData() {
		SyncState s1 = new SyncState(interest1);
		SyncState s2 = new SyncState(interest2);
		SyncState s3 = new SyncState(interest3);
		Rolodex aRolodex = new Rolodex(s1, dataPrefix, new DSyncReporting("name", "id"));
		aRolodex.add(s2);
		aRolodex.add(s3);
		return aRolodex;
	}

	public Rolodex makeNewRolodexWithDifferentlyOdderedSeededData() {
		SyncState s1 = new SyncState(interest1);
		SyncState s2 = new SyncState(interest2);
		SyncState s3 = new SyncState(interest3);
		Rolodex aRolodex = new Rolodex(s3, dataPrefix, new DSyncReporting("name", "id"));
		aRolodex.add(s1);
		aRolodex.add(s2);
		return aRolodex;
	}

	@BeforeEach
	void setUp() {
		rolodex = makeNewRolodexWithSeededData();

	}

	@AfterEach
	void tearDown() {
		rolodex = null;
	}

	@Test
	public void testRolodexesWithSameContentsEqual() {
		Rolodex rolodex1 = makeNewRolodexWithDifferentlyOdderedSeededData();
		Rolodex rolodex2 = makeNewRolodexWithSeededData();
		assertEquals(true, rolodex2.equals(rolodex1));
		assertEquals(rolodex1, rolodex2);
	}

	@Test
	public void testRolodexSerializeable() throws Exception {
		byte[] rolodexSer = rolodex.serialize();
		System.out.println(rolodexSer.length);
		assertEquals(true, rolodexSer.length > 0);
	}

	@Test
	public void testRolodexDeserializeable() throws Exception {
		testRolodexesWithSameContentsEqual();
		byte[] rolodexSer = rolodex.serialize();
		Rolodex matchingRolodex = rolodex.deserialize(rolodexSer);
		Rolodex diffOrder = makeNewRolodexWithDifferentlyOdderedSeededData();
		assertEquals(diffOrder, matchingRolodex);
		assertEquals(diffOrder.size(), matchingRolodex.size());
		for (int i = 0; i < rolodex.size(); i++) {
			assertEquals(rolodex.get(i), matchingRolodex.get(i));
		}
	}
}