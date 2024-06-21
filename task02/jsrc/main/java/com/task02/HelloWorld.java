package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world-test",
	roleName = "hello_world-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class HelloWorld implements RequestHandler<Map<String, String> , Map<String, Object>> {




	public Map<String, Object> handleRequest(Map<String, String> input, Context context) {


		Map<String, Object> resultMap = new HashMap<>();
		String path = input.get("path");
		String method = input.get("httpMethod");

		if ("/hello".equals(path)) {
			resultMap.put("statusCode", 200);
			resultMap.put("message", "Hello from Lambda");
			System.out.println("Usao u 200");
		} else {
			System.out.println("Usao u 400");
			resultMap.put("statusCode", 400);
			resultMap.put("message", "Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + method);
		}

		return resultMap;
	}


}
