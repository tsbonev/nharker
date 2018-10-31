package com.tsbonev.nharker.adapter.nitrite

import com.tsbonev.nharker.core.Article
import com.tsbonev.nharker.core.ArticleNotFoundException
import com.tsbonev.nharker.core.ArticleRequest
import com.tsbonev.nharker.core.ArticleTitleTakenException
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Catalogue
import com.tsbonev.nharker.core.ElementNotInMapException
import com.tsbonev.nharker.core.Entry
import com.tsbonev.nharker.core.EntryAlreadyInArticleException
import com.tsbonev.nharker.core.EntryNotInArticleException
import com.tsbonev.nharker.core.Paginator
import com.tsbonev.nharker.core.SortBy
import org.dizitart.kno2.filters.elemMatch
import org.dizitart.kno2.filters.eq
import org.dizitart.kno2.filters.text
import org.dizitart.no2.Nitrite
import org.dizitart.no2.NitriteId
import org.dizitart.no2.objects.ObjectRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
class NitriteArticles(
	private val nitriteDb: Nitrite,
	private val collectionName: String = "Articles",
	private val clock: Clock = Clock.systemUTC()
) : Articles, Paginator<Article> {

	private val repo: ObjectRepository<Article>
		get() = nitriteDb.getRepository(collectionName, Article::class.java)

	private val paginator: Paginator<Article> by lazy {
		NitritePaginator(repo)
	}

	override fun create(articleRequest: ArticleRequest): Article {
		val article = Article(
			NitriteId.newId().toString(),
			articleRequest.fullTitle,
			LocalDateTime.now(clock)
		)

		if (repo.find(Article::title text article.title)
				.filter { it.title == article.title }
				.any())
			throw ArticleTitleTakenException(articleRequest.fullTitle)

		repo.insert(article)
		return article
	}

	override fun save(article: Article): Article {
		repo.update(article, true)
		return article
	}

	override fun changeTitle(articleId: String, newTitle: String): Article {
		val article = findByIdOrThrow(articleId)

		if (repo.find(Article::title text newTitle).toList()
				.filter { it.title == newTitle }
				.any()
		) throw ArticleTitleTakenException(newTitle)

		val updatedArticle = article.copy(title = newTitle)

		repo.update(updatedArticle)
		return updatedArticle
	}

	override fun getAll(order: SortBy): List<Article> {
		return paginator.getAll(order)
	}

	override fun getPaginated(order: SortBy, page: Int, pageSize: Int): List<Article> {
		return paginator.getPaginated(order, page, pageSize)
	}

	override fun getById(articleId: String): Optional<Article> {
		val article = repo.find(Article::id eq articleId).firstOrNull()
			?: return Optional.empty()

		return Optional.of(article)
	}

	override fun getByCatalogue(catalogue: Catalogue): List<Article> {
		return repo.find(Article::catalogues elemMatch (Article::catalogues eq catalogue.id)).toList()
	}

	override fun deleteById(articleId: String): Article {
		val article = findByIdOrThrow(articleId)

		repo.remove(article)
		return article
	}

	override fun addCatalogue(articleId: String, catalogue: Catalogue): Article {
		val article = findByIdOrThrow(articleId)

		val updatedArticle = article.copy(catalogues = article.catalogues.plus(catalogue.id))

		repo.update(updatedArticle)
		return updatedArticle
	}

	override fun removeCatalogue(articleId: String, catalogue: Catalogue): Article {
		val article = findByIdOrThrow(articleId)

		val updatedArticle = article.copy(catalogues = article.catalogues.minus(catalogue.id))

		repo.update(updatedArticle)
		return updatedArticle
	}

	override fun appendEntry(articleId: String, entry: Entry): Article {
		val article = findByIdOrThrow(articleId)

		if (article.entries.raw().containsKey(entry.id))
			throw EntryAlreadyInArticleException(entry.id, articleId)

		article.entries.append(entry.id)

		repo.update(article)
		return article
	}

	override fun removeEntry(articleId: String, entry: Entry): Article {
		val article = findByIdOrThrow(articleId)

		if (!article.entries.contains(entry.id))
			throw EntryNotInArticleException(entry.id, articleId)

		article.entries.subtract(entry.id)

		repo.update(article)
		return article
	}

	override fun switchEntries(articleId: String, first: Entry, second: Entry): Article {
		val article = findByIdOrThrow(articleId)

		return try {
			article.entries.switch(first.id, second.id)

			repo.update(article)
			article
		} catch (ex: ElementNotInMapException) {
			throw EntryNotInArticleException(ex.reference, articleId)
		}
	}

	override fun attachProperty(articleId: String, propertyName: String, propertyEntry: Entry): Article {
		val article = findByIdOrThrow(articleId)

		article.properties.attachProperty(propertyName, propertyEntry.id)

		repo.update(article)
		return article
	}

	override fun detachProperty(articleId: String, propertyName: String): Article {
		val article = findByIdOrThrow(articleId)

		article.properties.detachProperty(propertyName)

		repo.update(article)
		return article
	}

	override fun searchByFullTitle(searchString: String): List<Article> {
		return repo.find(Article::title text searchString).toList()
	}

	/**
	 * Finds an article by id or throws an exception.
	 *
	 * @param articleId The id to find.
	 * @return The found Article.
	 *
	 * @exception ArticleNotFoundException thrown when the article is not found.
	 */
	private fun findByIdOrThrow(articleId: String): Article {
		return repo.find(Article::id eq articleId).firstOrNull()
			?: throw ArticleNotFoundException(articleId)
	}
}