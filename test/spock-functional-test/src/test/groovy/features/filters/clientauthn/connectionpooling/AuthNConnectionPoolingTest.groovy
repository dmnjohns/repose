package features.filters.clientauthn.connectionpooling

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.DeproxyEndpoint
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: izrik
 *
 */
class AuthNConnectionPoolingTest extends Specification {

    int reposePort
    int reposeStopPort
    int originServicePort
    int identityServicePort
    String urlBase

    IdentityServiceResponseSimulator identityService

    Deproxy deproxy
    DeproxyEndpoint originEndpoint
    DeproxyEndpoint identityEndpoint

    Properties properties
    def logFile
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    ReposeLogSearch reposeLogSearch

    def setup() {

        // get ports
        PortFinder pf = new PortFinder()

        reposePort = pf.getNextOpenPort()
        reposeStopPort = pf.getNextOpenPort()
        originServicePort = pf.getNextOpenPort()
        identityServicePort = pf.getNextOpenPort()

        identityService = new IdentityServiceResponseSimulator(identityServicePort, originServicePort)

        // start deproxy
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(originServicePort)
        identityEndpoint = deproxy.addEndpoint(identityServicePort,
                "identity", "localhost", identityService.handler)

        // configure and start repose
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())

        def targetHostname = properties.getProperty("target.hostname")
        urlBase = "http://${targetHostname}:${reposePort}"
        logFile = properties.getProperty("repose.log")

        def configDirectory = properties.getProperty("repose.config.directory")
        def configSamples = properties.getProperty("repose.config.samples")
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                urlBase,
                configDirectory,
                reposePort,
                reposeStopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);

        def params = [
                'reposePort': reposePort.toString(),
                'repose_port': reposePort.toString(),
                'targetPort': originServicePort.toString(),
                'target_port': originServicePort.toString(),
                'targetHostname': targetHostname.toString(),
                'target_hostname': targetHostname.toString(),
                'identityPort': identityServicePort.toString(),
                'identity_port': identityServicePort.toString()
        ]
        reposeConfigProvider.applyConfigsRuntime("common", params)
        reposeConfigProvider.applyConfigsRuntime("features/filters/clientauthn/connectionpooling", params)
        reposeConfigProvider.applyConfigsRuntime("features/filters/clientauthn/connectionpooling2", params)
        repose.start()
    }

    def "when a client makes requests, Repose should re-use the connection to the Identity service"() {

        setup: "craft an url to a resource that requires authentication"
        def url = "${urlBase}/servers/tenantid/resource"


        when: "making two authenticated requests to Repose"
        def mc1 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token1'])
        def mc2 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token2'])
        // collect all of the handlings that make it to the identity endpoint into one list
        def allOrphanedHandlings = mc1.orphanedHandlings + mc2.orphanedHandlings
        List<Handling> identityHandlings = allOrphanedHandlings.findAll { it.endpoint == identityEndpoint }
        def commons = allOrphanedHandlings.intersect(identityHandlings)
        def diff = allOrphanedHandlings.plus(identityHandlings)
        diff.removeAll(commons)


        then: "the connections for Repose's request to Identity should have the same id"

        mc1.orphanedHandlings.size() > 0
        mc2.orphanedHandlings.size() > 0
        identityHandlings.size() > 0
        // there should be no requests to auth with a different connection id
        diff.size() == 0
    }

    def cleanup() {

        if (repose && repose.isUp()) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
