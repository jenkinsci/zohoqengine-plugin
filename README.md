# Zoho QEngine

## About Zoho QEngine

**Zoho QEngine** is a cloud-based test automation platform designed to streamline software testing for web, API, and mobile applications. With features such as scriptless testing, self-healing capabilities, and detailed reporting, QEngine simplifies quality assurance, enhances efficiency, and ensures robust application performance.

To learn more about Zoho QEngine, please refer to our [User Guide](https://help.zoho.com/portal/en/kb/qengine/user-guide).

## Jenkins Integration

Integrating Zoho QEngine with Jenkins enables automated test execution, enhances CI/CD workflows, and provides real-time feedback. This integration improves reporting, fosters collaboration, and accelerates issue detection while supporting scalable and efficient test management.

To get started, install the **Zoho QEngine Test Plan Execution** Plugin in Jenkins to trigger test executions.

For more details about this integration, please refer to the [Integration](https://help.zoho.com/portal/en/kb/qengine/user-guide/setup/integration/articles/integrating-zoho-qengine-with-jenkins) page.



## Pre-requisites for Configuration

- **API Key**: Generate an API Key in Zoho QEngine.
- **Test Plan Execution URL**: Retrieve the Execution URL from the Test Plans section in Zoho QEngine.


## Steps to Configure

1.	**Create a New Pipeline**
	<p>Set up a new pipeline in Jenkins.</p><br>

2.	**Add a Build Step with the Zoho QEngine Plugin**
	<p>Include a build step that utilizes the QEngine plugin.</p><br>

3.	**Provide Build Step Details**
	**<p>Test Plan Execution URL**: Enter the execution URL obtained from Zoho QEngine.</p>
	**<p>API Key**: Input the [API key](https://help.zoho.com/portal/en/kb/qengine/user-guide/setup/api-keys/articles/generate-api-keys-in-zoho-qengine#1_Overview) in Zoho QEngine.</p>
	**<p>Maximum Wait Time**: Define the maximum expected time (in minutes) for the test plan to complete. If this time is exceeded, the build will terminate and be marked as failed.</p>
	**<p>Build Name**: Assign a name to the build.</p><br>

4.	**Execute the Build**
	<p>Trigger the build to initiate test execution.</p>


## Support

For any issues or further assistance, please contact us at support@zohoqengine.com

To know more about Zoho QEngine, visit [https://www.zoho.com/qengine/](https://www.zoho.com/qengine/).


