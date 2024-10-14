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

package com.qualys.feign.jaxrs;

import jakarta.ws.rs.*;

import java.util.Map;

/**
 * Created by sskrla on 10/12/15.
 */
public interface QueryResource {
    @GET
    String withParam(@BeanParam QueryParamBean bean);

    @GET
    String withExtendParam(@BeanParam ExtendedSetterQueryParamBean bean);

    @GET
    @Path("headers")
    String withHeader(@BeanParam HeaderBeanParam bean);

    @GET
    @Path("{id1}/{id2}")
    String withPath(@BeanParam PathBeanParam path);

    @GET
    @Path("{id}")
    String withPathString(@PathParam("id") String id);

    @GET
    @Path("/path1")
    String testQueryString(@QueryParam("param1") String param);

    @GET
    @Path("path1/{id1}/path2/{id2}")
    String withMixed(@PathParam("id1") int id, @QueryParam("param1") String param, @QueryParam("param3") String param3,
                     @BeanParam MixedBeanParam bean, @HeaderParam("header1") String header);

    @GET
    @Path("/mapQueryParam")
    Map<String, String> mapQueryParam(@QueryParam("map") Map<String, String> map);

    @POST
    void postModel(PostModelParam model);

    class QueryParamBean {
        @QueryParam("one")
        String param1;
        @QueryParam("two")
        String param2;
        @QueryParam("three")
        String param3;

        public String getParam1() {
            return param1;
        }

        public void setParam1(String param1) {
            this.param1 = param1;
        }

        public String getParam2() {
            return param2;
        }

        public void setParam2(String param2) {
            this.param2 = param2;
        }

        public String getParam3() {
            return param3;
        }

        public void setParam3(String param3) {
            this.param3 = param3;
        }
    }

    class ParamBeanForExtend {
        String param1;
        String param2;
        String param3;

        public String getParam1() {
            return param1;
        }

        public void setParam1(String param1) {
            this.param1 = param1;
        }

        public String getParam2() {
            return param2;
        }

        public void setParam2(String param2) {
            this.param2 = param2;
        }

        public String getParam3() {
            return param3;
        }

        public void setParam3(String param3) {
            this.param3 = param3;
        }
    }

    class ExtendedSetterQueryParamBean extends ParamBeanForExtend {
        @QueryParam("one")
        public void setParam1(String param1) {
            this.param1 = param1;
        }

        @QueryParam("two")
        public void setParam2(String param2) {
            this.param2 = param2;
        }

        @QueryParam("three")
        public void setParam3(String param3) {
            this.param3 = param3;
        }
    }

    class HeaderBeanParam {
        @HeaderParam("test1")
        String testParam1;

        @HeaderParam("test2")
        String testParam2;

        public String getTestParam1() {
            return testParam1;
        }

        public void setTestParam1(String testParam1) {
            this.testParam1 = testParam1;
        }

        public String getTestParam2() {
            return testParam2;
        }

        public void setTestParam2(String testParam2) {
            this.testParam2 = testParam2;
        }
    }

    class PathBeanParam {
        @PathParam("id1")
        int id1;

        @PathParam("id2")
        int id2;

        public int getId1() {
            return id1;
        }

        public void setId1(int id1) {
            this.id1 = id1;
        }

        public int getId2() {
            return id2;
        }

        public void setId2(int id2) {
            this.id2 = id2;
        }
    }

    class MixedBeanParam {
        @PathParam("id2")
        int id;
        @QueryParam("param2")
        String param;
        @HeaderParam("header2")
        String header;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }
    }

    class PostModelParam {
        private Long id;
        private String name;

        public PostModelParam() {
        }

        public PostModelParam(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
