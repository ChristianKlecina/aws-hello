package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;



import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "api_handler",
		roleName = "api_handler-role",
		isPublishVersion = false,
		layers = {"sdk-layer"}
)
@LambdaLayer(
		layerName = "sdk-layer",
		libraries = {"lib/weather-forecast-1.0-SNAPSHOT.jar"},
		runtime = DeploymentRuntime.JAVA11,
		architectures = {Architecture.ARM64},
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<Object, String> {

	public String handleRequest(Object request, Context context) {

		WeatherForecast weatherForecast = new WeatherForecast();
        try {
            String weather = weatherForecast.getWeather();
			return weather;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


//        System.out.println("Hello from lambda");
//		Map<String, Object> resultMap = new HashMap<String, Object>();
//		resultMap.put("statusCode", 200);
//		resultMap.put("body", "Hello from Lambda");
		//return "resultMap";
//		Map<String, Object> resultMap = new HashMap<>();
//		try {
//			WeatherService weatherService = new WeatherService();
//			String forecastString = weatherService.getWeatherForecast();
//			System.err.println(forecastString);
//			return forecastString.replaceAll("\\\"", "\"");
//
//		} catch (IOException | InterruptedException e) {
//			resultMap.put("statusCode", 200);
//			resultMap.put("body", e.getMessage());
//		}
//		return "";
	}
}
