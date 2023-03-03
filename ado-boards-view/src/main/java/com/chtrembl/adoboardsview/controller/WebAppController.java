package com.chtrembl.adoboardsview.controller;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.chtrembl.adoboardsview.model.ContainerEnvironment;
import com.chtrembl.adoboardsview.model.Project;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web Controller for all of the model/presentation construction and various endpoints
 */
@Controller
public class WebAppController {
	private static Logger logger = LoggerFactory.getLogger(WebAppController.class);
	
	@Autowired
	private ContainerEnvironment containerEnvironment;
	
	@ModelAttribute
	public void setModel(HttpServletRequest request, Model model) {
		model.addAttribute("containerEnvironment",containerEnvironment);
	}

	@GetMapping(value = {"/","overview"})
	public String overview(Model model, HttpServletRequest request) throws URISyntaxException {
		logger.info("ADO Boards default view / requested, routing to overview view...");
		model.addAttribute("currentPage", "overview");
		return "landing";
	}
	
	@GetMapping(value = "/project")
	public String products(Model model,HttpServletRequest request, @RequestParam(name = "id") String id)
			throws URISyntaxException {

		logger.info(String.format("ADO Boards /project requested for %s, routing to project view...", id));

		Project project = this.containerEnvironment.getProjects().stream()
				  .filter(p -> id.equals(p.getId()))
				  .findAny()
				  .orElse(null);
		
		model.addAttribute("project", project);
		model.addAttribute("currentPage", "project");
		
		return "project";
	}
}
