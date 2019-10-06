package com.kh.api

import com.kh.api.request.SkillPayload
import com.kh.api.response.SkillResponse
import com.kh.occupying.Korail
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class KakaoSkillHandler(val korail: Korail) {

    fun findTrains(req: ServerRequest): Mono<ServerResponse> {
        return req.bodyToMono(SkillPayload::class.java)
                .flatMap {
                    val params = it.action.params
                    val departureAt = LocalDateTime.parse(
                            params.departureDate + params.departureTime,
                            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

                    korail.search(
                            departureAt = departureAt,
                            departureStation = params.departureStation,
                            destination = params.destination
                    )
                }.flatMap {
                    ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .syncBody(SkillResponse.fromCommonResponse(it))
                }
                .switchIfEmpty(ServerResponse.notFound().build())
    }

}
