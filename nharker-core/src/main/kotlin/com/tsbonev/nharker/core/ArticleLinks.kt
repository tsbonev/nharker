package com.tsbonev.nharker.core

/**
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
data class ArticleLinks(val links: MutableMap<String, Int>) {

    fun get(articleTitle: String): Int?{
        return links[articleTitle]
    }

    fun contains(articleTitle: String): Boolean{
        return links[articleTitle] != null
    }

    fun addLink(articleTitle: String){
        links[articleTitle] = (links[articleTitle] ?: 0) + 1
    }

    fun removeLink(articleTitle: String){
        if(contains(articleTitle)){
            val timesMentioned = links[articleTitle]!! - 1
            if(timesMentioned <= 0) links.remove(articleTitle)
            else links[articleTitle] =  timesMentioned
        }
    }
}