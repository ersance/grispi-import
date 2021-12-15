package com.grispi.grispiimport.zendesk

import com.fasterxml.jackson.annotation.JsonIgnore

abstract class ZendeskEntity {

    @JsonIgnore
    lateinit var operationId: String

}