package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = false
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class HelloWorld implements RequestHandler<APIGatewayV2HTTPEvent, String> {

	public String handleRequest(APIGatewayV2HTTPEvent input, Context context) {

//		Map<String, Object> resultMap = new HashMap<>();
//
			String path = input.getRequestContext().getHttp().getPath();
			String method = input.getRequestContext().getHttp().getMethod();
//		if ("/hello".equals(path)) {
//			resultMap.put("statusCode", 200);
//			resultMap.put("message", "Hello from Lambda");
//		} else {
//			resultMap.put("statusCode", 400);
//			resultMap.put("message", "Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + method);
//		}
		if ("/hello".equals(path)) {
			return "{\"statusCode\": 200, \"message\": \"Hello from Lambda\"}";
		} else {
			return "{\"statusCode\": 400, \"message\": \"Bad request syntax or unsupported method. Request path: " + path + ". HTTP method: " + method + "\"}";
		}

	}


}
