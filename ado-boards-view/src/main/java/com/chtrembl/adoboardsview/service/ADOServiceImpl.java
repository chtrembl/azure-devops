package com.chtrembl.adoboardsview.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.chtrembl.adoboardsview.model.ContainerEnvironment;
import com.chtrembl.adoboardsview.model.Project;
import com.chtrembl.adoboardsview.model.ProjectWrapper;
import com.chtrembl.adoboardsview.model.WebRequest;
import com.chtrembl.adoboardsview.model.WorkItem;
import com.chtrembl.adoboardsview.model.WorkItemWrapper;

import jakarta.annotation.PostConstruct;

@Component
public class ADOServiceImpl implements ADOService {
	private static Logger logger = LoggerFactory.getLogger(ADOServiceImpl.class);

	private WebClient adoWebClient = null;

	@Autowired
	private WebRequest webRequest;

	@Autowired
	private ContainerEnvironment containerEnvironment;

	@Value("${ado.services.pat:}")
	private String adoServicesPAT;

	@Value("${ado.services.url:}")
	private String adoServicesUrl;

	@Value("${ado.services.projects.uri:}")
	private String adoServicesProjectsUri;

	@Value("${ado.services.workitems.uri:}")
	private String adoServicesWorkItemsUri;

	@Value("${ado.services.wiql.workitems.uri:}")
	private String adoServicesWIQLWorkItemsUri;

	@Value("${ado.workitem.history:-30d}")
	private String adoWorkItemHistory;

	@Value("${ado.rest.api.requestdelay:250}")
	private int adoRestAPIRequestDelay;

	private String adoServicesWIQLWorkItemsQuery;

	@Value("classpath:json/workItemsJSON.txt")
	private Resource workItemsJSONResource;

	@Value("classpath:wiql/workItemsWIQL.txt")
	private Resource workItemsWIQLResource;

	@PostConstruct
	public void initialize() throws Exception {
		String exception = "";
		if (!StringUtils.hasText(this.containerEnvironment.getAdoServicesOrg())) {
			exception = "Azure DevOps Organization runtime argument missing, please start container with '-e ADO_SERVICES_ORG=<org here>' option...";
		}
		if (!StringUtils.hasText(adoServicesPAT)) {
			exception += " Azure DevOps Personal Access Token runtime argument missing, please start container with '-e ADO_SERVICES_PAT=<pat here>' option...";
		}
		if (StringUtils.hasText(exception)) {
			throw new Exception(exception);
		}

		this.adoWebClient = WebClient.builder().baseUrl(this.adoServicesUrl).build();

		logger.info("loading ADO data...");

		this.adoServicesWIQLWorkItemsQuery = String.format(
				"{\"query\":\"" + this.workItemsWIQLResource.getContentAsString(StandardCharsets.UTF_8)
						.replace("\n", "").replace("\r", "").replaceAll(" +", " ") + "\"}",
				this.adoWorkItemHistory, this.adoWorkItemHistory);

		logger.info("starting loadProjectsStep1()...");
		this.loadProjectsStep1();
		logger.info("starting loadProjectsStep2()...");
		this.loadWorkItemIdsStep2();
		logger.info("starting loadProjectsStep3()...");
		this.loadWorkItemMetaDataStep3();

		logger.info("done loading ADO data...");
	}

	@Override
	public void loadProjectsStep1() {
		logger.info(String.format("retrieving projects from %s%s", this.adoServicesUrl, this.adoServicesProjectsUri));
		List<Project> projects = new ArrayList<Project>();
		Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());

