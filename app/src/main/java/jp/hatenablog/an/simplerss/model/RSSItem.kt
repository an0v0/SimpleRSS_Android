package jp.hatenablog.an.simplerss.model

import java.util.*

/**
 * @param read 既読状態
 * @param title 記事のタイトル
 * @param link 記事のリンク
 * @param itemDescription 記事の詳細
 * @param pubDate 記事の日付
 * @param enclosureUrl サムネイル
 */
data class RSSItem(var read: Boolean = false, var title: String? = null, var link: String? = null,
                   var itemDescription: String? = null, var pubDate: String? = null,
                   var enclosureUrl: String? = null) {

    // 識別子
    val id: String = UUID.randomUUID().toString()

}