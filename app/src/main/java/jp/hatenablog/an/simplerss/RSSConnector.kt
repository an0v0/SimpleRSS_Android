package jp.hatenablog.an.simplerss

import android.content.Context
import android.content.ContextWrapper
import android.os.AsyncTask
import android.util.Xml
import jp.hatenablog.an.simplerss.model.RSSChannel
import jp.hatenablog.an.simplerss.model.RSSItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class RSSConnector(private val context: ContextWrapper) {

    companion object {
        // RSSフィードのURL保存key
        private const val keyRssUrl = "RssUrl"
        private const val keyPreference = "Preference"
    }

    // XMLタグ
    private enum class Tag(val value: String) {
        rss("rss"),
        channel("channel"),
        item("item"),
        title("title"),
        link("link"),
        description("description"),
        pubDate("pubDate"),
        enclosure("enclosure"),
    }

    // enclosureタグの属性
    private enum class EnclosureAttribute(val value: String) {
        length("length"),
        type("type"),
        url("url"),
    }

    // RSSフィードのURL
    var rssUrl: String?
        get() {
            val preference = context.getSharedPreferences(keyPreference, Context.MODE_PRIVATE)
            return preference.getString(keyRssUrl, null)
        }
        set(value) {
            val preference = context.getSharedPreferences(keyPreference, Context.MODE_PRIVATE)
            val editor = preference.edit()
            if (value != null && value.isNotEmpty()) {
                editor.putString(keyRssUrl, value)
            }
            else {
                editor.remove(keyRssUrl)
            }
            editor.apply()
        }

    // コールバック
    var callback: RSSConnectorCallback<String>? = null

    // 分析中のデータ
    var rssChannel: RSSChannel? = null

    // 通信クラス
    private var downloadTask: DownloadTask? = null


    /**
     * 有効なRSSフィードのURLがあるかどうか
     *
     * @return RSSフィードの登録状態
     */
    fun hasRssUrl(): Boolean {
        return rssUrl != null
    }

    /**
     * RSSをダウンロードする
     */
    fun downloadRSSItems() {
        rssUrl ?: return

        callback?.also {
            downloadTask = DownloadTask(it, this).apply {
                execute(rssUrl)
            }
        }
    }

    /**
     * ダウンロード処理
     */
    private class DownloadTask(callback: RSSConnectorCallback<String>, connector: RSSConnector)
        : AsyncTask<String, Int, DownloadTask.Result>() {

        private var callback: RSSConnectorCallback<String>? = null
        private var connector: RSSConnector? = null

        init {
            this.callback = callback
            this.connector = connector
        }

        private class Result {
            var rssChannel: RSSChannel? = null
            var exception: Exception? = null

            constructor(rssChannel: RSSChannel) {
                this.rssChannel = rssChannel
            }

            constructor(exception: Exception) {
                this.exception = exception
            }
        }

        override fun doInBackground(vararg urls: String): Result? {
            var result: Result? = null
            if (!isCancelled && urls.isNotEmpty()) {
                val urlString = urls[0]
                result = try {
                    val url = URL(urlString)
                    val channel = downloadUrl(url)
                    if (channel != null) {
                        Result(channel)
                    }
                    else {
                        throw IOException("No response received.")
                    }
                } catch (e: Exception) {
                    Result(e)
                }
            }
            return result
        }

        override fun onPostExecute(result: Result?) {
            callback?.apply {
                connector?.let { connector ->
                    result?.rssChannel?.also { rssChannel ->
                        connector.rssChannel = rssChannel
                        connectorDidFinishDownloading(connector)
                        return
                    }
                    didFailedWithError(connector, result?.exception)
                }
            }
        }

        /**
         * RSSをダウンロードする
         */
        @Throws(IOException::class)
        private fun downloadUrl(url: URL): RSSChannel? {
            var connection: HttpsURLConnection? = null
            return try {
                connection = (url.openConnection() as? HttpsURLConnection)
                connection?.run {
                    readTimeout = 3000
                    connectTimeout = 3000
                    requestMethod = "GET"
                    doInput = true
                    connect()
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        throw IOException("HTTP error code: $responseCode")
                    }
                    parse(inputStream)
                }
            }
            finally {
                connection?.inputStream?.close()
                connection?.disconnect()
            }
        }

        /**
         * XMLをパースする
         *
         * @param inputStream 入力ストリーム
         * @return RSSデータ
         */
        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(inputStream: InputStream): RSSChannel? {
            return inputStream.use<InputStream, RSSChannel?> { input ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(input, null)
                parser.nextTag()
                readRSS(parser)
            }
        }

        /**
         * XMLをパースする（rssタグ）
         *
         * @param parser XMLパーサ
         * @return Channelデータ
         */
        @Throws(XmlPullParserException::class, IOException::class)
        private fun readRSS(parser: XmlPullParser): RSSChannel? {
            parser.require(XmlPullParser.START_TAG, null, Tag.rss.value)
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                if (parser.name == Tag.channel.value) {
                    return readChannel(parser)
                }
                else {
                    skip(parser)
                }
            }
            return null
        }

        /**
         * XMLをパースする（channelタグ）
         *
         * @param parser XMLパーサ
         * @return Channelデータ
         */
        @Throws(XmlPullParserException::class, IOException::class)
        private fun readChannel(parser: XmlPullParser): RSSChannel {
            parser.require(XmlPullParser.START_TAG, null, Tag.channel.value)
            val channel = RSSChannel()
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                when (parser.name) {
                    Tag.title.value -> channel.title = readText(parser, Tag.title.value)
                    Tag.link.value -> channel.link = readText(parser, Tag.link.value)
                    Tag.item.value -> channel.items.add(readItem(parser))
                    else -> skip(parser)
                }
            }
            return channel
        }

        /**
         * XMLをパースする（itemタグ）
         *
         * @param parser XMLパーサ
         * @return Itemデータ
         */
        @Throws(XmlPullParserException::class, IOException::class)
        private fun readItem(parser: XmlPullParser): RSSItem {
            parser.require(XmlPullParser.START_TAG, null, Tag.item.value)
            val rssItem = RSSItem()
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                when (parser.name) {
                    Tag.title.value -> rssItem.title = readText(parser, Tag.title.value)
                    Tag.link.value -> rssItem.link = readText(parser, Tag.link.value)
                    Tag.description.value -> rssItem.itemDescription = readText(parser, Tag.description.value)
                    Tag.pubDate.value -> rssItem.pubDate = readText(parser, Tag.pubDate.value)
                    Tag.enclosure.value -> rssItem.enclosureUrl = readEnclosure(parser)
                    else -> skip(parser)
                }
            }
            return rssItem
        }

        /**
         * XMLから指定されたタグの文字列を取得する
         *
         * @param parser XMLパーサ
         * @param tag タグ文字列
         * @return 指定されたタグの文字列
         */
        @Throws(IOException::class, XmlPullParserException::class)
        private fun readText(parser: XmlPullParser, tag: String): String {
            parser.require(XmlPullParser.START_TAG, null, tag)
            var result = ""
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.text
                parser.nextTag()
            }
            parser.require(XmlPullParser.END_TAG, null, tag)
            return result
        }

        /**
         * XMLをパースする（enclosureタグ）
         *
         * @param parser XMLパーサ
         * @return enclosureタグのurl属性文字列
         */
        @Throws(IOException::class, XmlPullParserException::class)
        private fun readEnclosure(parser: XmlPullParser): String {
            var url = ""
            parser.require(XmlPullParser.START_TAG, null, Tag.enclosure.value)
            val tag = parser.name
            val type = parser.getAttributeValue(null, EnclosureAttribute.type.value)
            if (tag == Tag.enclosure.value) {
                if (isImage(type)) {
                    url = parser.getAttributeValue(null, EnclosureAttribute.url.value)
                    parser.nextTag()
                }
            }
            parser.require(XmlPullParser.END_TAG, null, Tag.enclosure.value)
            return url
        }

        /**
         * 次のタグまでスキップ
         *
         * @param parser XMLパーサ
         */
        @Throws(XmlPullParserException::class, IOException::class)
        private fun skip(parser: XmlPullParser) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException()
            }
            var depth = 1
            while (depth != 0) {
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
        }

        /**
         * type属性から画像か判定する
         *
         * @param type type属性値
         * @return true=画像、 false=それ以外
         */
        private fun isImage(type: String): Boolean {
            return type.contains("image")
        }

    }

}