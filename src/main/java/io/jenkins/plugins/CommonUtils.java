package io.jenkins.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

public class CommonUtils {
    private static final String QENGINE_API_URL_PREFIX = "/api/v1/integration/";
    public static final String API_TOKEN = "Bearer ";

    // public static Long executeTestPlan(String portalUrl, String projectID, String testPlanID, String buildName,
    // PrintStream ps)
    public static Long executeTestPlan(QEnginePluginBuilder pluginBuilder) {

        pluginBuilder.getPrintStream().println("Test Plan Execution URL : " + pluginBuilder.getTestPlanUrl());

        HttpClient httpClient = HttpClients.createDefault();
        HttpPatch patchReq = new HttpPatch(pluginBuilder.getTestPlanUrl());
        patchReq.addHeader(HttpHeaders.AUTHORIZATION, API_TOKEN + pluginBuilder.getApiKey());

        if (pluginBuilder.getBuildName() != null) {
            JSONObject buildJson = new JSONObject();
            buildJson.put("name", pluginBuilder.getBuildName());

            JSONObject bodyJson = new JSONObject();
            bodyJson.put("testplan", buildJson);

            StringEntity entity = new StringEntity(bodyJson.toString(), StandardCharsets.UTF_8);
            patchReq.setEntity(entity);
        } // if(buildName != null)
        try {
            HttpResponse response = httpClient.execute(patchReq);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseStr = readContents(response.getEntity().getContent(), pluginBuilder.getPrintStream());
            if (statusCode != HttpStatus.SC_ACCEPTED) {
                pluginBuilder.getPrintStream().println("Failed to initiate Test Plan execution!");
                pluginBuilder.getPrintStream().println(responseStr);
            } // if(statusCode != HttpStatus.SC_ACCEPTED)

            JSONObject responseJson = new JSONObject(responseStr);
            Long runID = responseJson.getJSONObject("testplan").getLong("id");
            pluginBuilder.getPrintStream().println("Run ID\t\t : " + runID);

            return runID;
        } // try
        catch (Exception ex) {
            pluginBuilder
                    .getPrintStream()
                    .println("Exception Occurred while initiating Test Plan execution." + ex.getMessage());
        } // catch (Exception ex)
        return null;
    } // public static Long executeTestPlan (String portalUrl, String projectID, String testPlanID, String buildName,
    // PrintStream ps)

    public static boolean getTestPlanStatus(QEnginePluginBuilder pluginBuilder, Long runID) {
        try {
            String statusUrl = pluginBuilder.getRequestUrl() + QENGINE_API_URL_PREFIX + pluginBuilder.getPortalName()
                    + "/projects/" + pluginBuilder.getProjectID() + "/scheduleexecutions/" + runID;

            pluginBuilder.getPrintStream().println("Waiting 30 seconds for the Test Plan to initiate...");
            pluginBuilder
                    .getPrintStream()
                    .println(
                            "**********************************************************************************************");
            Thread.sleep(30 * 1000L);
            long testPlanStartTime = System.currentTimeMillis();

            int pollCount = 5;
            long pollingInterval = (pluginBuilder.getMaxWaitTime() * 60) / (pollCount - 1) * 1000;

            for (int i = 1; i <= pollCount; i++) {
                HttpGet statusReq = new HttpGet(statusUrl);
                statusReq.addHeader(HttpHeaders.AUTHORIZATION, API_TOKEN + pluginBuilder.getApiKey());
                HttpClient httpClient = HttpClients.createDefault();
                HttpResponse statusResp = httpClient.execute(statusReq);

                int statusCode = statusResp.getStatusLine().getStatusCode();
                String responseStr = readContents(statusResp.getEntity().getContent(), pluginBuilder.getPrintStream());
                if (statusCode != HttpStatus.SC_ACCEPTED) {
                    pluginBuilder.getPrintStream().println("Unable to retrieve the Test Plan execution status.");
                    pluginBuilder.getPrintStream().println(responseStr);
                    return false;
                } // if(statusCode != HttpStatus.SC_ACCEPTED)
                else {
                    JSONObject respJson = new JSONObject(responseStr);
                    String statusStr =
                            respJson.getJSONObject("scheduleexecution").getString("status");

                    switch (statusStr) {
                        case "queued":
                            pluginBuilder
                                    .getPrintStream()
                                    .println("Poll#" + i + " Test Plan Execution is in the queue...");
                            break;
                        case "running":
                            pluginBuilder
                                    .getPrintStream()
                                    .println("Poll#" + i + " Test Plan Execution is in progress...");
                            break;
                        case "onlyManual":
                            pluginBuilder
                                    .getPrintStream()
                                    .println("Poll#" + i + " The Test Plan contains only manual cases...");
                            break;
                        case "stopped":
                        case "terminated":
                            pluginBuilder
                                    .getPrintStream()
                                    .println("Poll#" + i + " Test Plan Execution has been terminated...");
                            pluginBuilder
                                    .getPrintStream()
                                    .println(
                                            "**********************************************************************************************");
                            return false;
                        case "completed":
                            pluginBuilder.getPrintStream().println("Poll#" + i + " Test Plan Execution Completed!!!");
                            long duration = System.currentTimeMillis() - testPlanStartTime;
                            long durationMin = (duration / 1000 / 60);
                            pluginBuilder
                                    .getPrintStream()
                                    .println(
                                            "Duration to complete Test Plan Execution: " + durationMin + " minutes...");
                            pluginBuilder.getPrintStream().println(respJson);
                            pluginBuilder
                                    .getPrintStream()
                                    .println(
                                            "**********************************************************************************************");
                            return true;
                        default:
                            pluginBuilder
                                    .getPrintStream()
                                    .println("Poll#" + i + " Unexpected Execution status - '" + statusStr
                                            + "'. Please refer to the Zoho QEngine results page for further details.");
                            pluginBuilder
                                    .getPrintStream()
                                    .println(
                                            "**********************************************************************************************");
                            return true;
                    } // switch(statusStr)
                    if (i < pollCount) {
                        pluginBuilder
                                .getPrintStream()
                                .println("waiting " + pollingInterval / 1000 + " seconds before next poll..");
                        pluginBuilder
                                .getPrintStream()
                                .println(
                                        "**********************************************************************************************");
                        Thread.sleep(pollingInterval);
                    }
                } // else
            } // for(int i=1; i<= pollCount; i++)
        } // try//try
        catch (Exception ex) {
            ex.printStackTrace();
            pluginBuilder
                    .getPrintStream()
                    .println("Exception occurred while retrieving the Test Plan execution status.");
        } // catch (Exception ex)
        pluginBuilder
                .getPrintStream()
                .println("Unexpected failure. Please refer to the QEngine results page for more details.");
        pluginBuilder
                .getPrintStream()
                .println(
                        "**********************************************************************************************");
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
