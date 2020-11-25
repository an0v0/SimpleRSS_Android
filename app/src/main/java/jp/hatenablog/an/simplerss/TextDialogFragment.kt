package jp.hatenablog.an.simplerss

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * 入力ダイアログ
 *
 * @param titleId タイトル文字列リソースID
 * @param messageId メッセージ文字列リソースID
 * @param placeholder プレースホルダ文字列リソースID
 * @param positiveButtonTitleId ポジティブボタンタイトル文字列リソースID
 * @param negativeButtonTitleId ネガティブボタンタイトル文字列リソースID
 * @param positiveButtonListener ポジティブボタンリスナー
 * @param negativeButtonListener ネガティブボタンリスナー
 * @param cancelListener Cancelリスナー
 */
class TextDialogFragment(
    private val titleId: Int?,
    private val messageId: Int?,
    private val placeholder: String?,
    private val positiveButtonTitleId: Int?,
    private val negativeButtonTitleId: Int?,
    private val positiveButtonListener: ((String) -> Unit)?,
    private val negativeButtonListener: (() -> Unit)?,
    private val cancelListener: (() -> Unit)?
): DialogFragment() {

    private lateinit var editText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())

        titleId?.let { builder.setTitle(it) }
        messageId?.let { builder.setMessage(it) }

        val editText = EditText(requireContext())
        placeholder?.let { editText.setText(it) }
        builder.setView(editText)
        this.editText = editText

        positiveButtonTitleId?.let {
            builder.setPositiveButton(it) { _, _ ->
                positiveButtonListener?.invoke(editText.text.toString())
            }
        }
        negativeButtonTitleId?.let {
            builder.setNegativeButton(it) { _, _ ->
                negativeButtonListener?.invoke()
            }
        }
        cancelListener?.let {
            builder.setOnCancelListener { _ ->
                it.invoke()
            }
        }

        return builder.create()
    }

}