/*
 * Licensed to Qualys, Inc. (QUALYS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * QUALYS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.qualys.feign.jaxrs
import feign.Client
import feign.Feign
import feign.Request
import feign.Response
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import spock.lang.Specification
/**
 * Created by sskrla on 10/12/15.
 */
class BeanParamTest extends Specification {
    Request sent
    def client = Feign.builder()
            .encoder(new BeanParamEncoder(new JacksonEncoder()))
            .decoder(new JacksonDecoder())
            .invocationHandlerFactory(new BeanParamInvocationHandlerFactory())
            .contract(new EncoderJAXRS3Contract())
            .client(new Client() {
                @Override
                Response execute(Request request, Request.Options options) throws IOException {
                    sent = request
                    Response.builder().request(request).status(200).reason("OK").headers([:]).body(new byte[0]).build()
                }
            })
            .target(QueryResource, "http://localhost")

    def "query params"() {
        when:
        client.withParam(new QueryResource.QueryParamBean(param1: "one", param2: "two", param3: "three"))

        then:
        sent.url() == "http://localhost/?one=one&two=two&three=three"
    }

    def "query params by overridden setters"() {
        when:
        client.withExtendParam(new QueryResource.ExtendedSetterQueryParamBean(param1: "one", param2: "two", param3: "three"))

        then:
        sent.url() == "http://localhost/?one=one&two=two&three=three"
    }

    def "last null query param not sent"() {
        when:
        client.withParam(new QueryResource.QueryParamBean(param1: "one"))

        then:
        sent.url() == "http://localhost/?one=one"
    }

    def "middle null query param not sent"() {
        when:
        client.withParam(new QueryResource.QueryParamBean(param1: "one", param3: "three"))

        then:
        sent.url() == "http://localhost/?one=one&three=three"
    }

    def "first null query param not sent"() {
        when:
        client.withParam(new QueryResource.QueryParamBean(param3: "three"))

        then:
        sent.url() == "http://localhost/?three=three"
    }

    def "header param"() {
        when:
        client.withHeader(new QueryResource.HeaderBeanParam(testParam1: "ing", testParam2: "ing2"))

        then:
        sent.url() == "http://localhost/headers"
        sent.headers().get("test1")[0] == "ing"
        sent.headers().get("test2")[0] == "ing2"
    }

    def "map query param"() {
        when:
        client.mapQueryParam(Map.of("testParam1", "ing", "testParam2", "ing2"))

        then:
        sent.url() == "http://localhost/mapQueryParam?map=%7B%0D%0A%20%20%22testParam2%22%20%3A%20%22ing2%22%2C%0D%0A%20%20%22testParam1%22%20%3A%20%22ing%22%0D%0A%7D"
    }

    def "first null header param not sent"() {
        when:
        client.withHeader(new QueryResource.HeaderBeanParam(testParam2: "ing2"))

        then:
        sent.url() == "http://localhost/headers"
        sent.headers().get("test1") == null
        sent.headers().get("test2")[0] == "ing2"
    }

    def "path param"() {
        when:
        client.withPath(new QueryResource.PathBeanParam(id1: 42, id2: 123))

        then:
        sent.url() == "http://localhost/42/123"
    }

    def "mixed param"() {
        when:
        client.withMixed(5, "one", new QueryResource.MixedBeanParam(id: 10, param: "two", header: "headerTwo"), "headerOne")

        then:
        sent.url() == "http://localhost/path1/5/path2/10?param1=one&param2=two"
        sent.headers().get("header1")[0] == ("headerOne")
        sent.headers().get("header2")[0] == ("headerTwo")
    }

    def "path param string"() {
        when:
        client.withPathString("test")

        then:
        sent.url() == "http://localhost/test"
        sent.body() == null
    }

    def "mixed param with special characters"() {
        when:
        client.withMixed(5, "o{n}e", new QueryResource.MixedBeanParam(id: 10, param: "t{w}o", header: "headerTwo"), "heade{rOne}")

        then:
        sent.url() == "http://localhost/path1/5/path2/10?param1=o%7Bn%7De&param2=t%7Bw%7Do"
        sent.headers().get("header1")[0] == ("heade{rOne}")
        sent.headers().get("header2")[0] == ("headerTwo") /// headers in BeanParam doesn't support values with curly braces
    }

    def "mixed param only path"() {
        when:
        client.withMixed(5, null, new QueryResource.MixedBeanParam(id: 10), null)

        then:
        sent.url() == "http://localhost/path1/5/path2/10"
    }
}
