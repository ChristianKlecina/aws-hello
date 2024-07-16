package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "processor",
	roleName = "processor-role",
	isPublishVersion = true,
	tracingMode = TracingMode.Active
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
@DependsOn(name = "Weather", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "table", value = "${target_table}")})
public class Processor implements RequestHandler<Object, Map<String, Object>> {

	public Map<String, Object> handleRequest(Object request, Context context) {
		WeatherAPIReader weatherAPIReader = new WeatherAPIReader();
		Map<String, Object> weatherResponse = new HashMap<String, Object>();
		String id = UUID.randomUUID().toString();

		Map<String, Object> hourly = (Map<String, Object>) weatherAPIReader.readWeatherAPI().get("hourly");
		hourly.remove("wind_speed_10m");
		hourly.remove("interval");
		hourly.remove("relative_humidity_2m");
		Map<String, Object> hourly_units = (Map<String, Object>) weatherAPIReader.readWeatherAPI().get("hourly_units");
		hourly_units.remove("wind_speed_10m");
		hourly_units.remove("interval");
		hourly_units.remove("relative_humidity_2m");

		weatherResponse.put("id", id);

		Map<String, Object> forecastObj = new HashMap<>();

		forecastObj.put("elevation", weatherAPIReader.readWeatherAPI().get("elevation"));
		forecastObj.put("generationtime_ms", weatherAPIReader.readWeatherAPI().get("generationtime_ms"));
		forecastObj.put("hourly", hourly);
		forecastObj.put("hourly_units", hourly_units);
		forecastObj.put("latitude", weatherAPIReader.readWeatherAPI().get("latitude"));
		forecastObj.put("longitude", weatherAPIReader.readWeatherAPI().get("longitude"));
		forecastObj.put("timezone", weatherAPIReader.readWeatherAPI().get("timezone"));
		forecastObj.put("timezone_abbreviation", weatherAPIReader.readWeatherAPI().get("timezone_abbreviation"));
		forecastObj.put("utc_offset_seconds", weatherAPIReader.readWeatherAPI().get("utc_offset_seconds"));







		weatherResponse.put("forecast", forecastObj);


		dynamoDbSave(ItemUtils.fromSimpleMap(weatherResponse));

		return weatherResponse;
	}

	private void dynamoDbSave(Map<String, AttributeValue> items){
		final AmazonDynamoDB database = AmazonDynamoDBClientBuilder.standard().withRegion(System.getenv("region")).build();
		database.putItem(System.getenv("table"), items);
	}
}
