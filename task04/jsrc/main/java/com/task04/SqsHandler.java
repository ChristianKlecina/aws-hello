package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.*;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@LambdaHandler(lambdaName = "sqs_handler",
        roleName = "sqs_handler-role",
        isPublishVersion = false,
        timeout = 10
 
)
@SqsTriggerEventSource(
        targetQueue = "async_queue",
        batchSize = 10
)
@DependsOn(
        name = "async_queue",
        resourceType = ResourceType.SQS_QUEUE
)
public class SqsHandler implements RequestHandler<SQSEvent, String> {
 
    private static final Logger logger = LogManager.getLogger(SqsHandler.class);
 
    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger lambdaLogger = context.getLogger();
 
        for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            lambdaLogger.log(message.getBody());
            logger.info(message.getBody());
        }
        return "Success";
    }
}
