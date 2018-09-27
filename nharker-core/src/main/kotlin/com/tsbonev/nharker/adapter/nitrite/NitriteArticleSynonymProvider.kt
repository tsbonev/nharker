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
class NitriteArticleSynonymProvider(private val nitriteDb: Nitrite,
                                    private val collectionName: String = "Article_synonyms",
                                    private val globalMapId: String = "Global_synonym_map")
    : ArticleSynonymProvider {

    private val coll: NitriteCollection
        get() = nitriteDb.getCollection(collectionName)

    override fun getSynonymMap(): Map<String, String> {
        val mapDocument = coll.find("globalId" eq globalMapId).firstOrNull()
                ?: updateOrCreateMap(mutableMapOf())

        @Suppress("UNCHECKED_CAST")
        return mapDocument["synonymMap"] as Map<String, String>
    }

    override fun addSynonym(articleSynonym: String, article: Article): String {
        val map = getSynonymMap().toMutableMap()

        if (map[articleSynonym] != null) throw SynonymAlreadyTakenException()

        map[articleSynonym] = article.linkTitle

        updateOrCreateMap(map)
        return articleSynonym
    }

    override fun removeSynonym(articleSynonym: String): String {
        val map = getSynonymMap().toMutableMap()

        if (map[articleSynonym] == null) throw SynonymNotFoundException()

        map.remove(articleSynonym)

        updateOrCreateMap(map)
        return articleSynonym
    }

    /**
     * Updates or creates a document object with the global map id
     * and a given map as a value.
     *
     * @param synonymMap The map whose values to save.
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