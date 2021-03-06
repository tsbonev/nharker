package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleSynonymProvider
import com.tsbonev.nharker.core.SynonymAlreadyTakenException
import com.tsbonev.nharker.core.SynonymNotFoundException
import org.dizitart.kno2.filters.eq
import org.dizitart.no2.Document
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteCollection

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticleSynonymProvider(
	private val nitriteDb: Nitrite,
	private val collectionName: String = "Article_synonyms",
	private val globalMapId: String = "Global_synonym_map"
) : ArticleSynonymProvider {
	private val coll: NitriteCollection
		get() = nitriteDb.getCollection(collectionName)

	@Suppress("UNCHECKED_CAST")
	override fun getSynonymMap(): Map<String, String> {
		val mapDocument = coll.find("globalId" eq globalMapId).firstOrNull()
			?: updateOrCreateMap(mutableMapOf())

		return mapDocument["synonymMap"] as Map<String, String>
	}

	override fun addSynonym(synonym: String, article: Article): String {
		val map = getSynonymMap().toMutableMap()

		if (map.containsKey(synonym)) throw SynonymAlreadyTakenException(synonym)

		map[synonym] = article.id

		updateOrCreateMap(map)
		return synonym
	}

	override fun removeSynonym(synonym: String): Pair<String, String> {
		val map = getSynonymMap().toMutableMap()

		if (!map.containsKey(synonym)) throw SynonymNotFoundException(synonym)

		val articleId = map[synonym]!!

		map.remove(synonym)

		updateOrCreateMap(map)
		return synonym to articleId
	}

	/**
	 * Updates or creates a document object with the global map id
	 * and a given map as a value.
	 *
	 * @param synonymMap The map whose ids to save.
	 * @return A Document of the map.
	 */
	private fun updateOrCreateMap(synonymMap: Map<String, String>): Document {
		val mapDocument = coll.find("globalId" eq globalMapId).firstOrNull()
			?: Document.createDocument("globalId", globalMapId)

		mapDocument["synonymMap"] = synonymMap

		coll.update(mapDocument, true)
		return mapDocument
	}
}