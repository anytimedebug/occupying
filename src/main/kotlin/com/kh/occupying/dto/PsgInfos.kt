package com.kh.occupying.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PsgInfos(
        @JsonProperty("psg_info")
        val psgInfo: List<PsgInfo>
)
