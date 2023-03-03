## Azure DevOps Boards View App

Azure DevOps is awesome (as you probably already know)! However there are some really great tools for interacting with its data using the [Azure DevOps Rest API](https://learn.microsoft.com/en-us/rest/api/azure/devops/?view=azure-devops-rest-7.1)  and [WIQL Queries](https://learn.microsoft.com/en-us/azure/devops/boards/queries/wiql-syntax?view=azure-devops)

The DevOps Boards View App is a standalone Spring Boot App that you can build & run locally with only Docker needed! It allows you to learn about interacting with the Azure DevOps Rest API and see how you could push/pull data for your own use cases. This app serves as a starter app to pull all projects and specified work items for a given organization giving a high level glance of each project, quickly, outside of the DevOps product itself. It also rolls up the last changed date to any of the work items that were queried from the REST API and displays that with the project meta data.  

View [Azure DevOps Boards View App](https://github.com/chtrembl/azure-devops/tree/main/ado-boards-view)
