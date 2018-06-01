package com.uofantarctica;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

public class RolodexTest {

	@BeforeEach
	public void setUp() throws Exception {
	}

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
	}
}