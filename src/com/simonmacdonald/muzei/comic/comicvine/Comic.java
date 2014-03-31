package com.simonmacdonald.muzei.comic.comicvine;

public class Comic {
	private String title;
	private String imageUrl;
	private String detailsUrl;
	
	public Comic(String title, String imageUrl, String detailsUrl) {
		this.title = title;
		this.imageUrl = imageUrl;
		this.detailsUrl = detailsUrl;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public String getDetailsUrl() {
		return detailsUrl;
	}
	public void setDetailsUrl(String detailsUrl) {
		this.detailsUrl = detailsUrl;
	}
}
