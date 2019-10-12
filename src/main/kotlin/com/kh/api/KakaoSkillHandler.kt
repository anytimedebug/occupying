package com.kh.api

import com.kh.api.request.LoginParams
import com.kh.api.request.SkillPayload
import com.kh.api.request.SearchTrainParams
import com.kh.api.response.OutPuts
import com.kh.api.response.SkillResponse
import com.kh.api.response.carousel.CarouselTemplate
import com.kh.api.response.simpleText.SimpleText
import com.kh.api.response.simpleText.SimpleTextTemplate
import com.kh.occupying.Korail
import com.kh.occupying.domain.Login
import com.kh.occupying.domain.SeatCode
import com.kh.occupying.domain.Train
import com.kh.occupying.dto.response.*
import com.kh.util.mapTo
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class KakaoSkillHandler(val korail: Korail) {

    fun findTrains(req: ServerRequest): Mono<ServerResponse> {
        return req.bodyToMono(SkillPayload::class.java)
                .flatMap {
                    val payload = it.action.params
                            .mapTo<SearchTrainParams>()
                            .getSearchParams()
                    korail.search(payload)
                }.flatMap {
                    ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .syncBody(makeBody(it))
                }
    }

    fun reserveTrain(req: ServerRequest): Mono<ServerResponse> {
        return req.bodyToMono(SkillPayload::class.java)
                .flatMap {
                    val searchPayload = it.action.clientExtra!!
                            .mapTo<SearchTrainParams>()
                            .getSearchParams()
                    val findTrains = korail.search(searchPayload)
                            .map { response ->
                                (response as SearchResponse).train.items
                                        .map { item -> Train.fromDto(item) }
                                        .first {train ->
                                            train.no == it.action.clientExtra["train-no"]
                                        }
                            }

                    val loginPayload = it.action.params.mapTo<LoginParams>()
                    val loginResult = korail.login(
                            id = loginPayload.id,
                            pw = loginPayload.pw
                    ).map {response ->
                        Login.fromDto(response as LoginResponse)
                    }

                    Mono.zip(loginResult, findTrains)
                }.flatMap {
                    korail.reserve(it.t1, it.t2)
                }.flatMap {
                    val message = if (it is ReservationResponse) {
                        "예약 성공"
                    } else {
                        "예약 실패"
                    }

                    val body  = SkillResponse(
                            version = "2.0",
                            template = OutPuts(listOf(
                                    SimpleTextTemplate(
                                            simpleText = SimpleText(
                                                    message
                                            )
                                    )
                            ))
                    )
                    ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .syncBody(body)
                }
    }

    private fun makeBody(it: CommonResponse): SkillResponse<CarouselTemplate> {
        val template = OutPuts(
                outputs = listOf(
                        CarouselTemplate.fromResponse(it)
                )
        )
        return SkillResponse(
                version = "2.0",
                template = template
        )
    }

}