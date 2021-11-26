package com.grispi.grispiimport

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class HtmlController {

  @GetMapping("/{id}")
  fun blog(@PathVariable id: Long): String {
    return "blog" + id;
  }

}