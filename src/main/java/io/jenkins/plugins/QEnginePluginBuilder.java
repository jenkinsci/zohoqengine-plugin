package io.jenkins.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.PrintStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class QEnginePluginBuilder extends Builder {
    private String requestUrl;
    private String testPlanUrl;
    private String apiKey;
    private String portalName;
    private String projectID;
    private String testPlanID;
    private int maxWaitTime;
    private String buildName;
    private PrintStream ps;

    @DataBoundConstructor
    public QEnginePluginBuilder(String testPlanUrl, String apiKey, int maxWaitTime, String buildName) {
        this.testPlanUrl = testPlanUrl;
        this.apiKey = apiKey;
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
            if (Util.fixEmptyAndTrim(apiKey) != null) {
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
        ps = listener.getLogger();
        ps.print("\n************************* QEngine : Start executing the test plan **************************\n");
        parseAndVerifyParams(listener);
        // Long runID = CommonUtils.executeTestPlan(testPlanUrl, projectID, testPlanID, buildName, ps);
        Long runID = CommonUtils.executeTestPlan(this);
        if (runID != null) {
            return CommonUtils.getTestPlanStatus(this, runID);
        } // if(runID != null)

        return false;
    } // public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener)

    /**
     * Method to verify build paramas
     * @param listener
     */
    private void parseAndVerifyParams(BuildListener listener) {
        if (Util.fixEmptyAndTrim(testPlanUrl) == null) {
            listener.error("The Test Plan URL is empty.");
        } else {
            String[] protocolSplit = testPlanUrl.split("//");
            if (protocolSplit.length == 2) {
                String[] resourceSplit = protocolSplit[1].split("/");
                if (resourceSplit.length == 7) {
                    this.requestUrl = protocolSplit[0] + "//" + resourceSplit[0];
                    this.portalName = resourceSplit[1];
                    this.projectID = resourceSplit[3];
                    this.testPlanID = resourceSplit[5];
                }
            }
        }

        if (Util.fixEmptyAndTrim(apiKey) == null) {
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
    } // private void parseAndVerifyParams (BuildListener listener)

    /**
     * @return the portalUrl
     */
    public String getTestPlanUrl() {
        return testPlanUrl;
    }

    /**
     * @param portalUrl the portalUrl to set
     */
    public void setPortalUrl(String testPlanUrl) {
        this.testPlanUrl = testPlanUrl;
    }

    /**
     * @return the projectID
     */
    public String getProjectID() {
        return projectID;
    }

    /**
     * @param projectID the projectID to set
     */
    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    /**
     * @return the testPlanID
     */
    public String getTestPlanID() {
        return testPlanID;
    }

    /**
     * @param testPlanID the testPlanID to set
     */
    public void setTestPlanID(String testPlanID) {
        this.testPlanID = testPlanID;
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

    public void setPrintStream(PrintStream ps) {
        this.ps = ps;
    }

    public PrintStream getPrintStream() {
        return this.ps;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestUrl() {
        return this.requestUrl;
    }

    public void setPortalName(String portalName) {
        this.portalName = portalName;
    }

    public String getPortalName() {
        return this.portalName;
    }

    public void setApiKey(String apikey) {
        this.apiKey = apikey;
    }

    public String getApiKey() {
        return this.apiKey;
    }
}
