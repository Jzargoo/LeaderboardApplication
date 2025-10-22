package com.jzargo.scoringmicroservice;

import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserEventHappenedCommand;
import com.jzargo.scoringmicroservice.config.KafkaConfig;
import com.jzargo.scoringmicroservice.service.ScoringService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class ScoringServiceIntegrationTest {
    private static final String LEADERBOARD_ID = "leaderboard-123";
    public static final String LOSE_GAME = "LOSE_GAME";
    public static final String COMPLETE_QUEST = "COMPLETE_QUEST";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Container
    static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("scoringdb")
            .withUsername("postgres")
            .withPassword("root333");

    @Container
    static final GenericContainer<?> debezium = new GenericContainer<>(
            DockerImageName.parse("debezium/connect:latest")
    )
            .withEnv("BOOTSTRAP_SERVERS", kafka.getBootstrapServers())
            .withEnv("GROUP_ID", "1")
            .withEnv("CONFIG_STORAGE_TOPIC", "my_connect_configs")
            .withEnv("OFFSET_STORAGE_TOPIC", "my_connect_offsets")
            .withEnv("STATUS_STORAGE_TOPIC", "my_connect_statuses")
            .withEnv("KEY_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
            .withEnv("VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("DATABASE_HOSTNAME", postgresqlContainer.getHost())
            .withEnv("DATABASE_PORT", postgresqlContainer.getFirstMappedPort().toString())
            .withEnv("DATABASE_USER", postgresqlContainer.getUsername())
            .withEnv("DATABASE_PASSWORD", postgresqlContainer.getPassword())
            .withEnv("DATABASE_DBNAME", postgresqlContainer.getDatabaseName())
            .withExposedPorts(8083);
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
        debezium.start();
        String url = "http://" + debezium.getHost() + ":" + debezium.getMappedPort(8083) + "/connectors";
        String body = """
                {
                  "name": "pg-connector",
                  "config": {
                    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                    "database.hostname": "%s",
                    "database.port": "%s",
                    "database.user": "jzargo",
                    "database.password": "root333",
                    "database.dbname": "generaldb",
                    "topic.prefix": "dbz",
                    "plugin.name": "pgoutput"
                  }
                }
                """.formatted(postgresqlContainer.getHost(), postgresqlContainer.getMappedPort(5432));
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
    public void test(){
        LeaderboardEventInitialization build = LeaderboardEventInitialization.builder()
                .lbId(LEADERBOARD_ID)
                .events(
                        Map.of(
                                "WIN_GAME", 100.0,
                                LOSE_GAME, -50.0,
                                COMPLETE_QUEST, 200.0,
                                "DEFEAT_BOSS", 500.0
                        )
                )
                .isPublic(true)
                .metadata(new HashMap<>())
                .build();

        scoringService.saveEvents(build);

        UserEventHappenedCommand userEventHappenedCommand =
                new UserEventHappenedCommand(LEADERBOARD_ID, COMPLETE_QUEST, 42L, Map.of());

        scoringService.saveUserEvent(userEventHappenedCommand);
        Map<String, Object> lastMessage = testConsumer.getLastMessage();


    }
}
