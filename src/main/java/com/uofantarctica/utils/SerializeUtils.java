package com.uofantarctica.utils;

import com.uofantarctica.Rolodex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerializeUtils<T> {
	private static final String TAG = SerializeUtils.class.getName();
	private static final Logger log = Logger.getLogger(TAG);

	public byte[] serialize(T t) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(t);
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

	public T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis
			= new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			Object o = in.readObject();
			return (T)o;
		} catch (Exception e) {
			log.log(Level.SEVERE, "failed to deserialize rolodex.", e);
			throw
				e;
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
