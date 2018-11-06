package com.tsbonev.nharker.adapter.nitrite.helpers

import com.tsbonev.nharker.core.IdGenerator
import org.dizitart.no2.objects.ObjectRepository
import org.dizitart.no2.objects.filters.ObjectFilters

/**
 * Helpers that aid the generation of unique ids in a NitriteDb context.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Generates a new id until it is verified to be unique.
 *
 * @param repo The collection that requires a unique id.
 * @return An id unique to that collection.
 */
fun IdGenerator.generateNitriteUniqueId(repo: ObjectRepository<*>): String {
	var generatedId = this.generateId()

	while (repo.find(ObjectFilters.eq("id", generatedId)).firstOrNull() != null){
		generatedId = this.generateId()
	}

	return generatedId
}