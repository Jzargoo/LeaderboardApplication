package com.jzargo.scoringmicroservice;

import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserEventHappenedCommand;
import com.jzargo.scoringmicroservice.service.ScoringService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ScoringServiceIntegrationTest {
    private static final String LEADERBOARD_ID = "leaderboard-123";
    public static final String LOSE_GAME = "LOSE_GAME";
    public static final String COMPLETE_QUEST = "COMPLETE_QUEST";
    public static final double V_3 = 200.0;

    static final Network network = Network.newNetwork();

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.2.1")
    ).withNetwork(network);

    @Container
    static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("debezium/postgres:latest")
            .withNetwork(network)
            .withDatabaseName("scoresdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
      final static GenericContainer<?> debezium =
            new GenericContainer<>(
                    DockerImageName.parse("quay.io/debezium/connect:latest")
            )
                    .withNetwork(network)
                    .withExposedPorts(8083)
                    .withEnv("BOOTSTRAP_SERVERS", kafka.getBootstrapServers())
                    .withEnv("GROUP_ID", "1")
                    .withEnv("CONFIG_STORAGE_TOPIC", "my_connect_configs")
                    .withEnv("OFFSET_STORAGE_TOPIC", "my_connect_offsets")
                    .withEnv("STATUS_STORAGE_TOPIC", "my_connect_statuses")
                    .withEnv("KEY_CONVERTER_SCHEMAS_ENABLE", "false")
                    .withEnv("VALUE_CONVERTER_SCHEMAS_ENABLE", "false")
                    .withEnv("KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
                    .withEnv("VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
                    .withEnv("DATABASE_HOSTNAME", postgresqlContainer.getHost())
                    .withEnv("DATABASE_PORT", String.valueOf(postgresqlContainer.getMappedPort(5432)))
                    .withEnv("DATABASE_USER", postgresqlContainer.getUsername())
                    .withEnv("DATABASE_PASSWORD", postgresqlContainer.getPassword())
                    .withEnv("DATABASE_DBNAME", postgresqlContainer.getDatabaseName());



    @Autowired
    private TestConsumer testConsumer;

    @DynamicPropertySource
    private static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
    }

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException {
        kafka.start();
        postgresqlContainer.start();

        String url = "http://" + debezium.getHost() + ":" + debezium.getMappedPort(8083) + "/connectors";
        String body = Arrays.toString(Files.readAllBytes(Path.of("src/")));
        HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }
    @Autowired
    private ScoringService scoringService;
    @Test
    @SuppressWarnings("unchecked")
    public void test(){
        LeaderboardEventInitialization build = LeaderboardEventInitialization.builder()
                .lbId(LEADERBOARD_ID)
                .events(
                        Map.of(
                                "WIN_GAME", 100.0,
                                LOSE_GAME, -50.0,
                                COMPLETE_QUEST, V_3,
                                "DEFEAT_BOSS", 500.0
                        )
                )
                .isPublic(true)
                .metadata(new HashMap<>())
                .build();

        scoringService.saveEvents(build);

        UserEventHappenedCommand userEventHappenedCommand =
                new UserEventHappenedCommand();

        userEventHappenedCommand.setLbId(LEADERBOARD_ID);
        userEventHappenedCommand.setEventName(COMPLETE_QUEST);
        userEventHappenedCommand.setUserId(42L);
        userEventHappenedCommand.setMetadata(new HashMap<>());

        scoringService.saveUserEvent(userEventHappenedCommand);
        Map<String, Object> lastMessage = testConsumer.getLastMessage();
        Map<String, Object> payload = (Map<String, Object>) lastMessage.get("payload");
        Map<String, Object> after = (Map<String, Object>) payload.get("after");

        assertEquals(userEventHappenedCommand.getUserId(), after.get("user_id"));
        assertEquals(userEventHappenedCommand.getLbId(), after.get("lb_id"));
        assertEquals("c", payload.get("op"));
        assertEquals(V_3, after.get("score_delta"));

    }
}
