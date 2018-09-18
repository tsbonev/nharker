package com.tsbonev.nharker.core

import com.tsbonev.nharker.core.helpers.ReferenceId

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class CatalogueRequest(val title: String, val parentCatalogue: String = ReferenceId.None.value)