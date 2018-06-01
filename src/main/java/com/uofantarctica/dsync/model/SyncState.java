package com.uofantarctica.dsync.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class SyncState implements Serializable {
	private String id;
	private long seq;
}
