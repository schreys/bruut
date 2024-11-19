package com.stubru.bruut.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object MetadataExtractor {
    suspend fun extractMetadata(streamUrl: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val url = URL(streamUrl)
        val connection = url.openConnection()

        connection.setRequestProperty("Icy-MetaData", "1")
        connection.connect()

        var icyMetaInt = connection.getHeaderFieldInt("icy-metaint", -1)
        val stream = connection.getInputStream()

        if (icyMetaInt != -1) {

            val buffer = ByteArray(icyMetaInt)
            stream.read(buffer)

            val metaDataLength = stream.read() * 16
            if (metaDataLength > 0) {
                val metaDataBuffer = ByteArray(metaDataLength)
                stream.read(metaDataBuffer)

                val metaData = String(metaDataBuffer)

                val title = metaData.substringAfter("StreamTitle='").substringBefore("';")
                val artist = metaData.substringAfter("StreamUrl='").substringBefore("';")

                return@withContext Pair(title, artist)
            }
        }

        return@withContext Pair("", "")
    }
}