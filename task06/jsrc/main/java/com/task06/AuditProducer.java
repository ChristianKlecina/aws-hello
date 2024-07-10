package com.task06;



import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.amazonaws.services.dynamodbv2.document.Item;

import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "audit_producer",
		roleName = "audit_producer-role",
		isPublishVersion = false
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
@DependsOn(name = "Audit", resourceType = ResourceType.DYNAMODB_TABLE)
@DependsOn(name = "Configuration", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "table", value = "${target_table}")})
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 10)
public class AuditProducer implements RequestHandler<DynamodbEvent, Map<String,Object>> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public Map<String,Object> handleRequest(DynamodbEvent request, Context context) {

		LambdaLogger lambdaLogger = context.getLogger();

		List<DynamodbEvent.DynamodbStreamRecord> dynamodbStreamRecords = request.getRecords();
		String key;
		String newValue;
		String oldValue;
		for(DynamodbEvent.DynamodbStreamRecord record: dynamodbStreamRecords){
			lambdaLogger.log(record.toString());
			key = record.getDynamodb().getOldImage().get("key").getS();
			newValue = record.getDynamodb().getNewImage().get("value").getN();
			oldValue = record.getDynamodb().getOldImage().get("value").getN();
			Item auditItem;
			if(record.getDynamodb().getOldImage().isEmpty()){
				auditItem = new Item()
						.withPrimaryKey("id", UUID.randomUUID().toString())
						.withString("itemKey", key)
						.withString("modificationTime", Instant.now().toString())
						.withMap("newValue", Map.of("key", key, "value", newValue));

			} else {
				auditItem = new Item()
						.withPrimaryKey("id", UUID.randomUUID().toString())
						.withString("itemKey", key)
						.withString("modificationTime", Instant.now().toString())
						.withMap("newValue", Map.of("key", key, "value", oldValue));
			}
			dynamoDbSave(ItemUtils.toAttributeValues(auditItem));
		}

		return null;
	}

	private void dynamoDbSave(Map<String, AttributeValue> items){
		final AmazonDynamoDB database = AmazonDynamoDBClientBuilder.standard().withRegion(System.getenv("region")).build();
		database.putItem(System.getenv("table"), items);
	}

}
