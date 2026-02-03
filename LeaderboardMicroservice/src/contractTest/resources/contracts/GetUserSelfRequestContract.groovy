package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method 'GET'
        urlPath'/api/v1/leaderboard/score/lb123'
    }

    response {
        status 200
        body("""
        {
            "userId": "10",
            "leaderboardId": "lb123",
            "score": 9876,
            "rank": 1
        }
        """)
        headers {
            contentType(applicationJson())
        }
    }
}