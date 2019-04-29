// jenkins-master-shared-library imports
import main.groovy.cicd.pipeline.settings.PipelineSettings;
import main.groovy.cicd.pipeline.plugins.CheckmarxPluginWrapper;
import main.groovy.cicd.pipeline.helpers.Utilities;

// jenkins API imports
import hudson.FilePath;
import jenkins.model.Jenkins;

def call(reportingConfig)
{
    initializeApplicationMetadata(reportingConfig.uai, reportingConfig.ci);
    initializeCodeCheckoutSettings();
    initializeSonarQubeSettings();
    initializeCheckmarxSettings();
    initializePipelineMetadata();
    initializeBuildSettings();
    //initializePublishSettings(reportingConfig);
    initializeDeploySettings(reportingConfig);
    initializeReportingSettings(reportingConfig);
    initializeProxySettings(reportingConfig);
}

private def initializeApplicationMetadata(uai, ci)
{
    /*
    *   Application Metadata settings
    */
    // Pipeline config settings passed in as arguments in scripted pipeline from Jenkinsfile
    PipelineSettings.ApplicationMetadata.uai = uai;
    PipelineSettings.ApplicationMetadata.ci = ci;
}

private def initializeCodeCheckoutSettings()
{
    def gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim();

    /*
    *   Code Checkout settings
    */
    // SCM config - magic strings
    // TODO - pass these in as envvars or params
    PipelineSettings.CodeCheckoutSettings.scm = 'github';
    PipelineSettings.CodeCheckoutSettings.baseUrl = 'https://github.build.ge.com';

    // Pipeline introspection
    def repoLocationTokens = JOB_NAME.split('/');
    PipelineSettings.CodeCheckoutSettings.org = repoLocationTokens[1];
    PipelineSettings.CodeCheckoutSettings.repo = repoLocationTokens[2];
    PipelineSettings.CodeCheckoutSettings.branch = repoLocationTokens[3];
    PipelineSettings.CodeCheckoutSettings.currentCommit = gitCommit;
}

private def initializeSonarQubeSettings()
{
    /*
    *   SonarQube settings
    */
    // Set project key to use when calling SQ REST API
    PipelineSettings.SonarQubeSettings.projectKey = "${PipelineSettings.CodeCheckoutSettings.repo}:${PipelineSettings.CodeCheckoutSettings.branch}";

    // Environment variables set with global SQ closure
    withSonarQubeEnv('SonarQube') {
        PipelineSettings.SonarQubeSettings.sonarHostUrl = env.SONAR_HOST_URL;
        PipelineSettings.SonarQubeSettings.sonarAuthToken = env.SONAR_AUTH_TOKEN;
    }
    PipelineSettings.SonarQubeSettings.initializeQualityMetrics();
}

private def initializeCheckmarxSettings()
{
    PipelineSettings.CheckmarxSettings.initializeSastMetrics();
}

private def initializePipelineMetadata()
{
    /*
    *   Pipeline Metadata settings
    */
    // Pipeline introspection
    PipelineSettings.PipelineMetadata.url = JENKINS_URL;

    // Version of shared library - just set null for now, it's not important
    PipelineSettings.PipelineMetadata.version = null;
}

private def initializeBuildSettings()
{
    // conditionally open file path on java.io.File(workspace) if node is master
    def nodeWorkspace = null;
    if(NODE_NAME == 'master' || env.NODE_NAME == null)
        nodeWorkspace = new FilePath(new File(pwd()));
    else
        nodeWorkspace = new FilePath(Jenkins.getInstance().getComputer(NODE_NAME).getChannel(), pwd());

    def maven = nodeWorkspace.child('pom.xml');
    def nodejs = nodeWorkspace.child('package.json');
    def settings = [:];

    // this implementation won't support multiple project types in a single repo
    if(maven.exists())
        settings = Utilities.getBuildSettingsFromPomXml(maven.readToString());

    if(nodejs.exists())
        settings = Utilities.getBuildSettingsFromPackageJson(nodejs.readToString());

    /*
    *   Build Settings
    */
    PipelineSettings.BuildSettings.artifactName = settings['artifactName'];
    PipelineSettings.BuildSettings.version = settings['version'];
}

private def initializePublishSettings()
{
    def nodeWorkspace = new FilePath(Jenkins.getInstance().getComputer(NODE_NAME).getChannel(), pwd());
    def publishScript = nodeWorkspace.child('publishScript.sh');
    def scriptContents = [];

    if (publishScript.exists())
        scriptContents = publishScript.readToString().tokenize('\n');

    def settings = Utilities.readScriptContents(scriptContents, ['dockerRepoUrl']);

    /*
    *   Publish settings
    */
    PipelineSettings.PublishSettings.repo = settings['dockerRepoUrl'];
    PipelineSettings.PublishSettings.hash = '';
    PipelineSettings.PublishSettings.password = '';
    PipelineSettings.PublishSettings.key = '';
}

private def initializeDeploySettings(reportingConfig)
{
    def configKey = reportingConfig.environments.keySet().find { BRANCH_NAME ==~ it }
    def config = reportingConfig.environments[configKey]
    PipelineSettings.DeploySettings.environment = config.key
}

private def initializeReportingSettings(reportingConfig)
{
    /*
    *   Reporting settings
    */
    // Read URLs from parameter map and get UAA credentials from Jenkins
    PipelineSettings.ReportingSettings.metricsUrl = reportingConfig.metricsUrl;
    PipelineSettings.ReportingSettings.uaaUrl = reportingConfig.uaaUrl;

    withCredentials([usernamePassword(credentialsId: reportingConfig.uaaCredId, usernameVariable: 'UAA_CLIENT_ID', passwordVariable: 'UAA_CLIENT_SECRET')]) {
        PipelineSettings.ReportingSettings.uaaClientId = UAA_CLIENT_ID;
        PipelineSettings.ReportingSettings.uaaClientSecret = UAA_CLIENT_SECRET;
    }
}

private def initializeProxySettings(reportingConfig)
{
    /*
    *   Proxy settings
    */
    if (reportingConfig.proxyHost && reportingConfig.proxyPort) {
        PipelineSettings.ProxySettings.proxyHost = reportingConfig.proxyHost;
        PipelineSettings.ProxySettings.proxyPort = reportingConfig.proxyPort;
    }

}