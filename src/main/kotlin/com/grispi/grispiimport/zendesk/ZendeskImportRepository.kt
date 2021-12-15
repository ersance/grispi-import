package com.grispi.grispiimport.zendesk

import org.springframework.data.mongodb.repository.MongoRepository

interface ZendeskImportRepository: MongoRepository<ZendeskTenantImport, String> {
}