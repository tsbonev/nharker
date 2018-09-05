package com.tsbonev.nharker.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class CatalogueRequest(val title: String, val parentCatalogue: String = "None")