package jp.hatenablog.an.simplerss

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_rss_list.*
import kotlinx.android.synthetic.main.content_rss_list.*

class RSSListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // superメソッドを呼ぶ前にThemeを設定する
        setTheme(R.style.AppTheme_NoActionBar)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rss_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            (rss_list_fragment as RSSListFragment).showInputDialog()
        }
    }

}
