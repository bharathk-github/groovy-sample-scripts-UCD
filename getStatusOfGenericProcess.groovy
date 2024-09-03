/*
* FUNCTION: This script prints out status of a generic process
* INPUTS: Pass username, password, hostname and application-process-request-id when calling the script
* WORD OF CAUTION: This is a loosely tested script, use it with caution
*/

import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder2
import groovy.json.JsonSlurper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils

if (args.length == 0) {
    println "No arguments provided!"
    println "Usage: groovy cript.groovy username password https://ucdserver:port request-process-id"
    System.exit(1)
}

username = args[0]
password = args[1]
host = args[2]
requestId =args[3]

builder = new HttpClientBuilder2();
builder.setUsername(username);
builder.setPassword(password);
builder.setTrustAllCerts(true);

def responseData = getProcessResponseLog(requestId)
println(responseData)

def getProcessResponseLog(String requestId) {
    url = host + "/cli/processRequest/" + requestId
    URL url = new URL(url);
    def slurper = new JsonSlurper();
    Set<String> results = new HashSet<>(List.of("CANCELED","FAILED TO START","FAULTED","SUCCEEDED"))
    def result
    while(true) {
        def data = getWorkflowDataFromServer(url)
        def responseJson = slurper.parse(new StringReader(data))
        result = responseJson.result
        if(results.contains(result))
            break;
    }
    return result
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
    if (code > 210)
        throw new RuntimeException("Invalid response from server " + code)

}

