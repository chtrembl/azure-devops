package com.chtrembl.adoboardsview.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Singleton to store container state
 */
@SuppressWarnings("serial")
@Component
public class ContainerEnvironment implements Serializable {
	@Value("${ado.services.org:}")
	private String adoServicesOrg;

	@Value("${app.version:}")
	private String appVersion;

	private List<Project> projects = new ArrayList<Project>();
	
	public List<Project> getProjects() {
		return projects;
	}

	public synchronized void setProjects(List<Project> projects) {
		this.projects = projects;
	}

	public String getAdoServicesOrg() {
		return adoServicesOrg;
	}

	public String getAppVersion() {
		return appVersion;
	}
}
