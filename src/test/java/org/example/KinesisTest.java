package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.net.URI;
import java.time.Duration;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

@Testcontainers
public class KinesisTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisTest.class);
    private static final String STREAM_NAME = "events";
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:0.14.1");

    private final MountableFile mountableFile = MountableFile
            .forHostPath("setup-localstack.sh", 0744);
    private final WaitStrategy waitStrategy = Wait.forLogMessage(".*KinesisStreamIsReady.*", 1)
            .withStartupTimeout(Duration.ofSeconds(20));

    @Container
    private final LocalStackContainer container = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(KINESIS)
            .withCopyFileToContainer(mountableFile, "/docker-entrypoint-initaws.d/setup-localstack.sh")
            .waitingFor(waitStrategy);

    private KinesisClient kinesisClient;

    @BeforeEach
    public void setup() throws Exception {
        kinesisClient = createKinesisClient();
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
