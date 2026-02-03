package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    label 'initOutOfTimeEventContract'
    input {
        triggeredBy('initOutOfTimeEvent()')
    }

    outputMessage {
        sentTo("leaderboard-update-topic")
        body(
                [
                    leaderboardId:  "leaderboard455",
                    ownerId: "123"
                ]
        )
        headers {
            header('message-id', 'LFLGOPFG213')
        }
    }
}