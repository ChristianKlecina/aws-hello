package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.syndicate.deployment.annotations.events.S3EventSource;
import com.syndicate.deployment.annotations.events.S3Events;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@LambdaHandler(lambdaName = "uuid_generator",
	roleName = "uuid_generator-role",
	isPublishVersion = false,
	runtime = DeploymentRuntime.JAVA11,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
//@S3EventSource(targetBucket = "uuid-storage", events = "s3:*")

public class UuidGenerator implements RequestHandler<Object, String> {

	private static final String BUCKET_NAME = "uuid-storage";
	private final AmazonS3 amazonS3Client = AmazonS3ClientBuilder.defaultClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public String handleRequest(Object request, Context context) {

		List<String> uuids = generateUUIDs(10);

		String fileName = getCurrentIsoTime();
		String fileContent = createFileContent(uuids);
		uploadToS3(BUCKET_NAME, fileName, fileContent);
		return "Success";

//		System.out.println("Hello from lambda");
//		Map<String, Object> resultMap = new HashMap<String, Object>();
//		resultMap.put("statusCode", 200);
//		resultMap.put("body", "Hello from Lambda");

	}

	public List<String> generateUUIDs(int count){
		List<String> uuids = new ArrayList<>();
		for(int i = 0; i< count; i++){
			uuids.add(UUID.randomUUID().toString());
		}
		return uuids;
	}

	private void uploadToS3(String bucketName, String fileName, String content) {
		amazonS3Client.putObject(bucketName, fileName, content);
	}

	private String getCurrentIsoTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date());
	}

	private String createFileContent(List<String> uuids) {
		Map<String, Object> data = new HashMap<>();
		data.put("ids", uuids);
		try {
			return objectMapper.writeValueAsString(data);
		} catch (IOException e) {
			throw new RuntimeException("Error creating JSON content", e);
		}
	}
}
