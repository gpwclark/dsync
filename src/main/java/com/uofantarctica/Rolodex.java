package com.uofantarctica;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Rolodex implements Serializable {
	private static final String TAG = Rolodex.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	ArrayList<String> contacts = new ArrayList<>();

	public Rolodex(String dataPrefix) {
		contacts.add(dataPrefix);
	}

	public void add(String newDataPrefix) {
		contacts.add(newDataPrefix);
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(this);
			out.flush();
			byte[] bytes = bos.toByteArray();
			return bytes;
		} catch (IOException e) {
			log.log(Level.SEVERE, "failed to serialize rolodex.", e);
			throw e;
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Rolodex rolodex = (Rolodex) o;
		return Objects.equals(contacts, rolodex.contacts);
	}

	@Override
	public int hashCode() {

		return Objects.hash(contacts);
	}

	public Rolodex deserialize(byte[] rolodexSer) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis
			= new ByteArrayInputStream(rolodexSer);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			Object o = in.readObject();
			return (Rolodex)o;
		} catch (Exception e) {
			log.log(Level.SEVERE, "failed to deserialize rolodex.", e);
			throw e;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}

	}
}
