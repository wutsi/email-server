package com.wutsi.email.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities.EscapeMode.extended
import org.springframework.stereotype.Service

@Service
class EmailStyleEnhancer {
    companion object {
        private val STYLES = mapOf(
            ".content" to """
                background: white;
                font-family: 'PT Sans', sans-serif;
                font-size: 1em; margin: 0 auto;
                max-width: 600px;
            """.trimIndent(),

            ".btn-primary" to """
                display: inline-block;
                font-weight: 400;
                color: #FFFFFF;
                background-color: #1D7EDF;
                text-align: center;
                vertical-align: middle;
                border: 1px solid transparent;
                padding: .375rem .75rem;
                font-size: 1rem;
                line-height: 1.5;
                text-decoration: none;
            """.trimIndent(),

            ".btn-secondary" to """
                display: inline-block;
                font-weight: 400;
                color: gray;
                background-color: #e4edf7;
                text-align: center;
                vertical-align: middle;
                border: 1px solid lightgray;
                padding: .375rem .75rem;
                line-height: 1.5;
                text-decoration: none;
            """.trimIndent(),

            ".small" to """
                font-size: small;
            """.trimIndent(),

            ".x-small" to """
                font-size: x-small;
            """.trimIndent(),

            ".no-margin" to """
                margin: 0
            """.trimIndent(),

            ".border" to """
                border: 1px solid lightgray;
            """.trimIndent(),

            ".rounded" to """
                border-radius: 5px
            """.trimIndent(),

            ".box-highlight" to """
                background: #e4edf7;
                border: 1px solid ##1D7EDF;
            """.trimIndent()

        )
    }

    fun enhance(html: String): String {
        val doc = Jsoup.parse(html)
        STYLES.keys.forEach {
            apply(it, doc)
        }
        doc.outputSettings()
            .charset("ASCII")
            .escapeMode(extended)
            .indentAmount(2)
            .prettyPrint(true)
            .outline(true)
        return doc.toString()
    }

    private fun apply(selector: String, doc: Document) {
        val style = STYLES[selector]?.replace("\n", "") ?: return
        doc.select(selector).forEach {
            if (it.hasAttr("style")) {
                it.attr("style", it.attr("style") + ";$style")
            } else {
                it.attr("style", style)
            }
        }
    }
}
