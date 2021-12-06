package com.grispi.grispiimport.grispi

import jodd.json.JsonSerializer

abstract class GrispiApiRequest {

    fun toJson(): String {
        return JsonSerializer().deep(true).serialize(this)
    }

}