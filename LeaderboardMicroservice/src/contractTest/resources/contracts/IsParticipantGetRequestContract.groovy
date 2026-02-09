package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    request {
        method 'GET'
        urlPath '/api/v1/leaderboard/view/participant/lb123'
    }

    response {
        status 200
        body "true"
        headers {
            contentType(applicationJson())
        }
    }
}
