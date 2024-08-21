/*
* This script is licensed under:
* Apache License
*   Version 2.0, January 2004
*   http://www.apache.org/licenses/
* This script takes a application process request id and prints out logs of failed plugin step
* If 2 steps fails then it will print logs for both the steps
* It does not work for a generic process request, however if a generic request is submitted through a application process-
* -then it can get failed step logs from the generic process
*  Substitute username, password, hostname and application-process-request-id with appropriate data when using
* Tested with IBM UrbanCode Deploy 8.0.1
* Command line usage:
* groovy -cp udclient.jar GetFailedStepLogOfAppProcess.groovy
*/

import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder2
import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils

FAILED = "FAULTED"
username = "admin"
password = "admin"
host = "https://X.X.X.X:8443"
applicationProcessRequestId = ""

builder = new HttpClientBuilder2();
builder.setUsername(username);
builder.setPassword(password);
builder.setTrustAllCerts(true);

def appProcResponseData = getAppProcessResponseLog(applicationProcessRequestId)
jsonSlurper = new JsonSlurper();
def jsonObject = jsonSlurper.parseText(appProcResponseData);
printFailedStepFromProcessResponse(jsonObject.children)


def getAppProcessResponseLog(String requestId) {
    url = host + "/rest/workflow/applicationProcessRequest/" + requestId
    URL url = new URL(url);
    def data = getWorkflowDataFromServer(url)
    return data
}

def printFailedStepFromProcessResponse(ArrayList<String> list) {
    for (Object child : list) {
        if (child.result.equals(FAILED)) {
            def processType = child.type
            if (processType.equals("componentProcess")) {
                getStdoutFromComponentProcessTraceLogs(child.componentProcessRequestId)
            } else if (child.type.equals("runProcess")) {
                getStdoutFromGenericProcessTraceLogs(child.childRequestId)
            } else if (child.children != null & !child.children.isEmpty()) {
                printFailedStepFromProcessResponse(child.children)
            }
        }
    }
}

def getStdoutFromComponentProcessTraceLogs(String requestId) {
    def componentProcessTrace = getComponentProcessChildren(requestId)
    def compProcessTraceJson = jsonSlurper.parseText(componentProcessTrace);
    download(compProcessTraceJson)
}

def getStdoutFromGenericProcessTraceLogs(String requestId) {
    def genericProcessTrace = getProcessChildren(requestId)
    def genericTraceJson = jsonSlurper.parseText(genericProcessTrace);
    download(genericTraceJson)
}

def getComponentProcessChildren(String requestId) {
    def compProcChildrenUrl = "/rest/workflow/componentProcessRequestChildren/"
    URL url = new URL(host + compProcChildrenUrl + requestId);
    def data = getWorkflowDataFromServer(url)
    return data;
}

def getProcessChildren(String requestId) {
    def requestProcChildren = "/rest/process/request/"
    def traceChildren = "traceChildren"
    URL url = new URL(host + requestProcChildren + requestId + "/" + traceChildren);
    def data = getWorkflowDataFromServer(url)
    return data;
}

private void download(stepList) {
    def workflowTraceId = null
    def id = null
    for (def traceJson : stepList) {
        if (traceJson.result.equals(FAILED)) {
            workflowTraceId = traceJson.workflowTraceId
            id = traceJson.id
            downloadStdoutFromServer(workflowTraceId, id)
        }
    }
    if (workflowTraceId == null || id == null)
        throw new RuntimeException("Error fetching workflow trace id from following object " + traceJson)
}


def downloadStdoutFromServer(String workflowTraceId, String id) {
    def logViewTrace = "/rest/logView/trace/"
    def stdout = "/stdOut.txt"
    String url = host + logViewTrace + workflowTraceId + "/" + id + stdout
    println("Getting logs from " + url)
    URL httpUrl = new URL(url)
    CloseableHttpClient client = null;
    try {
        client = builder.buildClient()
        HttpUriRequest request = new HttpGet(httpUrl.toURI());
        request.setHeader("Content-Type", "application/octet-stream")
        CloseableHttpResponse response = client.execute(request);
        checkResponse(response)
        if (response.getStatusLine().getStatusCode() > 299)
            throw new RuntimeException("Unable to fetch logs from URI " + url.toString())

        InputStream is = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        String line;
        println "********************************************************************************************************"
        while ((line = reader.readLine()) != null) {
            println(line)
        }
        println "********************************************************************************************************"
    } finally {
        client.close();
    }
}

String getWorkflowDataFromServer(URL url) {
    CloseableHttpClient client = null;
    try {
        client = builder.buildClient()
        HttpUriRequest request = new HttpGet(url.toURI());
        CloseableHttpResponse response = client.execute(request);
        checkResponse(response)
        return EntityUtils.toString(response.getEntity());
    } finally {
        client.close();
    }
}

def checkResponse(CloseableHttpResponse response) {
    def code = response.getStatusLine().getStatusCode()
    if (code > 299)
        throw new RuntimeException("Invalid response from server " + code)
}