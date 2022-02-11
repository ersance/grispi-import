package com.grispi.grispiimport.common

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value= HttpStatus.NOT_FOUND)
class NotFoundException(override val message: String): RuntimeException()