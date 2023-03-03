package com.chtrembl.adoboardsview.service;

import org.springframework.stereotype.Service;

@Service
public interface ADOService {
	public void loadProjectsStep1();
	public void loadWorkItemIdsStep2();
	public void loadWorkItemMetaDataStep3();	
}
