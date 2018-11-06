package com.tsbonev.nharker.server.adapter.koin

import com.tsbonev.nharker.adapter.nitrite.NitriteArticleSynonymProvider
import com.tsbonev.nharker.adapter.nitrite.NitriteArticles
import com.tsbonev.nharker.adapter.nitrite.NitriteCatalogues
import com.tsbonev.nharker.adapter.nitrite.NitriteEntityTrashCollector
import com.tsbonev.nharker.adapter.nitrite.NitriteEntries
import com.tsbonev.nharker.adapter.nitrite.NitriteEntryLinker
import com.tsbonev.nharker.core.ArticleSynonymProvider
import com.tsbonev.nharker.core.Articles
import com.tsbonev.nharker.core.Catalogues
import com.tsbonev.nharker.core.Entries
import com.tsbonev.nharker.core.EntryLinker
import com.tsbonev.nharker.core.TrashCollector
import com.tsbonev.nharker.cqrs.EventBus
import com.tsbonev.nharker.cqrs.SimpleEventBus
import com.tsbonev.nharker.server.helpers.ExceptionLogger
import com.tsbonev.nharker.server.workflow.ArticleSynonymWorkflow
import com.tsbonev.nharker.server.workflow.ArticleWorkflow
import com.tsbonev.nharker.server.workflow.CatalogueWorkflow
import com.tsbonev.nharker.server.workflow.EntryLinkingWorkflow
import com.tsbonev.nharker.server.workflow.EntryWorkflow
import com.tsbonev.nharker.server.workflow.TrashingWorkflow
import org.dizitart.kno2.nitrite
import org.koin.dsl.module.module

/**
 * Modules must be loaded in the order they are written in here.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */

/**
 * Database module with an in-memory storage.
 */
val fakeNitriteDbModule = module {
	single { nitrite {} }
}

/**
 * Persistent module dependent on the database module.
 */
val nitritePersistenceModule = module {
	single<Entries> { NitriteEntries(nitriteDb = get()) }
	single<Articles> { NitriteArticles(nitriteDb = get()) }
	single<Catalogues> { NitriteCatalogues(nitriteDb = get()) }

	single<TrashCollector> { NitriteEntityTrashCollector(nitriteDb = get()) }
	single<ArticleSynonymProvider> { NitriteArticleSynonymProvider(nitriteDb = get()) }
}

/**
 * Entry linking module dependent on the persistent module.
 */
val nitriteEntryLinkerModule = module {
	single<EntryLinker> { NitriteEntryLinker(entries = get(), articles = get(), synonyms = get()) }
}

/**
 * Simple Cqrs module.
 */
val simpleCqrsModule = module {
	single<EventBus> { SimpleEventBus() }
}

/**
 * Exception logging module.
 */
val exceptionLoggingModule = module {
	single { ExceptionLogger() }
}

/**
 * Module of the workflows, dependent on the persistent module, the cqrs module,
 * the linking module and the exception logging module.
 */
val workflowModule = module {
	single { EntryWorkflow(eventBus = get(), entries = get(), exceptionLogger = get()) }
	single { ArticleWorkflow(eventBus = get(), articles = get(), exceptionLogger = get())}
	single { CatalogueWorkflow(eventBus = get(), catalogues = get(), exceptionLogger = get())}
	single { TrashingWorkflow(eventBus = get(), trashCollector = get(), exceptionLogger = get()) }
	single { ArticleSynonymWorkflow(eventBus = get(), synonyms = get(), exceptionLogger = get()) }
	single { EntryLinkingWorkflow(eventBus = get(), linker = get()) }
}