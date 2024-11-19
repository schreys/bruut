package com.stubru.bruut.shared

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object MetadataJsonExtractor {
    fun fetchNowPlaying(): Pair<String, String>? {
        val url = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v2/channels/livestream-audio-stubrubruut"
        val client = OkHttpClient()

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let {
                    val json = JSONObject(it)
                    val schedule = json.getJSONArray("schedule")
                    for (i in 0 until schedule.length()) {
                        val item = schedule.getJSONObject(i)
                        if (item.has("nowOnAirItem")) {
                            val nowOnAirItem = item.getJSONObject("nowOnAirItem")
                            val title = nowOnAirItem.optString("title", "Unknown Title")
                            val artist = nowOnAirItem.optString("artist", "Unknown Artist")
                            return Pair(title, artist)
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}