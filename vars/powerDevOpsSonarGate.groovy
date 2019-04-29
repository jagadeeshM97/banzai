import main.groovy.cicd.pipeline.services.SonarQubeService;
import main.groovy.cicd.pipeline.helpers.SonarQubeHelper;
import main.groovy.cicd.pipeline.settings.PipelineSettings;

def call(Boolean proxiesOn = false)
{
    sleep 30;
    // initialize web service and structures
    def sqService = new SonarQubeService(PipelineSettings.SonarQubeSettings.sonarHostUrl, PipelineSettings.SonarQubeSettings.sonarAuthToken);
    def sqHelper = new SonarQubeHelper();
    PipelineSettings.SonarQubeSettings.initializeQualityMetrics();
    
    if (proxiesOn)
        sqService.enableProxies(PipelineSettings.ProxySettings.proxyHost, PipelineSettings.ProxySettings.proxyPort);

    // TODO - grab scan results and decorate based on branch regex (PR or not)

    // grab and parse quality scan results
    def response = sqService.getComponentMetrics(PipelineSettings.SonarQubeSettings.metricsList, PipelineSettings.SonarQubeSettings.projectKey);
    sqHelper.parseScanResults(PipelineSettings.SonarQubeSettings.metricsList, response);

    // log results
    println('Quality Scan Results: ' + PipelineSettings.SonarQubeSettings.metricsMap);
}