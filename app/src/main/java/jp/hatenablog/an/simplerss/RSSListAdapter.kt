package jp.hatenablog.an.simplerss

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import jp.hatenablog.an.simplerss.model.RSSItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class RSSListAdapter(
    var items: ArrayList<RSSItem>
) : RecyclerView.Adapter<RSSListAdapter.ViewHolder>() {

    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.rss_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleTextView.text = item.title?.let { trim(it) } ?: ""
        holder.contentsTextView.text = item.itemDescription?.let { trim(it) } ?: ""
        holder.dateTextView.text = item.pubDate?.let { formatPubDate(trim(it)) } ?: ""
        item.enclosureUrl?.let { setImageFromUrl(it, holder.thumbnailView) }

        holder.itemView.setOnClickListener {
            listener?.onItemClickListener(it, position)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * 文字列から前後の余白を取り除く
     *
     * @param string 処理対象文字列
     * @return 前後の余白を取り除いた文字列
     */
    private fun trim(string: String): String {
        return string.trim()
    }

    /**
     * 日付のフォーマットを表示用に変換
     *
     * @param string 処理対象文字列
     * @return 表示用にフォーマット変換した日付文字列（変換失敗した場合は元の文字列を返す）
     */
    private fun formatPubDate(string: String): String {
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
        val date = formatter.parse(string)
        val newFormatter = SimpleDateFormat("yyyy.MM.dd(EEE) HH:mm", Locale.JAPAN)
        return date?.let { newFormatter.format(it) } ?: string
    }

    /**
     * URLから取得した画像をイメージビューに設定する
     *
     * @param urlString URL文字列
     * @param imageView イメージビュー
     */
    private fun setImageFromUrl(urlString: String, imageView: ImageView) = GlobalScope.launch(Dispatchers.Main) {
        withContext(Dispatchers.IO) { getBitmapFromUrl(urlString) }?.let {
            imageView.setImageBitmap(it)
        }
    }

    /**
     * URLからBitmapを取得する
     *
     * @param urlString URL文字列
     * @return Bitmap
     */
    private fun getBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            url.openConnection().run {
                doInput = true
                connect()
                return BitmapFactory.decodeStream(getInputStream())
            }
        }
        catch (error: IOException) {
            error.printStackTrace()
            null
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
        val contentsTextView: TextView = view.findViewById(R.id.contentsTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val thumbnailView: ImageView = view.findViewById(R.id.thumbnailView)
    }

    interface OnItemClickListener {
        fun onItemClickListener(view: View, position: Int)
    }
}