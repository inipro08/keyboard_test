package com.inis.keyboard_test

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.preference.PreferenceManager
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.TextView
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class InputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private lateinit var customInputMethodView: ViewGroup
    private lateinit var deq: ArrayList<com.inis.keyboard_test.Keyboard>
    private lateinit var symKeyboard: com.inis.keyboard_test.Keyboard
    private var keyboardView: com.inis.keyboard_test.KeyboardView? = null
    private val mComposing = StringBuilder()

    private var textPreview: TextView? = null
    private var textPreviewEnd: TextView? = null
    private var mCapsLock: Boolean = true
    private val mENTER: Int = 66
    private var previousLength = 0
    private val context = this


    override fun onCreate() {
        super.onCreate()
        deq = ArrayList()
        symKeyboard = com.inis.keyboard_test.Keyboard(this, R.xml.sym_pad)

        updateLanguagesListFromPreferences() //load languages from settings to deq

        toggleCapsLock() //lowercase by default
    }


    override fun onCreateInputView(): View? {
        Log.d("FUNCALL", "onCreateInputView")
        customInputMethodView = layoutInflater.inflate(R.layout.keyboard_view, null) as ViewGroup
        textPreview = customInputMethodView.findViewById(R.id.text)
        keyboardView = customInputMethodView.findViewById(R.id.keyboard_view)
        keyboardView?.let {
            it.keyboard = deq[0]
            it.isPreviewEnabled = false
            it.setOnKeyboardActionListener(this)
        }
        updateShiftKeyState(currentInputEditorInfo)

        updateComposing()
        return customInputMethodView
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        Log.d("FUNCALL", "onStartInputView")
        super.onStartInputView(attribute, restarting)

        updateLanguagesListFromPreferences()
        refreshLanguage()
        updateComposing()
    }

    override fun onPress(p0: Int) {
    }

    override fun onRelease(p0: Int) {
    }

    override fun swipeLeft() {
        currentInputConnection?.let {
            val et = it.getExtractedText(ExtractedTextRequest(), 0)
            val selectionStart = et.selectionStart
            it.setSelection(selectionStart - 1, selectionStart - 1)

            updateComposing()
        }
    }

    override fun swipeRight() {
        currentInputConnection?.let {

            if (it.getTextAfterCursor(1000, 0).isEmpty()) {
                handleCharacter(32)//Space
            } else {
                //+1 selection
                val et = it.getExtractedText(ExtractedTextRequest(), 0)
                val selectionStart = et.selectionStart
                it.setSelection(selectionStart + 1, selectionStart + 1)
            }

            updateComposing()
        }
    }

    override fun swipeUp() {
        if (keyboardView?.keyboard == symKeyboard) {
            refreshLanguage()
        } else {
            keyboardView?.keyboard = symKeyboard
        }
    }

    override fun swipeDown() {
        handleClose()
    }

    private fun toggleCapsLock() {
        mCapsLock = !mCapsLock
        updateShiftKeyState(currentInputEditorInfo)
    }

    private fun handleClose() {
        requestHideSelf(0)
        keyboardView!!.closing()
    }

    override fun onFinishInput() {
        super.onFinishInput()

        // Clear current composing text and candidates.
        mComposing.setLength(0)
        previousLength = 0

        keyboardView?.closing()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        // If the current selection in the text view changes, we should
        // clear whatever textPreview text we have.
        if ((newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            updateComposing()
            val ic = currentInputConnection
            ic?.finishComposingText()
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                handleBackspace()
            }
            Keyboard.KEYCODE_SHIFT -> {
                toggleCapsLock()
            }
            Keyboard.KEYCODE_MODE_CHANGE -> {
                toggleLanguage()
            }
            else -> {
                handleCharacter(primaryCode)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onText(text: CharSequence) {
        var s = text.toString()

        if (isInputViewShown) {
            if (keyboardView?.isShifted == true) {
                s = s.toUpperCase()
            }
        }

        mComposing.append(s)
        currentInputConnection.commitText(s, 1)

        previousLength = mComposing.length
    }

    private fun updateComposing() {
        mComposing.setLength(0) //clear it
        var end: CharSequence = ""
        currentInputConnection?.let {
            val before: CharSequence? = it.getTextBeforeCursor(1000, 0)
            if (before != null) {
                mComposing.append(before)
            }
            val after: CharSequence? = it.getTextAfterCursor(1000, 0)
            if (after != null) {
                end = after
            }

        }

        textPreview?.text = mComposing

        textPreviewEnd?.let {
            it.text = end

            if (end.isNotEmpty()) {
                it.visibility = View.VISIBLE

            } else {
                it.visibility = View.GONE

            }
        }
    }

    private fun handleCharacter(primaryCode: Int) {
        if (mComposing.length != previousLength) {
            mCapsLock = false
            updateShiftKeyState(currentInputEditorInfo)
        }

        var pc = primaryCode
        if (isInputViewShown) {
            if (keyboardView?.isShifted == true) {
                pc = Character.toUpperCase(primaryCode)
            }
        }

        val code = pc.toChar()
        mComposing.append(code)
        currentInputConnection.commitText(code.toString(), 1)

        previousLength = mComposing.length
    }

    private fun handleBackspace() {
        val length = mComposing.length
        if (length > 0) {
            mComposing.delete(length - 1, length)
            textPreview?.text = mComposing
            previousLength = mComposing.length

            val selectedText: CharSequence? = currentInputConnection.getSelectedText(0)
            if (selectedText == null) {
                currentInputConnection.deleteSurroundingText(1, 0)
            } else {
                if (selectedText.isEmpty()) {
                    currentInputConnection.deleteSurroundingText(1, 0)
                } else {
                    currentInputConnection.commitText("", 1)
                }
            }

        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL)
        }
        updateShiftKeyState(currentInputEditorInfo)
        updateComposing()
    }

    private fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode)
        )
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, keyEventCode)
        )
    }

    private fun updateShiftKeyState(attr: EditorInfo?) {
        if (attr != null && keyboardView != null) {
            var caps = 0
            val editorInfo = currentInputEditorInfo
            if (editorInfo != null && editorInfo.inputType != InputType.TYPE_NULL) {
                caps = currentInputConnection.getCursorCapsMode(attr.inputType)
            }
            keyboardView?.isShifted = mCapsLock || caps != 0
        }
    }

    private fun updateLanguagesListFromPreferences() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val keyMapsRev: HashMap<String, Int> = HashMap()
        MainActivity().keyMaps.forEach { keyMapsRev[it.value] = it.key }

        val prf = preferences.getString("languages", null)
        val setLangs = if (prf != null) JSONObject(prf) else JSONObject(keyMapsRev as Map<*, *>)
        Log.d("set langs", setLangs.toString())
        Log.d("key maps", keyMapsRev.toString())

        keyMapsRev.forEach {
            if (setLangs.has(it.key)) { // if enabled languages list has such language enabled
                if (!deq.any { lt -> lt.layoutId == it.value }) {
                    // if language keyboard with such
                    // layout isn't in list - add it
                    deq.add(com.inis.keyboard_test.Keyboard(this, it.value))
                }
            } else { // disabled, search for it and delete
                for (kb in deq) {
                    if (kb.layoutId == it.value || kb.layoutId !in MainActivity().keyMaps) {
                        deq.remove(kb)
                        break
                    }
                }
            }

        }
        Log.d("deq maps", deq.toString())
    }

    private fun refreshLanguage() {
        // set keyboard to last available from deq (for example if language was disabled it sets next to it)
        Log.d("REFRESH", deq.toString())
        keyboardView?.keyboard = deq[0]
    }

    private fun toggleLanguage() {
        updateLanguagesListFromPreferences()
        keyboardView?.let {
            Collections.rotate(deq, 1)
            refreshLanguage()
            updateShiftKeyState(currentInputEditorInfo)
        }
    }
}