package com.task11;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.LocalTime;
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
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> request = null;
        try {
            request = objectMapper.readValue(objectMapper.writeValueAsString(event), LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String path = (String) request.get("path");
        String method = (String) request.get("httpMethod");


        System.err.println("Path: " + path + " method: " + method);
        try {
            System.err.println("Request: " + objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


        if ("/signup".equals(path) && "POST".equalsIgnoreCase(method)) {
            return signUp(event);
        }

        if ("/signin".equals(path) && "POST".equalsIgnoreCase(method)) {
            return signIn(event);
        }

        if("/tables".equals(path) && "POST".equalsIgnoreCase(method)){
			return postTable(event);
		}

        if("/tables".equals(path) && "GET".equalsIgnoreCase(method)){
            return getTables();
        }

        Map<String, Object> pathParameters = null;
        try {
            pathParameters = objectMapper.readValue(objectMapper.writeValueAsString(event.getPathParameters()), LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if ("GET".equalsIgnoreCase(method) && pathParameters != null) {
            if (pathParameters.containsKey("tableId")) {
                try {
                    return getTableById(objectMapper.writeValueAsString(event.getPathParameters().get("tableId")));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if("/reservations".equals(path) && "POST".equalsIgnoreCase(method)){
            return postReservation(event);
        }

        if("/reservations".equals(path) && "GET".equalsIgnoreCase(method)){
            return getReservations();
        }

        return new APIGatewayProxyResponseEvent();
    }


    private APIGatewayProxyResponseEvent signUp(APIGatewayProxyRequestEvent event) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
            System.err.println("signUp was called");
            String email = String.valueOf(body.get("email"));
            String password = String.valueOf(body.get("password"));

            if (!EmailValidator.validateEmail(email)) {
                System.err.println("Email is invalid");
                throw new Exception("Email is invalid");
            }

            if (!PasswordValidator.validatePassword(password)) {
                System.err.println("Password is invalid");
                throw new Exception("Email is invalid");
            }

            String userPoolId = new CognitoHelper().getUserPoolIdByName(System.getenv("bookingUserPool"))
                    .orElseThrow(() -> new IllegalArgumentException("No such user pool"));

            AdminCreateUserRequest adminCreateUserRequest = AdminCreateUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .userAttributes(AttributeType.builder().name("email").value(email).build())
                    .messageAction(MessageActionType.SUPPRESS)
                    .build();
            System.err.println(adminCreateUserRequest.toString());
            AdminSetUserPasswordRequest adminSetUserPassword = AdminSetUserPasswordRequest
                    .builder()
                    .password(password)
                    .userPoolId(userPoolId)
                    .username(email)
                    .permanent(true)
                    .build();
            System.err.println(adminSetUserPassword.toString());

            cognitoClient.adminCreateUser(adminCreateUserRequest);
            cognitoClient.adminSetUserPassword(adminSetUserPassword);

            response.setStatusCode(200);

        } catch (Exception ex) {
            System.err.println(ex);
            response.setStatusCode(400);
            response.setBody(ex.toString());
        }
        return response;
    }

    private APIGatewayProxyResponseEvent signIn(APIGatewayProxyRequestEvent event) {
        System.err.println("signIn was called");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ObjectMapper objectMapper = new ObjectMapper();

        try {

            Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
            System.err.println("signUp was called");
            String email = String.valueOf(body.get("email"));
            String password = String.valueOf(body.get("password"));

            if (!EmailValidator.validateEmail(email)) {
                System.err.println("Email is invalid");
                throw new Exception("Email is invalid");
            }

            if (!PasswordValidator.validatePassword(password)) {
                System.err.println("Password is invalid");
                throw new Exception("Email is invalid");
            }

            String userPoolId = new CognitoHelper().getUserPoolIdByName(System.getenv("bookingUserPool"))
                    .orElseThrow(() -> new IllegalArgumentException("No such user pool"));

            String clientId = new CognitoHelper()
                    .getClientIdByUserPoolName(System.getenv("bookingUserPool"))
                    .orElseThrow(() -> new IllegalArgumentException("No such client id"));

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            System.err.println(authParams);
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();
            System.err.println(authRequest);

            AdminInitiateAuthResponse result = cognitoClient.adminInitiateAuth(authRequest);
            String accessToken = result.authenticationResult().idToken();
            System.err.println(accessToken);

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("accessToken", accessToken);

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(jsonResponse));
            System.err.println(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception ex) {
            System.err.println(ex);
            response.setStatusCode(400);
            response.setBody(ex.toString());
        }
        return response;
    }


    private APIGatewayProxyResponseEvent postTable(APIGatewayProxyRequestEvent event) {
        System.err.println("postTable was called");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ObjectMapper objectMapper = new ObjectMapper();
        AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
                .withRegion(System.getenv("region"))
                .build();
        try {


            Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
            System.err.println(body);
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
            System.err.println(item);
            ddb.putItem(System.getenv("tablesTable"), ItemUtils.toAttributeValues(item));


            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("id", Integer.parseInt(id));
            System.err.println(jsonResponse);
            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(jsonResponse));
        } catch (
                Exception ex) {
            System.err.println(ex);
            response.setStatusCode(400);
            response.setBody(ex.toString());
        }
        return response;
    }

    private APIGatewayProxyResponseEvent getTables() {
        System.err.println("getTables was called");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(System.getenv("region"))
                    .build();

            ScanRequest scanRequest = new ScanRequest().withTableName(System.getenv("tablesTable"));
            ScanResult scanResult = ddb.scan(scanRequest);
            System.err.println(scanResult);

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
            System.err.println(tables);

            tables.sort(Comparator.comparing(o -> (Integer) o.get("id")));
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("tables", tables);
            System.err.println(jsonResponse);
            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            response.setStatusCode(400);
        }
        return response;
    }


    private APIGatewayProxyResponseEvent getTableById(String tableId) {
        System.err.println("getTableById was called");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(System.getenv("region"))
                    .build();

            ScanRequest scanRequest = new ScanRequest().withTableName(System.getenv("tablesTable"));
            ScanResult scanResult = ddb.scan(scanRequest);
            System.err.println(scanResult);
            Map<String, AttributeValue> table = new HashMap<>();
            for (Map<String, AttributeValue> item : scanResult.getItems()) {
                int existingId = Integer.parseInt(item.get("id").getS().trim().replaceAll("\"", ""));
                int requiredId = Integer.parseInt(tableId.trim().replaceAll("\"", ""));
                if (existingId == requiredId) {
                    table = item;
                }
            }
            System.err.println(table);
            Map<String, Object> jsonResponse = ItemUtils.toSimpleMapValue(table);
            jsonResponse.replace("id", Integer.parseInt((String) jsonResponse.get("id")));

            System.err.println(jsonResponse);
            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(jsonResponse));
        } catch (Exception e) {
            response.setStatusCode(400);
        }
        return response;
    }

	private APIGatewayProxyResponseEvent postReservation(APIGatewayProxyRequestEvent event){
        System.err.println("postReservation was called");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		ObjectMapper objectMapper = new ObjectMapper();
		AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
				.withRegion(System.getenv("region"))
				.build();
		try{
			Map<String, Object> body = objectMapper.readValue(event.getBody(), Map.class);
            System.err.println(body);

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



            System.err.println(item);

            if (!tableExists(ddb,System.getenv("tablesTable"), tableNumber)) {
                response.setStatusCode(400);
                response.setBody("Table does not exist");
                System.err.println("Table does not exist");
                return response;
            }

            if (isOverlappingReservation(ddb,System.getenv("reservationsTable"), tableNumber, date, slotTimeStart, slotTimeEnd)) {
                response.setStatusCode(400);
                response.setBody("Reservation overlaps with an existing reservation");
                System.err.println("Reservation overlaps with an existing reservation");
                return response;
            }


			ddb.putItem(System.getenv("reservationsTable"), ItemUtils.toAttributeValues(item));

			Map<String, Object> jsonResponse = new HashMap<>();
			jsonResponse.put("reservationId", reservationId);
            System.err.println(jsonResponse);
			response.setStatusCode(200);
			response.setBody(objectMapper.writeValueAsString(jsonResponse));
		} catch (Exception ex){
			response.setStatusCode(400);
		}
		return response;
	}

    private APIGatewayProxyResponseEvent getReservations() {
        System.err.println("getReservations was called");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(System.getenv("region"))
                    .build();

            ScanRequest scanRequest = new ScanRequest().withTableName(System.getenv("reservationsTable"));
            ScanResult scanResult = ddb.scan(scanRequest);
            System.err.println(scanResult);

            List<Map<String, Object>> reservations = new ArrayList<>();
            for (Map<String, AttributeValue> item : scanResult.getItems()) {
                item.remove("id");
                Map<String, Object> reservation = new LinkedHashMap<>();
                reservation.put("tableNumber", Integer.parseInt(item.get("tableNumber").getS()));
                reservation.put("clientName", item.get("clientName").getS());
                reservation.put("phoneNumber", item.get("phoneNumber").getS());
                reservation.put("date", item.get("date").getS());
                reservation.put("slotTimeStart", item.get("slotTimeStart").getS());
                reservation.put("slotTimeEnd", item.get("slotTimeEnd").getS());

                reservations.add(reservation);
            }

            System.err.println(reservations);

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("reservations", reservations);
            System.err.println(jsonResponse);

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(jsonResponse));

        } catch (Exception ex) {
            response.setStatusCode(400);
        }
        return response;
    }

    public boolean tableExists(AmazonDynamoDB ddb, String tableName, String tableNumber) {
        ScanResult scanResult = ddb.scan(new ScanRequest().withTableName(tableName));

        for (Map<String, AttributeValue> item : scanResult.getItems()) {
            if (tableNumber.equals(item.get("number").getN())) {
                System.err.println("Table exists, number: " + tableNumber);
                return true;
            }
        }
        return false;
    }

    public boolean isOverlappingReservation(AmazonDynamoDB ddb,String tableName, String tableNumber, String date, String slotTimeStart, String slotTimeEnd) {
        ScanResult scanResult = ddb.scan(new ScanRequest().withTableName(tableName));
        for (Map<String, AttributeValue> item : scanResult.getItems()) {
            String existingTableNumber = item.get("tableNumber").getS();
            String existingDate = item.get("date").getS();

            if (tableNumber.equals(existingTableNumber) && date.equals(existingDate)) {
                String existingSlotTimeStart = item.get("slotTimeStart").getS();
                String existingSlotTimeEnd = item.get("slotTimeEnd").getS();

                return isTimeOverlap(slotTimeStart, slotTimeEnd, existingSlotTimeStart, existingSlotTimeEnd);
            }
        }

        return false;
    }

    private boolean isTimeOverlap(String slotTimeStart, String slotTimeEnd, String existingSlotTimeStart, String existingSlotTimeEnd) {

        LocalTime start = LocalTime.parse(slotTimeStart);
        LocalTime end = LocalTime.parse(slotTimeEnd);
        LocalTime existingStart = LocalTime.parse(existingSlotTimeStart);
        LocalTime existingEnd = LocalTime.parse(existingSlotTimeEnd);

        return (start.isBefore(existingEnd) && end.isAfter(existingStart));
    }
}