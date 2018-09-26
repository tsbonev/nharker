package com.tsbonev.nharker.core

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class EntryLinkerImpl : EntryLinker {

    override fun findLinksInContent(entry: Entry, articleLinkTitles: List<String>): List<String> {

        val content = removeExplicitLinksFromContent(entry.content, entry.links.keys.toList())

        val dashConcatenatedContent = content.toLinkTitle()

        val discoveredLinks = mutableListOf<String>()

        articleLinkTitles.forEach{
            if(dashConcatenatedContent.contains(it)) discoveredLinks.add(it)
        }

        return discoveredLinks
    }

    private fun removeExplicitLinksFromContent(content: String, explicitLinks: List<String>): String{
        var editedContent = content

        explicitLinks.forEach {
            editedContent = editedContent.replace(it, "")
        }

        return editedContent.trim()
    }
}