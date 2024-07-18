package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.*;

@LambdaHandler(lambdaName = "api_handler",
		roleName = "api_handler-role",
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
		runtime = DeploymentRuntime.JAVA11
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
@DependsOn(name = "Tables", resourceType = ResourceType.DYNAMODB_TABLE)
@DependsOn(name = "Reservations", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "tablesTable", value = "${tables_table}"),
		@EnvironmentVariable(key = "reservationsTable", value = "${reservations_table}"),
		@EnvironmentVariable(key = "bookingUserPool", value = "${booking_userpool}")
})
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private final CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
			.region(Region.of(System.getenv("region")))
			.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
			.build();


	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		ObjectMapper objectMapper = new ObjectMapper();
		String httpMethod = event.getRequestContext().getHttp().getMethod();
		String path = event.getRawPath();

        Map<String, Object> pathParameters = null;
        try {
            pathParameters = objectMapper.readValue(objectMapper.writeValueAsString(event.getPathParameters()), LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if ("GET".equalsIgnoreCase(httpMethod) && pathParameters != null) {
			if (pathParameters.containsKey("tableId")) {
                try {
                    return getTableById(objectMapper.writeValueAsString(event.getPathParameters().get("tableId")));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
		}

		if(path.equalsIgnoreCase("/signup") && httpMethod.equalsIgnoreCase("POST")){
			signUp(event);
		}

		if(path.equalsIgnoreCase("/signin") && httpMethod.equalsIgnoreCase("POST")){
			signIn(event);
		}

		if(path.equalsIgnoreCase("/tables") && httpMethod.equalsIgnoreCase("GET")){
			getTables();
		}

		if(path.equalsIgnoreCase("/tables") && httpMethod.equalsIgnoreCase("POST")){
			postTable(event);
		}

		if(path.equalsIgnoreCase("/reservations") && httpMethod.equalsIgnoreCase("GET")){
			getReservations();
		}

		if(path.equalsIgnoreCase("/reservations") && httpMethod.equalsIgnoreCase("POST")){
			postReservation(event);
		}

		return new APIGatewayV2HTTPResponse();
	}

	public APIGatewayV2HTTPResponse signUp(APIGatewayV2HTTPEvent event){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();

		try{
			Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
			String email = String.valueOf(body.get("email"));
			String password = String.valueOf(body.get("password"));

			Validator.isValidEmail(email);
			Validator.isValidPassword(password);

			String userPoolId = String.valueOf(getUserPoolIdByName(System.getenv("bookingUserPool")));

			AdminCreateUserRequest adminCreateUserRequest = AdminCreateUserRequest
					.builder()
					.userPoolId(userPoolId)
					.username(email)
					.userAttributes(AttributeType.builder()
									.name("email")
									.value(email)
									.build())
					.messageAction(MessageActionType.SUPPRESS)
					.build();
			AdminSetUserPasswordRequest adminSetUserPassword = AdminSetUserPasswordRequest
					.builder()
					.password(password)
					.userPoolId(userPoolId)
					.username(email)
					.permanent(true)
					.build();

			cognitoClient.adminCreateUser(adminCreateUserRequest);
			cognitoClient.adminSetUserPassword(adminSetUserPassword);
			response.setStatusCode(200);
		} catch (Exception ex){
			response.setStatusCode(400);
		}
		return response;
	}


	public APIGatewayV2HTTPResponse signIn(APIGatewayV2HTTPEvent event){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();

		try{
			Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
			String email = String.valueOf(body.get("email"));
			String password = String.valueOf(body.get("password"));

			Validator.isValidEmail(email);
			Validator.isValidPassword(password);

			String userPoolId = String.valueOf(getUserPoolIdByName(System.getenv("bookingUserPool")));
			String clientId = String.valueOf(getClientIdByUserPoolName(System.getenv("bookingUserPool")));

			Map<String, String> authParams = new HashMap<>();
			authParams.put("USERNAME", email);
			authParams.put("PASSWORD", password);


			AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
					.userPoolId(userPoolId)
					.clientId(clientId)
					.authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
					.authParameters(authParams)
					.build();

			AdminInitiateAuthResponse result = cognitoClient.adminInitiateAuth(authRequest);

			String accessToken = result.authenticationResult().idToken();

			Map<String, Object> jsonResponse = new HashMap<>();
			jsonResponse.put("accessToken", accessToken);
			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));
		} catch (Exception ex){
			response.setStatusCode(400);
		}
		return response;
	}

	private APIGatewayV2HTTPResponse getTables(){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();
		try{
			AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
					.withRegion(System.getenv("region"))
					.build();

			ScanRequest scanRequest = new ScanRequest().withTableName(System.getenv("tablesTable"));
			ScanResult scanResult = ddb.scan(scanRequest);

			List<Map<String, Object>> tables = new ArrayList<>();
			for (Map<String, AttributeValue> item : scanResult.getItems()) {
				Map<String, Object> table = new LinkedHashMap<>();
				table.put("id", Integer.parseInt(item.get("id").getS()));
				table.put("number", Integer.parseInt(item.get("number").getN()));
				table.put("places", Integer.parseInt(item.get("places").getN()));
				table.put("isVip", Boolean.parseBoolean(item.get("isVip").getBOOL().toString()));
				table.put("minOrder", Integer.parseInt(item.get("minOrder").getN()));
				tables.add(table);
			}
			tables.sort(Comparator.comparing(o -> (Integer) o.get("id")));
			Map<String, Object> jsonResponse = new HashMap<>();
			jsonResponse.put("tables", tables);

			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));
		} catch (Exception e){
			response.setStatusCode(400);
		}
		return response;
	}


	private APIGatewayV2HTTPResponse getTableById(String tableId){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();
		try{
			AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
					.withRegion(System.getenv("region"))
					.build();

			ScanRequest scanRequest = new ScanRequest().withTableName(System.getenv("tablesTable"));
			ScanResult scanResult = ddb.scan(scanRequest);
			Map<String, AttributeValue> table = new HashMap<>();
			for (Map<String, AttributeValue> item : scanResult.getItems()) {
				int existingId = Integer.parseInt(item.get("id").getS().trim().replaceAll("\"", ""));
				int requiredId = Integer.parseInt(tableId.trim().replaceAll("\"", ""));
				if (existingId == requiredId) {
					table = item;
				}
			}
			Map<String, Object> jsonResponse = ItemUtils.toSimpleMapValue(table);
			jsonResponse.replace("id", Integer.parseInt((String) jsonResponse.get("id")));

			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));
		} catch (Exception e){
			response.setStatusCode(400);
		}
		return response;
	}

	private APIGatewayV2HTTPResponse getReservations(){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();
		try{
			AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
					.withRegion(System.getenv("region"))
					.build();

			ScanRequest scanRequest = new ScanRequest().withTableName(System.getenv("reservationsTable"));
			ScanResult scanResult = ddb.scan(scanRequest);

			List<Map<String, Object>> jsonResponse = new ArrayList<>();
			for (Map<String, AttributeValue> item : scanResult.getItems()) {
				item.remove("id");
				Map<String, Object> reservation = new LinkedHashMap<>();
				reservation.put("tableNumber", Integer.parseInt(item.get("tableNumber").getS()));
				reservation.put("clientName", item.get("clientName").getS());
				reservation.put("phoneNumber", item.get("phoneNumber").getS());
				reservation.put("date", item.get("date").getS());
				reservation.put("slotTimeStart", item.get("slotTimeStart").getS());
				reservation.put("slotTimeEnd", item.get("slotTimeEnd").getS());

				jsonResponse.add(reservation);
			}

			Map<String, Object> json = new HashMap<>();
			json.put("reservations", jsonResponse);

			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));

		} catch (Exception ex){
			response.setStatusCode(400);
		}
		return response;
	}

	private APIGatewayV2HTTPResponse postTable(APIGatewayV2HTTPEvent event){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();
		AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
				.withRegion(System.getenv("region"))
				.build();
		try{

			Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
			String id = String.valueOf(body.get("id"));
			int number = (Integer) body.get("number");
			int places = (Integer) body.get("places");
			boolean isVip = (Boolean) body.get("isVip");
			int minOrder = -1;
			if (body.containsKey("minOrder")) {
				minOrder = (Integer) body.get("minOrder");
			}

			Item item = new Item()
					.withString("id", id)
					.withInt("number", number)
					.withInt("places", places)
					.withBoolean("isVip", isVip);
			if (minOrder != -1) {
				item.withInt("minOrder", minOrder);
			}

			ddb.putItem(System.getenv("tablesTable"), ItemUtils.toAttributeValues(item));


			Map<String, Object> jsonResponse = new HashMap<>();
			jsonResponse.put("id", Integer.parseInt(id));

			response.setStatusCode(200);

			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));

		} catch (Exception ex){
			response.setStatusCode(400);
		}
		return response;
	}

	private APIGatewayV2HTTPResponse postReservation(APIGatewayV2HTTPEvent event){
		APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
		ObjectMapper objectMapper = new ObjectMapper();
		AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
				.withRegion(System.getenv("region"))
				.build();
		try{
			Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);

			String reservationId = UUID.randomUUID().toString();
			String tableNumber = String.valueOf(body.get("tableNumber"));
			String clientName = String.valueOf(body.get("clientName"));
			String phoneNumber = String.valueOf(body.get("phoneNumber"));
			String date = String.valueOf(body.get("date"));
			String slotTimeStart = String.valueOf(body.get("slotTimeStart"));
			String slotTimeEnd = String.valueOf(body.get("slotTimeEnd"));


			Item item = new Item()
					.withString("id", reservationId)
					.withString("tableNumber", tableNumber)
					.withString("clientName", clientName)
					.withString("phoneNumber", phoneNumber)
					.withString("date", date)
					.withString("slotTimeStart", slotTimeStart)
					.withString("slotTimeEnd", slotTimeEnd);


			ddb.putItem(System.getenv("reservationsTable"), ItemUtils.toAttributeValues(item));

			Map<String, Object> jsonResponse = new HashMap<>();
			jsonResponse.put("reservationId", reservationId);

			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));
		} catch (Exception ex){
			response.setStatusCode(400);
		}
		return response;
	}

	public Optional<String> getUserPoolIdByName(String userPoolName) {
		ListUserPoolsRequest listUserPoolsRequest = ListUserPoolsRequest.builder()
				.build();

		ListUserPoolsResponse listUserPoolsResponse = cognitoClient.listUserPools(listUserPoolsRequest);
		for (UserPoolDescriptionType userPool : listUserPoolsResponse.userPools()) {
			if (userPool.name().equals(userPoolName)) {
				return Optional.of(userPool.id());
			}
		}

		return Optional.empty();
	}

	public Optional<String> getClientIdByUserPoolName(String userPoolName) {
		String userPoolId = getUserPoolIdByName(userPoolName).get();

		ListUserPoolClientsRequest listUserPoolClientsRequest = ListUserPoolClientsRequest.builder()
				.userPoolId(userPoolId)
				.build();

		ListUserPoolClientsResponse listUserPoolClientsResponse = cognitoClient.listUserPoolClients(listUserPoolClientsRequest);
		for (UserPoolClientDescription client : listUserPoolClientsResponse.userPoolClients()) {
			return Optional.of(client.clientId());
		}

		return Optional.empty();
	}


}
