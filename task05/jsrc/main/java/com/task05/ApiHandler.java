package com.task05;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import com.amazonaws.services.dynamodbv2.document.Item;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;

import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.*;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = true
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
@DependsOn(name = "Events", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "table", value = "${target_table}")})
public class ApiHandler implements RequestHandler<Object, APIGatewayV2HTTPResponse> {

    //    private final String TABLE_NAME = "Events";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(Object event, Context context) {
        LambdaLogger lambdaLogger = context.getLogger();
        try {
            lambdaLogger.log("Lambda logger EVENT: " + objectMapper.writeValueAsString(event));
            lambdaLogger.log("EVENT TYPE: " + event.getClass());
            Map<String, Object> requestBody = objectMapper.readValue(objectMapper.writeValueAsString(event), LinkedHashMap.class);

            String id = UUID.randomUUID().toString();
            int principalId = (Integer) requestBody.get("principalId");
            Map<String, Object> content = (Map<String, Object>) requestBody.get("content");

            lambdaLogger.log(content.toString());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            String createdAt = OffsetDateTime.now(ZoneOffset.UTC).format(formatter);
            lambdaLogger.log("Time: " + createdAt);

            // DYNAMODB

            Map<String, AttributeValue> itemValues = getAttributesMap(id, principalId, content, createdAt);

            Item item = new Item();
            item.withString("id", id);
            item.withInt("principalId", principalId);
            item.withString("createdAt", createdAt);
            item.withMap("body", content);

            saveToDynamoDb(itemValues, item);


            return createSuccessResponse(item);
        } catch (Exception e) {
            lambdaLogger.log("Error: " + e.getMessage());
            e.printStackTrace();
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withBody(e.getMessage())
                    .build();
        }
    }

    private void saveToDynamoDb(Map<String, AttributeValue> itemValues, Item item) throws JsonProcessingException {
        final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
                .withRegion(System.getenv("region"))
                .build();

        try {
            ddb.putItem(System.getenv("table"), itemValues);
        } catch (ResourceNotFoundException e) {
            System.err.format("Error: The table \"%s\" can't be found.\n", System.getenv("table"));
            System.err.println(objectMapper.writeValueAsString(itemValues));
            System.exit(1);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, AttributeValue> getAttributesMap(String id,
                                                                int principalId,
                                                                Map<String, Object> content,
                                                                String createdAt) {
        Map<String, AttributeValue> itemValues = new HashMap<>();

        AttributeValue principalIdAttribute = new AttributeValue();
        principalIdAttribute.setN(String.valueOf(principalId));

        AttributeValue contentAttribute = new AttributeValue();
        Map<String, AttributeValue> contentAttributesMap = new HashMap<>();
        for (Map.Entry entry : content.entrySet()) {
            contentAttributesMap.put(String.valueOf(entry.getKey()), new AttributeValue((String) entry.getValue()));
        }
        contentAttribute.setM(contentAttributesMap);

        itemValues.put("id", new AttributeValue(id));
        itemValues.put("principalId", principalIdAttribute);
        itemValues.put("createdAt", new AttributeValue(createdAt));
        itemValues.put("body", contentAttribute);
        return itemValues;
    }

    private APIGatewayV2HTTPResponse createSuccessResponse(Item item) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("statusCode", 201);
        responseBody.put("event", item.asMap());

        String body;
        try {
            body = objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            body = null;
        }

        APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setStatusCode(201);
        response.setBody(body);
        return response;
    }

}