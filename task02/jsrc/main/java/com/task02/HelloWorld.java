package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class HelloWorld implements RequestHandler<Map<String, String> , Map<String, Object>> {




	public Map<String, Object> handleRequest(Map<String, String> requestData, Context context) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			// Main logic
			System.out.println("Hello from lambda");
			resultMap.put("statusCode", 200);
			resultMap.put("message", "Hello from Lambda");

			// Retrieve AWS function URL
			String functionArn = context.getInvokedFunctionArn();
			String functionUrl = "https://" + functionArn.split(":")[5] + ".execute-api." + context.getAwsRequestId().split(":")[3] + ".amazonaws.com";
			resultMap.put("functionUrl", functionUrl);
		} catch (Exception e) {
			// Handle error
			resultMap.put("statusCode", 400);
			resultMap.put("message", "Bad request syntax or unsupported method. Request path: "
					+ context.getFunctionName() + ". HTTP method: " + context.getInvokedFunctionArn());
		}
		return resultMap;
	}


}
