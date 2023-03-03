## Azure DevOps Boards View App

In this section, we'll begin to set up your environment both for local development (Optional) and for Azure.

## Building and Running the ado-boards-view app locally

Forking this repository is probably the easiest way to get started ensuring that you're able to make changes as needed while also easily fetching future changes as the become available. I also encourage pull requests for documentation change request(s), defect(s) and feature suggestions :)

 - Ensure you have Docker installed locally, this will be all you need to build and run this app locally. We will use a Multi Stage Docker build to pull in Maven/Java to build the app and then Docker to build the container image for this app. If you do not have Docker Desktop installed, Head to https://www.docker.com/products/docker-desktop/ and download & install it.

 - Head to https://github.com/chtrembl/azure-devops and select Fork in the top right or Code Button (Download Zip)

 - Open you CLI of choice and head to your repo clone or extracted zip location and cd into azure-devops/azure-boards-view
	For example:
```> /Users/christremblay/Development/git/ado-boards-view```

 - If you are running a Mac M1 Chip Machine you only, will need to uncomment a section of the maven pom.xml. Otherwise skip ahead.
	For example:
	```vi pom.xml```
	
	Uncomment the dependency element between ```MAC M1 CHIPS ONLY START``` and ```MAC M1 CHIPS ONLY END``` save this file and exit.

 - Back at your root ```> /Users/christremblay/Development/git/ado-boards-view```
Run the following Docker command to build the ado-view-app and Docker image

	```docker build -t adoboardsview .```

	This should successfully build the docker image and you should see it with the ```docker image ls``` command

- Make sure you have you Azure DevOps Org handy and an Azure DevOps PAT (Personal Access Token).  Info on how to do that here https://learn.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops&tabs=Windows

	You will need 2 permission on your token: Project Read Only and Boards Read Only

- Run the app with the following command:

	```docker run --rm --name adoboardsview -p 8080:8080 -e ADO_SERVICES_ORG=<your org here> -e ADO_SERVICES_PAT=<your pat here> -e ADO_WORK_ITEM_HISTORY=-7d adoboardsview:latest```

	visit http://localhost:8080

	Notice the 3 parameters ```ADO_SERVICES_ORG``` your org, ```ADO_SERVICES_PAT``` your PAT, and 	```ADO_WORK_ITEM_HISTORY``` 

	```ADO_WORK_ITEM_HISTORY``` is the amount of history  from the date in which you start the app. For example -7d will indicate to bring back all work items from the last 7 days, for each project individually. You may want to tweak this number, depending on the volume of Work Items you have in each project. There also REST limits on these API's. Also you can view the WIQL query on this in ```/Users/christremblay/Development/git/azure-
devops/ado-boards-view/src/main/resources/wiql/workItemsWIQL.txt``` 

	The default query is pulling every work item back (Epics, Features, Bugs, Tasks etc...) Perhaps you want to limit this to just Epics, Features or just User Stories or whatever you prefer to keep your data manageable. You will see in the command window / application logs when you run the app how things are behaving, which requests are being made and the size of them.

	Once the app is up you can use it. Note that at the moment all of the work items are cached in memory at startup. There is no refresh unless you restart the app (Kill the app ctrl-c) and restart it. This is to avoid overloading your ADO instance and exhausting limits. Also there is a 250ms delay between all HTTP requests, to further delay any exhaustion. This parameter is also configurable ```-e ADO_REST_API_REQUESTDELAY=250```

	![overview view](https://github.com/chtrembl/azure-devops/blob/main/ado-boards-view/overview.png)

	![project view](https://github.com/chtrembl/azure-devops/blob/main/ado-boards-view/project.png)
