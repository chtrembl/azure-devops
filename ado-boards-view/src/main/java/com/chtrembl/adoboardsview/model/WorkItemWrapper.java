package com.chtrembl.adoboardsview.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public record WorkItemWrapper (@JsonAlias({"workitems","value"}) List<WorkItem> workItems) {}