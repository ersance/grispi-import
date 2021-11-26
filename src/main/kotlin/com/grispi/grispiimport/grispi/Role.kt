package com.grispi.grispiimport.grispi

class Role private constructor(val authority: String) {

    private constructor(authority: String, vararg impliedAuthorities: String): this(authority)

    companion object {
        val END_USER: Role = Role("ROLE_END_USER")

        val PLUGIN: Role = Role("ROLE_PLUGIN")

        val AGENT: Role = Role("ROLE_AGENT");

        val ADMIN: Role = Role("ROLE_ADMIN", "ROLE_AGENT");

        val INTEGRATION: Role = Role("ROLE_INTEGRATION", "ROLE_ADMIN");
    }

}
