package com.grispi.grispiimport.grispi

class Organization(
    val id: Long,
    val name: String,
    val details: String,
    val notes: String,
    val group: Group,
    val tags: HashSet<String>
) {

}