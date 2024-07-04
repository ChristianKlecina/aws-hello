package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import com.syndicate.deployment.annotations.resources.*;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LambdaHandler(lambdaName = "sqs_handler",
    roleName = "sqs_handler-role",
    isPublishVersion = false,
       runtime =  DeploymentRuntime.JAVA11,
    logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@SqsTriggerEventSource(
       batchSize = 10,
       targetQueue = "async_queue"
)
@DependsOn(
       name = "async_queue",
       resourceType = ResourceType.SQS_QUEUE
)
public class SqsHandler implements RequestHandler<SQSEvent, String> {

	private static final Logger logger = LoggerFactory.getLogger(SqsHandler.class);

	@Override
	public String handleRequest(SQSEvent event, Context context) {
		for (SQSEvent.SQSMessage message : event.getRecords()) {
			logger.info("Message Body: {}", message.getBody());
		}
		return "Messages processed successfully";
	}
}
