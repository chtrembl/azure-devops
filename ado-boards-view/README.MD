
## Azure DevOps Boards View App

Azure DevOps is awesome (as you probably already know)! However there are some really great tools for interacting with the data using the [Azure DevOps Rest API](https://learn.microsoft.com/en-us/rest/api/azure/devops/?view=azure-devops-rest-7.1)  and [WIQL Queries](https://learn.microsoft.com/en-us/azure/devops/boards/queries/wiql-syntax?view=azure-devops)

The DevOps Boards View App is a standalone Spring Boot App that you can run locally and/or build and run locally, with only Java or Docker needed. It allows you to learn about interacting with the Azure DevOps Rest API and see how you could push/pull data for your own use cases. This app serves as a starter app to pull all projects and specified work items for a given organization, giving a high level glance of each project, quickly, outside of the DevOps product itself. It also rolls up the last changed date to any of the work items that were queried from the REST API and displays that with the project meta data.  

You can certainly import this repository ado-boards-view project into your IDE and run and/or make changes etc... For folks looking to just run the app without having any desire to make changes, there are two options. 

1. install Java 17 JRE and run the ado-boards-view artifact jar. (Step 1 below)
2. install Docker Desktop and and build a Docker container for ado-boards-view and run the container with Docker. (Step 2 below)

## This is NOT official Microsoft documentation but rather an opinionated approach/guidance

## 1. Run the ado-boards-view app using Java
Clone/For/Download the repo. You'll notice ado-boards-view-0.0.1-SNAPSHOT.jar

Run the following command from your CLI of choice

```java -DADO_SERVICES_ORG=org -DADO_SERVICES_PAT=pat -DADO_WORK_ITEM_HISTORY=-120d ADO_REST_API_REQUESTDELAY=1 -jar ado-boards-view-0.0.1-SNAPSHOT.jar```

 - ADO_SERVICES_ORG is your DevOps Org
 - ADO_SERVICES_PAT is your DevOps Personal Access Token with Boards and Projects Read Access
 - ADO_WORK_ITEM_HISTORY is how many days back to query
 - ADO_REST_API_REQUESTDELAY is the delay in ms to wait in between subsequent HTTP request for each project within an organization
 - ADO_WORK_ITEM_TYPES is the work item types to bring back from your Agile processes, the default is to bring back all work items that are found, if you need to override with your own you can do so, for example -DADO_WORK_ITEM_TYPES="Bug,Epic,Feature,Issue,Task,Test Case,Test Plan,Test Suite,User Story" or -DADO_WORK_ITEM_TYPES="Bug,Epic,Some Custom Item" etc....

Head to http://localhost:8080 to view the running ado-boards-view app

![overview view](https://github.com/chtrembl/azure-devops/blob/main/ado-boards-view/overview.png)

![project view](https://github.com/chtrembl/azure-devops/blob/main/ado-boards-view/project.png)

To kill the app from the command line: ```ctrl+c``` from the terminal

## 2. Build and Run the ado-boards-view app using Docker

Forking this repository is probably the easiest way to get started ensuring that you're able to make changes as needed while also easily fetching future changes as the become available. I also encourage pull requests for documentation change request(s), defect(s) and feature suggestions :)

 - Ensure you have Docker installed locally, this will be all you need to build and run this app locally. We will use a Multi Stage Docker build to pull in Maven/Java to build the app and then Docker to build the container image for this app. If you do not have [Docker Desktop](https://www.docker.com/products/docker-desktop/), you will want to get that installed. (Make sure you get the right one for your chipset, Intel/M1 etc..)

 - Head to https://github.com/chtrembl/azure-devops and select clone, fork or select the code button (to download zip) if you want to download instead.

 - Open you CLI of choice and head to your repo clone or extracted zip location and cd into azure-boards-view
 - 
	For example:
```> /Users/christremblay/Development/git/azure-devops/ado-boards-view```
	
	or

	```> /Users/christremblay/Downloads/azure-devops-main/ado-boards-view```


 - If you are running a Mac M1 Chip Machine you only, will need to uncomment a section of the maven pom.xml. Otherwise skip ahead.
 
	For example:
	```vi pom.xml```
	
	Uncomment the dependency element between ```MAC M1 CHIPS ONLY START``` and ```MAC M1 CHIPS ONLY END``` save this file and exit.

 - Back at your root ```../ado-boards-view```
Run the following Docker command to build the ado-view-app and Docker image

	```docker build -t adoboardsview .```

	This should successfully build the docker image and you should see it with the ```docker image ls``` command

 - Make sure you have your Azure DevOps Org handy and an [Azure DevOps PAT](https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops&tabs=Windows
) (Personal Access Token).
	You will need 2 permissions on your token: Project Read Only and Boards Read Only

 - Run the app with the following command:

	```docker run --rm --name adoboardsview -p 8080:8080 -e ADO_SERVICES_ORG=<your org here> -e ADO_SERVICES_PAT=<your pat here> -e ADO_WORK_ITEM_HISTORY=-7d adoboardsview:latest```

	You can now use the app! Visit http://localhost:8080 in your browser.
	
	A little background on the app...
	
	Notice the 3 parameters ```ADO_SERVICES_ORG``` your org, ```ADO_SERVICES_PAT``` your PAT, and 	```ADO_WORK_ITEM_HISTORY``` 

	```ADO_WORK_ITEM_HISTORY``` is the amount of history from the date in which you start the app. For example -7d will indicate to bring back all work items from the last 7 days, for each project individually. You may want to tweak this number, depending on the volume of Work Items you have in each project. There also REST limits on these API's (200 WorkItem Limit per Project).  Also you can view the WIQL query on this in ```/Users/christremblay/Development/git/azure-
devops/ado-boards-view/src/main/resources/wiql/workItemsWIQL.txt```  Keep this in mind because Features/Epics/User Stories will allow you to bring back more items (maintaining lastUpdatedDates) for the children within them (Tasks, Bugs etc...) You will see in the command window / application logs when you run the app how things are behaving, which requests are being made and the size of them.

	Once the app is up you can use it. Note that at the moment, all of the work items are cached in memory at startup. There is no refresh unless you restart the app (Kill the app ```ctrl-c```) and restart it. This is to avoid overloading your ADO instance and exhausting limits. 

	Also there is a 250ms delay between all HTTP requests, to further delay any exhaustion. This parameter is also configurable ```-e ADO_REST_API_REQUESTDELAY=250```

	Perhaps an H2 DB can be introduced to persist workitems from each load to grow the data over time and/or perhaps a scheduled refresh on the in memory cache in the event you plan to deploy this Docker container to a PaaS Azure offering. If you do that, you may considering enabling Spring Security and/or Azure AAD on your app.

	![overview view](https://github.com/chtrembl/azure-devops/blob/main/ado-boards-view/overview.png)

	![project view](https://github.com/chtrembl/azure-devops/blob/main/ado-boards-view/project.png)

 - To kill the app from the command line: ```ctrl+c``` from the terminal
 
 ---

## Legal Notices

Microsoft and any contributors grant you a license to the Microsoft documentation and other content
in this repository under the [Creative Commons Attribution 4.0 International Public License](https://creativecommons.org/licenses/by/4.0/legalcode),
see the [LICENSE](LICENSE) file, and grant you a license to any code in the repository under the [MIT License](https://opensource.org/licenses/MIT), see the
[LICENSE-CODE](LICENSE-CODE) file.

Microsoft, Windows, Microsoft Azure and/or other Microsoft products and services referenced in the documentation
may be either trademarks or registered trademarks of Microsoft in the United States and/or other countries.
The licenses for this project do not grant you rights to use any Microsoft names, logos, or trademarks.
Microsoft's general trademark guidelines can be found at http://go.microsoft.com/fwlink/?LinkID=254653.

Privacy information can be found at https://privacy.microsoft.com/en-us/

Microsoft and any contributors reserve all other rights, whether under their respective copyrights, patents,
or trademarks, whether by implication, estoppel or otherwise.


