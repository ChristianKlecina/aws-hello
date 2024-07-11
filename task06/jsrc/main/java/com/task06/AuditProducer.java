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
import com.syndicate.deployment.model.DeploymentRuntime;
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
		isPublishVersion = false,
		runtime = DeploymentRuntime.JAVA11
)
@DependsOn(name = "Configuration", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "table", value = "${target_table}")})
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 10)
public class AuditProducer implements RequestHandler<DynamodbEvent, Map<String,Object>> {

	public Map<String,Object> handleRequest(DynamodbEvent request, Context context) {

		LambdaLogger lambdaLogger = context.getLogger();

		//List<DynamodbEvent.DynamodbStreamRecord> dynamodbStreamRecords = request.getRecords();

		for(DynamodbEvent.DynamodbStreamRecord record : request.getRecords()){
			lambdaLogger.log(record.toString());
			String key = record.getDynamodb().getNewImage().get("key").getS();
			String newValue = record.getDynamodb().getNewImage().get("value").getN();

			if("INSERT".equals(record.getEventName())){
				Item auditItem = new Item()
						.with("id", UUID.randomUUID().toString())
						.with("itemKey", key)
						.with("modificationTime", Instant.now().toString())
						.with("newValue", Map.of("key", key, "value", newValue));
						dynamoDbSave(ItemUtils.toAttributeValues(auditItem));
			}
			if("MODIFY".equals(record.getEventName())) {
				Item auditItem = new Item()
						.with("id", UUID.randomUUID().toString())
						.with("itemKey", key)
						.with("modificationTime", Instant.now().toString())
						.with("updatedAttribute", "value")
						.with("oldValue", Integer.valueOf(record.getDynamodb().getOldImage().get("value").getN()))
						.with("newValue", newValue);
						dynamoDbSave(ItemUtils.toAttributeValues(auditItem));
			}

		}

		return null;
	}

	private void dynamoDbSave(Map<String, AttributeValue> items){
		final AmazonDynamoDB database = AmazonDynamoDBClientBuilder.standard().withRegion(System.getenv("region")).build();
		database.putItem(System.getenv("table"), items);
	}

}
