package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

import java.net.URI;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

@Testcontainers
public class KinesisTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisTest.class);
    private static final String STREAM_NAME = "events";
    private static final int SHARD_COUNT = 5;
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:0.14.1");

    @Container
    private final LocalStackContainer container = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(KINESIS);

    private KinesisClient kinesisClient;

    @BeforeEach
    public void setup() throws Exception {
        kinesisClient = createKinesisClient();
        createStream();
        waitForStream();
    }

    @Test
    public void writeToKinesisStream() throws Exception {
        writeRecordToStream("{\"foo\":\"bar\"}");
    }

    private KinesisClient createKinesisClient() {
        LOGGER.info("Creating Kinesis client...");
        URI endpoint = container.getEndpointOverride(KINESIS);
        LOGGER.info("Endpoint: " + endpoint);
        String region = container.getRegion();
        LOGGER.info("Region: " + region);

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        container.getAccessKey(), container.getSecretKey()
                )
        );
        return KinesisClient.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .build();
    }

    private void createStream() {
        LOGGER.info("Creating Kinesis stream...");
        kinesisClient.createStream(builder -> builder
                .streamName(STREAM_NAME)
                .shardCount(SHARD_COUNT)
                .build());
    }

    private void waitForStream() throws InterruptedException {
        while (!isStreamActive()) {
            LOGGER.info("Waiting for Kinesis stream to become active...");
            Thread.sleep(100);
        }
        LOGGER.info("Kinesis stream is active.");
    }

    private boolean isStreamActive() {
        DescribeStreamResponse response = kinesisClient.describeStream(builder -> builder
                .streamName(STREAM_NAME)
                .build());
        return response.streamDescription().streamStatus() == StreamStatus.ACTIVE;
    }

    private PutRecordResponse writeRecordToStream(String data) {
        LOGGER.info("Writing data to Kinesis...");

        SdkBytes payload = convertToPayload(data);
        String partitionKey = String.valueOf(System.nanoTime());

        PutRecordResponse response = kinesisClient.putRecord(builder -> builder
                .streamName(STREAM_NAME)
                .partitionKey(partitionKey)
                .data(payload)
                .build());

        LOGGER.info("Data written to Kinesis: " + response);
        return response;
    }

    private SdkBytes convertToPayload(String data) {
        return SdkBytes.fromUtf8String(data);
    }

}