		ProjectWrapper projectWrapper = this.adoWebClient.get().uri(this.adoServicesProjectsUri)
				.accept(MediaType.APPLICATION_JSON).headers(consumer).header("Accept", MediaType.APPLICATION_JSON_VALUE)
				.header("Cache-Control", "no-cache")
				.header("Authorization", getBasicAuthenticationHeader(this.containerEnvironment.getAdoServicesOrg(),
						this.adoServicesPAT))
				.retrieve().bodyToMono(ProjectWrapper.class).block();
		projects = projectWrapper.projects();
		if (projects.size() > 1) {
			projects = projects.stream().sorted(Comparator.comparing(Project::getName)).collect(Collectors.toList());
		}
		this.containerEnvironment.setProjects(projects);
	}

	@Override
	public void loadWorkItemIdsStep2() {
		for (Project project : this.containerEnvironment.getProjects()) {
			try {
				String uri = String.format(this.adoServicesWIQLWorkItemsUri, project.getId());
				logger.info(String.format("retrieving workitem ids for project: from %s%s with the wiql query: %s",project.getName(),
						this.adoServicesUrl, uri, this.adoServicesWIQLWorkItemsQuery));
				Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());
				WorkItemWrapper workItemWrapper = this.adoWebClient.post().uri(uri).accept(MediaType.APPLICATION_JSON)
						.headers(consumer).header("Accept", MediaType.APPLICATION_JSON_VALUE)
						.header("Content-Type", MediaType.APPLICATION_JSON_VALUE).header("Cache-Control", "no-cache")
						.header("Authorization",
								getBasicAuthenticationHeader(this.containerEnvironment.getAdoServicesOrg(),
										this.adoServicesPAT))
						.body(BodyInserters.fromValue(this.adoServicesWIQLWorkItemsQuery)).retrieve()
						.bodyToMono(WorkItemWrapper.class).block();
				project.setWorkItems(workItemWrapper.workItems());
				logger.info(String.format("sleeping %sms between requests...",this.adoRestAPIRequestDelay));
				Thread.sleep(this.adoRestAPIRequestDelay);
			} catch (Exception e) {
				logger.error("Exception loading workitem ids for project: , skipping it...", project.getName());
			}
		}
	}

	@Override
	public void loadWorkItemMetaDataStep3() {
		logger.info(String.format("retrieving workitem meta data from %s%s", this.adoServicesUrl,
				this.adoServicesProjectsUri));
		for (Project project : this.containerEnvironment.getProjects()) {
			if (!project.getWorkItems().isEmpty()) {
				try
				{
				String ids = project.getWorkItems().stream().map(w -> String.valueOf(w.getId()))
						.collect(Collectors.joining(","));

				String workItemsBatchRequestBody = String.format(
						this.workItemsJSONResource.getContentAsString(StandardCharsets.UTF_8)
								.replace("\n", "").replace("\r", "").replaceAll(" +", " "),
						ids);
				
				logger.info(String.format("retrieving workitem meta data from %s%s with body: %s", this.adoServicesUrl, this.adoServicesWorkItemsUri, workItemsBatchRequestBody));

				Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());
				WorkItemWrapper workItemWrapper = this.adoWebClient.post().uri(this.adoServicesWorkItemsUri).accept(MediaType.APPLICATION_JSON)
						.headers(consumer).header("Accept", MediaType.APPLICATION_JSON_VALUE)
						.header("Content-Type", MediaType.APPLICATION_JSON_VALUE).header("Cache-Control", "no-cache")
						.header("Authorization", getBasicAuthenticationHeader(
								this.containerEnvironment.getAdoServicesOrg(), this.adoServicesPAT))
						.body(BodyInserters.fromValue(workItemsBatchRequestBody))
						.retrieve().bodyToMono(WorkItemWrapper.class).block();
				project.setWorkItems(workItemWrapper.workItems());
				project.setLastUpdatedDate(project.getWorkItems().get(project.getWorkItems().size()-1).getChangedDate());
				logger.info(String.format("sleeping %sms between requests...",this.adoRestAPIRequestDelay));
				Thread.sleep(this.adoRestAPIRequestDelay);
				}
				catch(Exception e) {
					logger.error("Exception loading workitem meta data for project: , skipping it...", project.getName());
				}
			}
			if(!StringUtils.hasText(project.getLastUpdatedDate()))
			{
				project.setLastUpdatedDate("0000-00-00T00:00:00.000Z");
			}
		}
	}

	private static final String getBasicAuthenticationHeader(String username, String password) {
		String valueToEncode = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
	}
}