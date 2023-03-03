package com.chtrembl.adoboardsview.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectWrapper (@JsonProperty("value") List<Project> projects) {}