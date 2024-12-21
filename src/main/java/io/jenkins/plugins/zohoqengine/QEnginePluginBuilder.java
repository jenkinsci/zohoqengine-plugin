package io.jenkins.plugins.zohoqengine;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.PrintStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class QEnginePluginBuilder extends Builder {
    private String testPlanUrl;
    private Secret apiKey;
    private int maxWaitTime;
    private String buildName;

    @DataBoundConstructor
    public QEnginePluginBuilder(String testPlanUrl, String apiKey, int maxWaitTime, String buildName) {
        this.testPlanUrl = testPlanUrl;
        this.apiKey = Secret.fromString(apiKey);
        this.maxWaitTime = maxWaitTime;
        this.buildName = buildName;
    } // public QEnginePluginBuilder(String portalUrl, Long projectID, Long testPlanID, int maxWaitTime, String
    // buildName)

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        } // public boolean isApplicable(Class<? extends AbstractProject> jobType)

        @Override
        public String getDisplayName() {
            return "Zoho QEngine Test Plan Execution";
        } // public String getDisplayName()

        @POST
        public FormValidation doCheckTestPlanUrl(@QueryParameter String testPlanUrl) {

            if (Util.fixEmptyAndTrim(testPlanUrl) != null) {
                String[] protocolSplit = testPlanUrl.split("//");
                if (protocolSplit.length == 2) {
                    String[] resourceSplit = protocolSplit[1].split("/");
                    if (resourceSplit.length == 7) {
                        return FormValidation.ok();
                    }
                }
            } // if(Util.fixEmptyAndTrim(portalUrl) != null)
            return FormValidation.warning("Please provide a valid Test Plan URL.");
        } // public FormValidation doCheckPortalUrl (@QueryParameter String portalUrl)

        @POST
        public FormValidation doCheckApiKey(@QueryParameter String apiKey) {
            if (apiKey != null) {
                return FormValidation.ok();
            }
            return FormValidation.warning("Please provide a valid API key.");
        }

        @POST
        public FormValidation doCheckMaxWaitTime(@QueryParameter int maxWaitTime) {
            if (maxWaitTime > 0) {
                return FormValidation.ok();
            } // if(maxWaitTime > 0)
            return FormValidation.warning("Please enter a valid maximum wait time.");
        } // public FormValidation doCheckMaxWaitTime (@QueryParameter int maxWaitTime)

        @POST
        public FormValidation doCheckBuildName(@QueryParameter String buildName) {
            // Here we need to validate pattern match
            return FormValidation.ok();
        } // public FormValidation doCheckBuildName (@QueryParameter String buildName)
    } // public static class Descriptor extends BuildStepDescriptor<Builder>

    @Override
    public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener) {
        PrintStream ps = listener.getLogger();
        String requestUrl = null, portalName = null, projectID = null, testPlanID = null;

        ps.print("\n************************* QEngine : Start executing the test plan **************************\n");

        if (Util.fixEmptyAndTrim(testPlanUrl) == null) {
            listener.error("The Test Plan URL is empty.");
        } else {
            String[] protocolSplit = testPlanUrl.split("//");
            if (protocolSplit.length == 2) {
                String[] resourceSplit = protocolSplit[1].split("/");
                if (resourceSplit.length == 7) {
                    requestUrl = protocolSplit[0] + "//" + resourceSplit[0];
                    portalName = resourceSplit[1];
                    projectID = resourceSplit[3];
                    testPlanID = resourceSplit[5];
                }
            }
        }

        if (Util.fixEmptyAndTrim(apiKey.getPlainText().trim()) == null) {
            listener.error("The API key is empty.");
        }

        if (Util.fixEmptyAndTrim(requestUrl) == null) {
            listener.error("Please check the provided URL.");
        }

        Long projectId = CommonUtils.extractLongValue(projectID);
        if (projectId == null) {
            listener.error("Please check the provided URL.");
        } // if(projectId == null)

        Long testPlanId = CommonUtils.extractLongValue(testPlanID);
        if (testPlanId == null) {
            listener.error("Please check the provided URL.");
        } // if(testPlanId == null)
        if (maxWaitTime < 0) {
            maxWaitTime = 180;
        } // if(maxWaitTime < 0)

        buildName = Util.fixEmptyAndTrim(buildName);

        Long runID = CommonUtils.executeTestPlan(apiKey, requestUrl, portalName, projectID, testPlanID, buildName, ps);
        if (runID != null) {
            return CommonUtils.getTestPlanStatus(apiKey, requestUrl, portalName, projectID, runID, ps, maxWaitTime);
        } // if(runID != null)

        return false;
    } // public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener)

    /**
     * Method to verify build paramas
     * @param listener
     */
    private void parseAndVerifyParams(
            BuildListener listener) {} // private void parseAndVerifyParams (BuildListener listener)

    /**
     * @return the testPlanUrl
     */
    public String getTestPlanUrl() {
        return testPlanUrl;
    }

    /**
     * @param testPlanUrl the testPlanUrl to set
     */
    public void setTestPlanUrl(String testPlanUrl) {
        this.testPlanUrl = testPlanUrl;
    }

    /**
     * @return the apiKey
     */
    public Secret getApiKey() {
        return this.apiKey;
    }

    /**
     * @param apiKey the apiKey to set
     */
    public void setApiKey(String apiKey) {
        this.apiKey = Secret.fromString(apiKey);
    }

    /**
     * @return the maxWaitTime
     */
    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * @param maxWaitTime the maxWaitTime to set
     */
    public void setMaxWaitTime(int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * @return the buildName
     */
    public String getBuildName() {
        return buildName;
    }

    /**
     * @param buildName the buildName to set
     */
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }
}
