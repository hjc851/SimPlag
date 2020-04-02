package me.haydencheers.simplag.synonyms

import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.CaseUtils
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.json.bind.JsonbBuilder
import javax.ws.rs.client.ClientBuilder
import kotlin.math.min

interface ISynonymClient {
    fun getSynonymsForTerm(term: String): List<String>
}

@Singleton
class DataMuseSynonymClient : ISynonymClient {
    @Inject
    @Named("SynonymCache")
    lateinit var cache: MutableMap<String, List<String>>

    val serialiser = JsonbBuilder.create()

    private val datamuseTarget = ClientBuilder.newClient()
            .target("https://api.datamuse.com/words")

    override fun getSynonymsForTerm(term: String): List<String> {

        val components = term.split(Regex("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))
            .flatMap { it.split("_") }

        val componentMatches = components.map { component ->

            if (component.isEmpty())
                return@map listOf("name")

            val capitaliseFirst = component[0].isUpperCase()
            val matches = getSynonymsForComponent(component)
                    .map { CaseUtils.toCamelCase(it.replace("[^A-Za-z0-9]", " "), capitaliseFirst, ' ', '-') }

            if (matches.isEmpty())
                return@map listOf(component)
            else
                return@map matches.take(min(matches.size, 25))
        }

        val cartesianProduct = componentMatches.cartesianProduct()
                .map { it.joinToString("") }

        return cartesianProduct
    }

    @Synchronized
    private fun getSynonymsForComponent(component: String): List<String> {
        val component = component.toLowerCase()
        if (cache.containsKey(component))
            return cache[component]!!

        synchronized(this) {
            println("Querying synonyms for term '$component'")
            val response = datamuseTarget.queryParam("ml", component)
                .request()
                .get()

            val ris = response.entity as InputStream
            val matches = serialiser.fromJson(ris, Array<Term>::class.java)
                .sortedByDescending { it.score }
                .map { it.word }
                .filter { StringUtils.isAlphanumeric(it) && it.firstOrNull()?.isLetter() ?: false }

            cache[component] = matches
            return matches
        }
    }

    private class Term {
        lateinit var word: String
        var score: Int = 0
        lateinit var tags: List<String>
    }
}