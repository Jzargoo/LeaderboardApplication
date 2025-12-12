package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method 'GET'
        urlPath('/view/lb123')
    }

    response {
        status 200
        body("""
        {
            "leaderboard": {
                "1": 100,
                "2": 90,
                "3": 80
            },
            "description": "Test Description",
            "name": "Test Leaderboard",
            "leaderboardId": "lb123",
        }
        """)
        headers {
            contentType(applicationJson())
        }
    }
}