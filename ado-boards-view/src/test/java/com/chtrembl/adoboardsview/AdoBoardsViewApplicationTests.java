package com.chtrembl.adoboardsview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.chtrembl.adoboardsview.service.ADOServiceImpl;

@SpringJUnitConfig
class AdoBoardsViewApplicationTests {
	
	@Test
	void contructWorkItemsTypesWIQL() {
		assertEquals(ADOServiceImpl.contructWorkItemsTypesWIQL("Bug,Epic,Feature,Issue,Task,Test Case,Test Plan,Test Suite,User Story"), " AND ([System.WorkItemType] == 'Bug' OR [System.WorkItemType] == 'Epic' OR [System.WorkItemType] == 'Feature' OR [System.WorkItemType] == 'Issue' OR [System.WorkItemType] == 'Task' OR [System.WorkItemType] == 'Test Case' OR [System.WorkItemType] == 'Test Plan' OR [System.WorkItemType] == 'Test Suite' OR [System.WorkItemType] == 'User Story')");
	
		assertEquals(ADOServiceImpl.contructWorkItemsTypesWIQL("FOOBAR,FEEBAR"), " AND ([System.WorkItemType] == 'FOOBAR' OR [System.WorkItemType] == 'FEEBAR')");
		
		assertEquals(ADOServiceImpl.contructWorkItemsTypesWIQL("FOOBAR"), " AND ([System.WorkItemType] == 'FOOBAR')");
	
		assertEquals(ADOServiceImpl.contructWorkItemsTypesWIQL(""), "");
	}

}
