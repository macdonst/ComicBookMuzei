package com.simonmacdonald.muzei.comic.comicvine;

public class Volume {
	private int id;
	private int firstIssue;
	private int lastIssue;
	
	public Volume(int id, int firstIssue, int lastIssue) {
		this.id = id;
		this.firstIssue = firstIssue;
		this.lastIssue = lastIssue;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getFirstIssue() {
		return firstIssue;
	}
	public void setFirstIssue(int firstIssue) {
		this.firstIssue = firstIssue;
	}
	public int getLastIssue() {
		return lastIssue;
	}
	public void setLastIssue(int lastIssue) {
		this.lastIssue = lastIssue;
	}
}
