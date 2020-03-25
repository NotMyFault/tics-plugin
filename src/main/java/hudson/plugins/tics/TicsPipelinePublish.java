package hudson.plugins.tics;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Strings;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class TicsPipelinePublish extends Recorder implements SimpleBuildStep {

    private static final String PUBLISH_TICS_RESULTS = "publishTicsResults";
    public final String viewerUrl;
    public String projectName;
    public String branchName;
    public String ticsProjectPath;
    public String userName;
    public String userId;
    public boolean checkQualityGate;
    public boolean failIfQualityGateFails;

    @DataBoundConstructor
    public TicsPipelinePublish(
            final String viewerUrl
    ) {
        this.viewerUrl = viewerUrl;
    }

    @Override
    public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener) throws InterruptedException, IOException {
        if (Strings.isNullOrEmpty(viewerUrl)) {
            throw new IllegalArgumentException("The pipeline method '" + PUBLISH_TICS_RESULTS + "' was used without specifying the 'viewerUrl'. " +
                    "For instance a 'viewerUrl' looks like: 'http://www.company.com:42506/tiobeweb/TICS'.\n");
        }
        if (Strings.isNullOrEmpty(projectName) || Strings.isNullOrEmpty(branchName)) {
            if (Strings.isNullOrEmpty(ticsProjectPath)) {
                throw new IllegalArgumentException("The pipeline method '" + PUBLISH_TICS_RESULTS + "' was used without specifying the 'projectName' or the 'branchName'.");
            }
        }

        final String credentialsId = getCredentials();
        final TicsPublisher tp = new TicsPublisher(viewerUrl, getTicsProjectPath(), credentialsId, this.checkQualityGate, this.failIfQualityGateFails);
        tp.perform(run, workspace, launcher, listener);
    }

    private String getTicsProjectPath() {
        if (Strings.isNullOrEmpty(ticsProjectPath)) {
            return String.join("/","HIE:/", projectName, branchName);
        } else {
            return this.ticsProjectPath;
        }
    }

    private String getCredentials() {
        if (Strings.isNullOrEmpty(this.userName) && Strings.isNullOrEmpty(this.userId)) {
            return "";
        }
        if (!Strings.isNullOrEmpty(this.userName) && !Strings.isNullOrEmpty(this.userId)) {
            throw new IllegalArgumentException("Please specify either one of the the 'userName' or 'userId' and not both.");
        }
        if (!Strings.isNullOrEmpty(this.userId)) {
            return findUserCredentials("userId" , this.userId, StandardUsernamePasswordCredentials::getId);
        } else {
            return findUserCredentials("userName" , this.userName, StandardUsernamePasswordCredentials::getUsername);
        }
    }

    private String findUserCredentials(String targetName, String targetValue, java.util.function.Function<StandardUsernamePasswordCredentials, String> func) {
        final List<StandardUsernamePasswordCredentials> credentialsList = CredentialsProvider.
                lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstanceOrNull(),
                        null,
                        (DomainRequirement) null);
        final String exceptionMessage = "No credentials found for " + targetName + ":'" + targetValue + "'" + " in Jenkins credentials store.";
        final StandardUsernamePasswordCredentials findUserInCredentialsList = credentialsList.stream()
                .filter(c -> func.apply(c).equals(targetValue))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(exceptionMessage));
        return findUserInCredentialsList.getId();
    }

    @DataBoundSetter
    public void setProjectName(String value) {
        this.projectName = value;
    }

    @DataBoundSetter
    public void setBranchName(String value) {
        this.branchName = value;
    }

    @DataBoundSetter
    public void setTicsProjectPath(String value) {
        this.ticsProjectPath = value;
    }

    @DataBoundSetter
    public void setUserName(String value) {
        this.userName = value;
    }

    @DataBoundSetter
    public void setUserId(String value) {
        this.userId = value;
    }

    @DataBoundSetter
    public void setCheckQualityGate(boolean value) {
        this.checkQualityGate = value;
    }

    @DataBoundSetter
    public void setFailIfQualityGateFails(boolean value) {
        this.failIfQualityGateFails = value;
    }

    @Symbol(PUBLISH_TICS_RESULTS) @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Nonnull
        @Override
        public String getDisplayName() {
            return "";
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
