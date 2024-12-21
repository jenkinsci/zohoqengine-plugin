package io.jenkins.plugins.zohoqengine;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class CommonUtils {
    private static final String QENGINE_API_URL_PREFIX = "/api/v1/integration/";
    private static final String API_TOKEN = "Bearer ";
    private static final String AUTHORIZATION = "Authorization";

    public static Long executeTestPlan(
            Secret apiKey,
            String requestUrl,
            String portalName,
            String projectID,
            String testPlanID,
            String buildName,
            PrintStream ps) {
        try {

            String executeUrl = requestUrl + QENGINE_API_URL_PREFIX + portalName + "/projects/" + projectID
                    + "/testplans/" + testPlanID + "/execute";

            ps.println("Test Plan Execution URL : " + executeUrl);

            HttpClient httpClient = ProxyConfiguration.newHttpClient();
            HttpRequest.Builder httpRequest = ProxyConfiguration.newHttpRequestBuilder(new URI(executeUrl));
            httpRequest.setHeader(
                    AUTHORIZATION, API_TOKEN + apiKey.getPlainText().trim());

            if (buildName != null) {
                JSONObject buildJson = new JSONObject();
                buildJson.put("name", buildName);

                JSONObject bodyJson = new JSONObject();
                bodyJson.put("testplan", buildJson);

                httpRequest.method("PATCH", BodyPublishers.ofString(bodyJson.toString(), StandardCharsets.UTF_8));
            } // if(buildName != null)
            HttpResponse<String> httpResponse = httpClient.send(httpRequest.build(), BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();
            String responseStr = httpResponse.body();
            if (statusCode != 202) {
                ps.println("Failed to initiate Test Plan execution!");
                ps.println(responseStr);
            } // if(statusCode != HttpStatus.SC_ACCEPTED)

            JSONObject responseJson = new JSONObject(responseStr);
            Long runID = responseJson.getJSONObject("testplan").getLong("id");
            ps.println("Run ID\t\t : " + runID);

            return runID;
        } // try
        catch (Exception ex) {
            ps.println("Exception Occurred while initiating Test Plan execution." + ex.getMessage());
        } // catch (Exception ex)
        return null;
    } // public static Long executeTestPlan (String portalUrl, String projectID, String testPlanID, String buildName,
    // PrintStream ps)

    public static boolean getTestPlanStatus(
            Secret apiKey,
            String requestUrl,
            String portalName,
            String projectID,
            Long runID,
            PrintStream ps,
            int maxWaitTime) {
        try {
            String statusUrl = requestUrl + QENGINE_API_URL_PREFIX + portalName + "/projects/" + projectID
                    + "/scheduleexecutions/" + runID;

            ps.println("Waiting 30 seconds for the Test Plan to initiate...");
            ps.println(
                    "**********************************************************************************************");
            Thread.sleep(30 * 1000L);
            long testPlanStartTime = System.currentTimeMillis();

            int pollCount = 5;
            long pollingInterval = (maxWaitTime * 60) / (pollCount - 1) * 1000;

            for (int i = 1; i <= pollCount; i++) {
                HttpClient httpClient = ProxyConfiguration.newHttpClient();
                HttpRequest.Builder httpRequest = ProxyConfiguration.newHttpRequestBuilder(new URI(statusUrl));
                httpRequest.setHeader(
                        AUTHORIZATION, API_TOKEN + apiKey.getPlainText().trim());
                HttpResponse<String> httpResponse =
                        httpClient.send(httpRequest.GET().build(), BodyHandlers.ofString());
                int statusCode = httpResponse.statusCode();
                String responseStr = httpResponse.body();
                if (statusCode != 202) {
                    ps.println("Unable to retrieve the Test Plan execution status.");
                    ps.println(responseStr);
                    return false;
                } // if(statusCode != HttpStatus.SC_ACCEPTED)
                else {
                    JSONObject respJson = new JSONObject(responseStr);
                    String statusStr =
                            respJson.getJSONObject("scheduleexecution").getString("status");

                    switch (statusStr) {
                        case "queued":
                            ps.println("Poll#" + i + " Test Plan Execution is in the queue...");
                            break;
                        case "running":
                            ps.println("Poll#" + i + " Test Plan Execution is in progress...");
                            break;
                        case "onlyManual":
                            ps.println("Poll#" + i + " The Test Plan contains only manual cases...");
                            break;
                        case "stopped":
                        case "terminated":
                            ps.println("Poll#" + i + " Test Plan Execution has been terminated...");
                            ps.println(
                                    "**********************************************************************************************");
                            return false;
                        case "completed":
                            ps.println("Poll#" + i + " Test Plan Execution Completed!!!");
                            long duration = System.currentTimeMillis() - testPlanStartTime;
                            long durationMin = (duration / 1000 / 60);
                            ps.println("Duration to complete Test Plan Execution: " + durationMin + " minutes...");
                            ps.println(respJson);
                            ps.println(
                                    "**********************************************************************************************");
                            return true;
                        default:
                            ps.println("Poll#" + i + " Unexpected Execution status - '" + statusStr
                                    + "'. Please refer to the Zoho QEngine results page for further details.");
                            ps.println(
                                    "**********************************************************************************************");
                            return true;
                    } // switch(statusStr)
                    if (i < pollCount) {
                        ps.println("waiting " + pollingInterval / 1000 + " seconds before next poll..");
                        ps.println(
                                "**********************************************************************************************");
                        Thread.sleep(pollingInterval);
                    }
                } // else
            } // for(int i=1; i<= pollCount; i++)
        } // try//try
        catch (Exception ex) {
            ex.printStackTrace();
            ps.println("Exception occurred while retrieving the Test Plan execution status.");
        } // catch (Exception ex)
        ps.println("Unexpected failure. Please refer to the QEngine results page for more details.");
        ps.println("**********************************************************************************************");
        return false;
    } // public static boolean getTestPlanStatus (String portalUrl, String projectID, int maxWaitTime, Long runID,
    // PrintStream ps)

    /**
     * Method to read string content from inputstream
     * @param in
     * @param ps
     * @return
     */
    public static String readContents(InputStream in, PrintStream ps) {
        try {
            char[] buf = new char[500000]; // even number so should not cut off unicode bytes
            StringBuilder sb = new StringBuilder();
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            synchronized (in) {
                int readBytes = 0;
                while ((readBytes = isr.read(buf, 0, buf.length)) >= 0) {
                    sb.append(buf, 0, readBytes);
                }
            }
            return sb.toString();
        } // try
        catch (IOException e) {
            ps.println("Exception on read response from server ");
            return null;
        } // catch (IOException e)
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
                ps.println("Unable to close inputstream");
            }
            ;
        } // finally
    } // public static String readContents(InputStream in, PrintStream ps)

    /**
     * Method to extract long value from string
     * @param stringValue
     * @return
     */
    public static Long extractLongValue(String stringValue) {
        try {
            Long longValue = Long.valueOf(stringValue);
            if (longValue > 0L) {
                return longValue;
            } // if(longValue > 0L)
        } // ty
        catch (Exception ex) {
            // No Need to handle exception just return null
        } // catch(Exception ex)
        return null;
    } // public static Long extractLongValue (String stringValue)
} // public class CommonUtils
