package com.grispi.grispiimport.grispi

class User(
    val id: Long,
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String,
    val organization: Organization,
    val tags: HashSet<String>,
    val roles: HashSet<Role>,
    val groups: HashSet<Role>
) {

    companion object {
        const val NO_PASSWORD = "_**NO_PASSWORD_f0r_1nternal**_";
    }

}