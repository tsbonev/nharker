package com.tsbonev.nharker.core

/**
 * A request object containing all of the necessary
 * information for creating a Catalogue object.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class CatalogueRequest(val title: String,
                            val parentId: String? = null)