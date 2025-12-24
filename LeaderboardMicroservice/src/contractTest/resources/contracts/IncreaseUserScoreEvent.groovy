package contracts

com.springframework.cloud.contract.spec.Contract.make {
    description "Increase user score event contract"

    label "increaseUserScoreEvent"
    inputMessage {
        sentTo('leaderboard.events.increaseUserScore')

        body(
                leaderboardId: "lb123",
                userId: "10",
                scoreIncrement: 500
        )

        headers {
            header('contentType', applicationJson())
        }
    }
    outputMessage {
        sentTo('leaderboard.events.increaseUserScore')

        body(
                leaderboardId: "lb123",
        )

        headers {
            header('contentType', applicationJson())
        }
    }
}