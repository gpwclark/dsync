package com.uofantarctica.dsync.model;

import com.uofantarctica.dsync.DSyncReporting;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RolodexTest {
	long defaultSeq = 0l;
	String dataPrefix = "/ndn/com/uofantarctica/dsync";
	String uuid1 = UUID.randomUUID().toString();
	String uuid2 = UUID.randomUUID().toString();
	String uuid3 = UUID.randomUUID().toString();
	String uuid4 = UUID.randomUUID().toString();
	String producerPrefix1 = dataPrefix + uuid1;
	String producerPrefix2 = dataPrefix + uuid2;
	String producerPrefix3 = dataPrefix + uuid3;
	String producerPrefix4 = dataPrefix + uuid4;
	Name name1a = new Name(producerPrefix1)
		.append(Long.toString(1l))
		.append(Long.toString(defaultSeq));

	Name name1b = new Name(producerPrefix1)
		.append(Long.toString(11l))
		.append(Long.toString(12l));

	Name name2 = new Name(producerPrefix2)
		.append(Long.toString(2l))
		.append(Long.toString(defaultSeq));

	Name name3 = new Name(producerPrefix3)
		.append(Long.toString(3l))
		.append(Long.toString(defaultSeq));

	Name name4 = new Name(producerPrefix4)
		.append(Long.toString(4l))
		.append(Long.toString(defaultSeq));

	Interest interest1a = new Interest(name1a);
	Interest interest1b = new Interest(name1b);
	Interest interest2 = new Interest(name2);
	Interest interest3 = new Interest(name3);
	Interest interest4 = new Interest(name4);

	Rolodex rolodex;
	Rolodex otherRolodex;
	Rolodex differentRolodex;

	public Rolodex makeNewRolodexWithSeededData() {
		SyncState s1 = new SyncState(interest1a);
		SyncState s2 = new SyncState(interest2);
		SyncState s3 = new SyncState(interest3);
		Rolodex aRolodex = new Rolodex(s1, new DSyncReporting("name", "id"));
		aRolodex.add(s2);
		aRolodex.add(s3);
		return aRolodex;
	}

	public Rolodex makeNewRolodexWithDifferentlyOrderedSeededData() {
		SyncState s2 = new SyncState(interest2);
		SyncState s1 = new SyncState(interest1b);
		SyncState s3 = new SyncState(interest3);
		Rolodex aRolodex = new Rolodex(s3, new DSyncReporting("name", "id"));
		aRolodex.add(s1);
		aRolodex.add(s2);
		return aRolodex;
	}

	public Rolodex makeUniqueRolodex() {
		SyncState s1 = new SyncState(interest1b);
		SyncState s2 = new SyncState(interest2);
		SyncState s4 = new SyncState(interest4);
		Rolodex aRolodex = new Rolodex(s4, new DSyncReporting("name", "id"));
		aRolodex.add(s1);
		aRolodex.add(s2);
		return aRolodex;
	}

	@BeforeEach
	void setUp() {
		rolodex = makeNewRolodexWithSeededData();
		otherRolodex = makeNewRolodexWithDifferentlyOrderedSeededData();
		differentRolodex = makeUniqueRolodex();
	}

	@Test
	public void testRolodexEqualities() {
		assertEquals(otherRolodex, rolodex);
		assertEquals(otherRolodex.getRolodexHashString(), rolodex.getRolodexHashString());
		assertNotEquals(differentRolodex, rolodex);
		assertNotEquals(differentRolodex.getRolodexHashString(), rolodex.getRolodexHashString());
	}

	@Test
	public void testRolodexSerializeable() throws Exception {
		byte[] rolodexSer = rolodex.serialize();
		System.out.println(rolodexSer.length);
		assertEquals(true, rolodexSer.length > 0);
	}

	@Test
	public void testRolodexDeserializeable() throws Exception {
		byte[] rolodexSer = rolodex.serialize();
		Rolodex matchingRolodex = rolodex.deserialize(rolodexSer);
		assertEquals(otherRolodex, matchingRolodex);
		assertEquals(otherRolodex.size(), matchingRolodex.size());
		for (int i = 0; i < rolodex.size(); i++) {
			assertEquals(rolodex.get(i), matchingRolodex.get(i));
		}
	}
}