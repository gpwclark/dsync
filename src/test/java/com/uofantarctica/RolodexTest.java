package com.uofantarctica;

import com.uofantarctica.dsync.model.Rolodex;
import org.junit.Test;

import static org.junit.Assert.*;

public class RolodexTest {

	@Test
	public void testRolodexSerializeable() throws Exception {
		Rolodex rolodex = new Rolodex("meow");
		rolodex.add("woww");
		rolodex.add("lkjslksjdlkjdlkjdslskjdslskjdlksdjdslksjdlkjdlkj092uj908h83303hj" + "-903j039hj309h3093h093h309h3093h093h039h3093h039h3039h3093h093h039h3093h039h309h");
		byte[] rolodexSer = rolodex.serialize();
		System.out.println(rolodexSer.length);
		assertEquals(true, rolodexSer.length > 0);
	}

	@Test
	public void testRolodexDeserializeable() throws Exception {
		Rolodex rolodex = new Rolodex("meow");
		rolodex.add("woww");
		rolodex.add("lkjslksjdlkjdlkjdslskjdslskjdlksdjdslksjdlkjdlkj092uj908h83303hj" + "-903j039hj309h3093h093h309h3093h093h039h3093h039h3039h3093h093h039h3093h039h309h");
		byte[] rolodexSer = rolodex.serialize();
		Rolodex matchingRolodex = rolodex.deserialize(rolodexSer);
		assertEquals(rolodex, matchingRolodex);
		assertEquals(rolodex.size(), matchingRolodex.size());
		for (int i = 0; i < rolodex.size(); i++) {
			assertEquals(rolodex.get(i), matchingRolodex.get(i));
		}
	}
}