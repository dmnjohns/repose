/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.headertranslation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 3/21/16.
 *  Header translation with quality test
 */
class HeaderTranslationWQualityTest extends ReposeValveTest {
    def static Map params = [:]
    def static originEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params);
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/wquality", params);
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when translation header quality in config should override quality from header" () {

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["x-pp-user"    : "a",
                          "x-tenant-name": "b",
                          "x-roles"      : "c;q=0.3"]
        )

        def sentRequest = mc.getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("x-rax-username")
        sentRequest.request.getHeaders().contains("x-rax-tenants")
        sentRequest.request.getHeaders().contains("x-rax-roles")
        sentRequest.request.getHeaders().getFirstValue("x-rax-roles") == "c;q=0.2"
        sentRequest.request.getHeaders().contains("x-pp-user")
        sentRequest.request.getHeaders().contains("x-tenant-name")
        sentRequest.request.getHeaders().contains("x-roles")
        sentRequest.request.getHeaders().getFirstValue("x-roles") == "c;q=0.3"
    }

    def "when translation header with different quality header" () {

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["x-pp-user"    : "a",
                          "x-tenant-name": "b",
                          "x-roles"      : "test",
                          "x-rax-roles"  : "test;q=0.5"]
        )

        def sentRequest = mc.getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("x-rax-username")
        sentRequest.request.getHeaders().contains("x-rax-tenants")
        sentRequest.request.getHeaders().contains("x-rax-roles")
        sentRequest.request.getHeaders().findAll("x-rax-roles").contains("test;q=0.2")
        sentRequest.request.getHeaders().findAll("x-rax-roles").contains("test;q=0.5")
        sentRequest.request.getHeaders().contains("x-pp-user")
        sentRequest.request.getHeaders().contains("x-tenant-name")
        sentRequest.request.getHeaders().contains("x-roles")
        sentRequest.request.getHeaders().getFirstValue("x-roles") == "test"
    }

    @Unroll("Request with: #method Headers: #reqHeaders")
    def "when translating request headers with quality"() {

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: reqHeaders)
        def sentRequest = mc.getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("x-rax-username")
        sentRequest.request.getHeaders().getFirstValue("x-rax-username") == "a;q=0.5"
        sentRequest.request.getHeaders().contains("x-rax-tenants")
        sentRequest.request.getHeaders().getFirstValue("x-rax-tenants") == "b"
        sentRequest.request.getHeaders().contains("x-rax-roles")
        sentRequest.request.getHeaders().getFirstValue("x-rax-roles") == "c;q=0.2"
        sentRequest.request.getHeaders().contains("x-pp-user")
        sentRequest.request.getHeaders().contains("x-tenant-name")
        sentRequest.request.getHeaders().contains("x-roles")

        where:
        method | reqHeaders
        "POST" | ["x-pp-user": "a", "x-tenant-name": "b", "x-roles": "c"]
        "GET"  | ["x-pp-user": "a", "x-tenant-name": "b", "x-roles": "c"]
    }
}
