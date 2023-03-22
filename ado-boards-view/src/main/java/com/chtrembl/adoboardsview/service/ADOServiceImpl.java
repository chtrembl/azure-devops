package com.chtrembl.adoboardsview.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;
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

	@Value("${ado.services.wiql.workitems.types:}")
	private String adoServicesWIQLWorkItemsTypes;

	@Value("${ado.workitem.history:-30d}")
	private String adoWorkItemHistory;

	@Value("${ado.rest.api.requestdelay:250}")
	private int adoRestAPIRequestDelay;

	private String adoServicesWIQLWorkItemsQuery;
	private String adoServicesWIQLWorkItemsSecondaryQuery;

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

		int size = 10000 * 1024;
    	logger.info(String.format("overriding webclient codec, maxInMemorySize:%smb",size/1024/1000));
		this.adoWebClient = WebClient.builder().baseUrl(this.adoServicesUrl).codecs(codecs -> codecs
	            .defaultCodecs()
	            .maxInMemorySize(size))
		    .build();

		logger.info("loading ADO data...");
		
		this.adoServicesWIQLWorkItemsQuery = String.format(
				"{\"query\":\"" + this.workItemsWIQLResource.getContentAsString(StandardCharsets.UTF_8)
						.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ") + "\"}",
				this.contructWorkItemsTypesWIQL(this.adoServicesWIQLWorkItemsTypes),this.adoWorkItemHistory, this.adoWorkItemHistory);

		
		//for projects that exhaust the 20,000 work item limit (when work item history is a very large parameter, run a second call with last 14 days)
		this.adoServicesWIQLWorkItemsSecondaryQuery = String.format(
				"{\"query\":\"" + this.workItemsWIQLResource.getContentAsString(StandardCharsets.UTF_8)
				.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ") + "\"}",
		this.contructWorkItemsTypesWIQL(this.adoServicesWIQLWorkItemsTypes),"-30d", "-30d");
		
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
		int largestWorkItemSize = 0;
		for (Project project : this.containerEnvironment.getProjects()) {
			try {
				setWorkItemsFromPOSTCall(project, this.adoServicesWIQLWorkItemsQuery);
			} catch (Exception e) {
				logger.error(String.format("Exception loading workitem ids for project: %s, post payload: %s, error: %s, skipping it... going to retry with shorter time frame", project.getName(), this.adoServicesWIQLWorkItemsQuery, e.getMessage()));
				setWorkItemsFromPOSTCall(project, this.adoServicesWIQLWorkItemsSecondaryQuery);
			}

			if(StringUtils.hasText(project.getWorkItemSize()) && Integer.parseInt(project.getWorkItemSize()) > largestWorkItemSize)
			{
				largestWorkItemSize = Integer.parseInt(project.getWorkItemSize());
			}
			 
			try
			{
				logger.info(String.format("sleeping %sms between requests...",this.adoRestAPIRequestDelay));
				Thread.sleep(this.adoRestAPIRequestDelay);
			} catch(Exception e)
			{
				logger.error(String.format("error sleeping %sms between requests...",this.adoRestAPIRequestDelay));
			}
		}
		
		// lousy hack to left pad because the presentation js sort wasn't working
		for (Project project : this.containerEnvironment.getProjects()) {
			project.setWorkItemSize(org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(project.getWorkItems().size()), String.valueOf(largestWorkItemSize).length(), "0"));
		}
	}

	public void setWorkItemsFromPOSTCall(Project project, String query)
	{
		String uri = String.format(this.adoServicesWIQLWorkItemsUri, project.getId());
		logger.info(String.format("retrieving workitem ids for project: %s from %s%s with the wiql query: %s",project.getName(),
				this.adoServicesUrl, uri, query));
		Consumer<HttpHeaders> consumer = it -> it.addAll(this.webRequest.getHeaders());
		WorkItemWrapper workItemWrapper = this.adoWebClient.post().uri(uri).accept(MediaType.APPLICATION_JSON)
				.headers(consumer).header("Accept", MediaType.APPLICATION_JSON_VALUE)
				.header("Content-Type", MediaType.APPLICATION_JSON_VALUE).header("Cache-Control", "no-cache")
				.header("Authorization",
						getBasicAuthenticationHeader(this.containerEnvironment.getAdoServicesOrg(),
								this.adoServicesPAT))
				.body(BodyInserters.fromValue(query)).retrieve()
				.bodyToMono(WorkItemWrapper.class).block();
		project.setWorkItems(workItemWrapper.workItems());
	}
	
	@Override
	public void loadWorkItemMetaDataStep3() {
		logger.info(String.format("retrieving workitem meta data from %s%s", this.adoServicesUrl,
				this.adoServicesProjectsUri));
		for (Project project : this.containerEnvironment.getProjects()) {
			if (!project.getWorkItems().isEmpty()) {
				String workItemsBatchRequestBody = null;
				try
				{
				//200 is the ADO REST limit, grab the latest 200
				String ids = project.getWorkItems().stream().map(w -> String.valueOf(w.getId()))
						.limit(200).collect(Collectors.joining(","));

				workItemsBatchRequestBody = String.format(
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
					logger.error(String.format("Exception loading workitem meta data for project: %s, post payload: %s, error: %s, skipping it... trace: %s", project.getName(), workItemsBatchRequestBody, e.getMessage(), ExceptionUtils.getStackTrace(e)));
				}
			}
			if(!StringUtils.hasText(project.getLastUpdatedDate()))
			{
				project.setLastUpdatedDate("0000-00-00T00:00:00.000Z");
			}
		}
	}

	public static final String contructWorkItemsTypesWIQL(String adoServicesWIQLWorkItemsTypes) {
		if(!StringUtils.hasText(adoServicesWIQLWorkItemsTypes))
		{
			return "";
		}
		
		List<String> workItemTypes = Stream.of(adoServicesWIQLWorkItemsTypes.split(",", -1))
				  .collect(Collectors.toList());
		
		String wiql = String.format(" AND (%s)", workItemTypes.stream().map(t -> String.format("[System.WorkItemType] == '%s'", t)).collect(Collectors.joining(" OR ")));
		
		return wiql;
	}

	private static final String getBasicAuthenticationHeader(String username, String password) {
		String valueToEncode = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
	}
}