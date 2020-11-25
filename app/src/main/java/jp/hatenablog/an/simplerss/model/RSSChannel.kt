package jp.hatenablog.an.simplerss.model

import java.util.*
import kotlin.collections.ArrayList

/**
 * @param id 識別子
 * @param title タイトル
 * @param link リンク
 * @param items 記事の配列
 */
data class RSSChannel(val id: String = UUID.randomUUID().toString(),
                      var title: String? = null, var link: String? = null,
                      var items: ArrayList<RSSItem> = arrayListOf()) {
}