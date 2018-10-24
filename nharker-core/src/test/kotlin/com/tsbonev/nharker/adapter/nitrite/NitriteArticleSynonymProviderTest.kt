package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.SynonymAlreadyTakenException
import com.tsbonev.nharker.core.SynonymNotFoundException
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.nitrite
import org.dizitart.no2.Document
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticleSynonymProviderTest {

    private val db = nitrite { }

    private val globalMapId = "Test_synonym_map_id"
    private val mapCollectionName = "Test_article_synonyms"

    private val synonymMapProvider = NitriteArticleSynonymProvider(
            db,
            mapCollectionName,
            globalMapId
    )

    private val article = Article(
            "::article-id::",
            "article-title",
            "Article title",
            LocalDateTime.now()
    )

    private val synonymMap = mapOf("::presaved-synonym::" to "article-title")

    @Suppress("UNCHECKED_CAST")
    private val presavedMap: Map<String, String>
        get() = db.getCollection(mapCollectionName)
                .find("globalId" eq globalMapId)
                .first()["synonymMap"] as Map<String, String>

    @Before
    fun setUp() {
        val mapDocument = Document.createDocument("globalId", globalMapId)
        mapDocument["synonymMap"] = synonymMap
        db.getCollection(mapCollectionName).insert(mapDocument)
    }

    @Test
    fun `Getting synonym map creates it`() {
        db.getCollection(mapCollectionName).remove("globalId" eq globalMapId)

        val synonymMap = synonymMapProvider.getSynonymMap()

        assertThat(synonymMap, Is(emptyMap()))
    }

    @Test
    fun `Get synonym map`() {
        val synonymMap = synonymMapProvider.getSynonymMap()

        assertThat(synonymMap, Is(synonymMap))
    }

    @Test
    fun `Add synonym to map`() {
        val synonym = synonymMapProvider.addSynonym("::synonym::", article)

        assertThat(presavedMap, Is(synonymMap.plus(
                synonym to article.id
        )))
    }

    @Test(expected = SynonymAlreadyTakenException::class)
    fun `Adding existing synonym throws exception`() {
        synonymMapProvider.addSynonym("::presaved-synonym::", article)
    }

    @Test
    fun `Remove synonym from map`() {
        synonymMapProvider.removeSynonym("::presaved-synonym::")


        assertThat(presavedMap, Is(emptyMap()))
    }

    @Test(expected = SynonymNotFoundException::class)
    fun `Removing non-existent synonym throws exception`() {
        synonymMapProvider.removeSynonym("::non-existent-synonym::")
    }
}