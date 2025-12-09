/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.a2a.server.apps.common;

import io.a2a.json.JsonProcessingException;
import io.a2a.json.JsonUtil;
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
