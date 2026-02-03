package com.jzargo.websocketapi;

import com.jzargo.websocketapi.handler.KafkaLeaderboardReceivedHandler;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@Profile("test")
@EmbeddedKafka
public class KafkaHandlerIntegrationTest {
    @MockitoSpyBean
    private KafkaLeaderboardReceivedHandler kafkaReceivedHandler;


    void test_HandleLocalLeaderboardUpdate_withUsers(){

    }
}
