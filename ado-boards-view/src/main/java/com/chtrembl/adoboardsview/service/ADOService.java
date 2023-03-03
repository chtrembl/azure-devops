package com.chtrembl.adoboardsview.service;

import org.springframework.stereotype.Service;

@Service
public interface ADOService {
	public void loadProjects();
	public void loadWorkItemIds();
	public void loadWorkItemMetaData();	
}
