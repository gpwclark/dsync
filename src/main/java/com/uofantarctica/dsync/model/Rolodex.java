package com.uofantarctica.dsync.model;

import com.uofantarctica.dsync.utils.SerializeUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Rolodex implements Serializable, Iterable<String> {
	private static final String TAG = Rolodex.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	private List<String> contacts = new ArrayList<>();

	public Rolodex(String contact) {
		contacts.add(contact);
	}

	public void add(String contact) {
		contacts.add(contact);
		log.log(Level.INFO, "New contact added: " + contact);
	}

	public byte[] serialize() throws IOException {
		return new SerializeUtils<Rolodex>().serialize(this);
	}

	public static Rolodex deserialize(byte[] rolodexSer) throws IOException, ClassNotFoundException {
		return new SerializeUtils<Rolodex>().deserialize(rolodexSer);

	}

	public List<String> merge(Rolodex newRolodex) {
		List<String> newContacts = new ArrayList<>();
		for (String contact : newRolodex) {
			if (!contacts.contains(contact)) {
				this.add(contact);
				newContacts.add(contact);
			}
		}
		return newContacts;
	}

	@Override
	public Iterator<String> iterator() {
		return contacts.iterator();
	}

	@Override
	public void forEach(Consumer<? super String> action) {
		contacts.forEach(action);
	}

	@Override
	public Spliterator<String> spliterator() {
		return contacts.spliterator();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Rolodex strings = (Rolodex) o;
		return Objects.equals(contacts, strings.contacts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contacts);
	}

	public int size() {
		return contacts.size();
	}

	public String get(int i) {
		return contacts.get(i);
	}
}
