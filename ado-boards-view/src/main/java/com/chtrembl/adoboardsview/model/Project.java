package com.chtrembl.adoboardsview.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Project implements Serializable {
	private String id = null;
	private String name = null;
	private String description = null;
	private String url = null;
	private String visibility = null;
	private String lastUpdatedDate = null; //this is going to be set by the latest work item that we retrieve
	private List<WorkItem> workItems = null;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getVisibility() {
		return visibility;
	}
	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}
	public String getLastUpdatedDate() {
		return lastUpdatedDate;
	}
	public void setLastUpdatedDate(String lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public List<WorkItem> getWorkItems() {
		return workItems;
	}
	
	public void setWorkItems(List<WorkItem> workItems) {
		this.workItems = workItems;
	}
}