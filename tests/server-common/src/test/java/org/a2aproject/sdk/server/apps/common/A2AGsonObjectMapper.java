/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.a2aproject.sdk.server.apps.common;

import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;


public class A2AGsonObjectMapper implements ObjectMapper {
    public static final A2AGsonObjectMapper INSTANCE = new A2AGsonObjectMapper();

    private A2AGsonObjectMapper() {
    }

     @Override
     public Object deserialize(ObjectMapperDeserializationContext context) {
         try {
             return JsonUtil.fromJson(context.getDataToDeserialize().asString(), context.getType());
         } catch (JsonProcessingException ex) {
             throw new RuntimeException(ex);
         }
    }

     @Override
    public Object serialize(ObjectMapperSerializationContext context) {
         try {
             return JsonUtil.toJson(context.getObjectToSerialize());
         } catch (JsonProcessingException ex) {
             
             throw new RuntimeException(ex);
         }
    }
}
