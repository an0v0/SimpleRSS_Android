package jp.hatenablog.an.simplerss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import jp.hatenablog.an.simplerss.model.RSSChannel
import kotlinx.android.synthetic.main.fragment_rss_list.swipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_rss_list.view.*
import java.lang.Exception

class RSSListFragment : Fragment(), RSSListAdapter.OnItemClickListener, RSSConnectorCallback<String> {

    private var rssChannel: RSSChannel? = null
    private lateinit var adapter: RSSListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rss_list, container, false)

        val rssListAdapter = RSSListAdapter(arrayListOf())
        rssListAdapter.listener = this
        adapter = rssListAdapter

        view.list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = rssListAdapter
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        view.swipeRefreshLayout?.setOnRefreshListener {
            refreshData()
            swipeRefreshLayout?.isRefreshing = false
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // RSSダウンロード開始
        refreshData()
    }

    /**
     * RSSダウンロード開始
     */
    private fun refreshData() {
        activity?.let {
            val connector = RSSConnector(it)
            if (connector.hasRssUrl()) {
                // RSSフィード登録済
                connector.callback = this
                connector.downloadRSSItems()
            }
            else {
                // RSSフィード未登録
                showInputDialog()
            }
        }
    }

    /**
     * RSSフィードデータを表示
     *
     * @param tmpRssChannel RSSフィードデータ
     */
    private fun setRssChannel(tmpRssChannel: RSSChannel?) {
        rssChannel = tmpRssChannel
        adapter.items = rssChannel?.items ?: arrayListOf()
        (activity as AppCompatActivity).supportActionBar?.title = rssChannel?.title
        adapter.notifyDataSetChanged()
    }

    /**
     * RSSフィードURL入力ダイアログ表示
     */
    fun showInputDialog() {
        activity?.let {
            TextDialogFragment(
                R.string.msg_input_rss_feed,
                null,
                RSSConnector(it).rssUrl,
                R.string.btn_submit,
                R.string.btn_cancel,
                { url ->
                    // OK時の処理
                    val connector = RSSConnector(it)
                    connector.rssUrl = url

                    if (url.isEmpty()) {
                        // リストをクリア
                        setRssChannel(null)
                    }
                    else {
                        // 登録されたRSSフィードを取得
                        connector.callback = this
                        connector.downloadRSSItems()
                    }
                },
                null,
                null
            ).apply {
                show(it.supportFragmentManager, "TextDialogFragment")
            }
        }
    }


    /////////////////////////////////////////////////
    //  OnItemClickListener
    /////////////////////////////////////////////////

    /**
     * Listの各行選択時
     *
     * @param view　選択された行のview
     * @param position 行番号
     */
    override fun onItemClickListener(view: View, position: Int) {
        val item = adapter.items[position]
        val intent = Intent(activity, RSSDetailActivity::class.java)
        val bundle = Bundle()
        bundle.putString(RSSDetailActivity.KEY_URL, item.link)
        intent.putExtras(bundle)

        startActivity(intent)
    }


    /////////////////////////////////////////////////
    //  DownloadCallback
    /////////////////////////////////////////////////

    /**
     * 通信失敗時
     *
     * @param connector RSS取得処理クラス
     * @param exception エラー情報
     */
    override fun didFailedWithError(connector: RSSConnector, exception: Exception?) {
        Log.i("RSSListFragment", "didFailedWithError: $exception")
    }

    /**
     * ダウンロード成功時
     *
     * @param connector RSS取得処理クラス
     */
    override fun connectorDidFinishDownloading(connector: RSSConnector) {
        setRssChannel(connector.rssChannel)
    }

}