package com.stubru.bruut.shared
import org.jsoup.Jsoup

object MetadataScraper {
    fun scrapeVRTData(): Pair<String, String>? {
        val url = "https://www.vrt.be/vrtmax/livestream/audio/studio-brussel-zware-gitaren"
        return try {
            val doc = Jsoup.connect(url).get()
            val mainTitle = doc.selectFirst("span.main-title")?.text() ?: "Unknown Title"
            val subTitle = doc.selectFirst("span.sub-title")?.text() ?: "Unknown Artist"
            Pair(mainTitle, subTitle)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}