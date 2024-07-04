package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.*;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@LambdaHandler(lambdaName = "sns_handler",
        roleName = "sns_handler-role",
        isPublishVersion = false,
        timeout = 10
 
)
@SnsEventSource(
        targetTopic = "lambda_topic",
        regionScope = RegionScope.DEFAULT
)
@DependsOn(
        name = "lambda_topic",
        resourceType = ResourceType.SNS_TOPIC
)
public class SnsHandler implements RequestHandler<SNSEvent, String> {
 
    private static final Logger logger = LogManager.getLogger(SqsHandler.class);
 
    @Override
    public String handleRequest(SNSEvent snsEvent, Context context) {
        LambdaLogger lambdaLogger = context.getLogger();
 
        for (SNSEvent.SNSRecord record : snsEvent.getRecords()) {
            SNSEvent.SNS sns = record.getSNS();
            String message = sns.getMessage();
            lambdaLogger.log(message);
            logger.info(message);
        }
        return "Success";
    }
}
