
package com.chtrembl.adoboardsview.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({ "serial"})
public class WorkItem implements Serializable {
	
	@JsonAlias({"id"})
	private int id;
	
	private String workItemType;
	private String title;
	private String state;
	private String areaPath;
	private String teamProject;
	private String createdDate;
	private String changedDate;
	
	@JsonProperty("fields")
	@JsonIgnoreProperties(value = { "System.AssignedTo" })
	private void unpackNameFromNestedObject(Map<String, String> fields) {
		this.workItemType = fields.get("System.WorkItemType");
		this.title = fields.get("System.Title");
		this.state = fields.get("System.State");
		this.areaPath = fields.get("System.AreaPath");
		this.teamProject = fields.get("System.TeamProject");
		this.createdDate = fields.get("System.CreatedDate");
		this.changedDate = fields.get("System.ChangedDate");
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getWorkItemType() {
		return workItemType;
	}
	public void setWorkItemType(String workItemType) {
		this.workItemType = workItemType;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getAreaPath() {
		return areaPath;
	}
	public void setAreaPath(String areaPath) {
		this.areaPath = areaPath;
	}
	public String getTeamProject() {
		return teamProject;
	}
	public void setTeamProject(String teamProject) {
		this.teamProject = teamProject;
	}
	public String getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}
	public String getChangedDate() {
		return changedDate;
	}
	public void setChangedDate(String changedDate) {
		this.changedDate = changedDate;
	}
}