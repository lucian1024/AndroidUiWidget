/**
 * @description: One time password widget, which is composed by a group of EditText views,
 * usually used for input verification code, etc.
 *
 * @author: Lucian
 * @date: 2019/12/29
 */
package com.lucian.otpview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout

class OtpView(context: Context, attr: AttributeSet) : ViewGroup(context, attr) {
    //number of verify code
    private val editNum: Int
    // width of verify code box with unit dp
    private val editWidth: Int
    // width of verify code box with unit dp
    private val editHeight: Int
    // the space between verify code box with unit dp
    private var editSpace: Int
    // verify code input type
    private var editInputType: String?
    // text size of verify code with sp unit
    private val editTextSize: Int
    // text color of verify code with
    private val editTextColor: Int
    // text font family of verify code
    private val editFontFamily: String?
    // background of verify code box for normal state
    private val editNormalBg: Drawable?
    // background of EditText for input correct state
    private val editSuccessBg: Drawable?
    // background of EditText for input incorrect state
    private val editErrorBg: Drawable?
    // the verify code box views
    private val editTexts: ArrayList<EditText>
    // the index of current focused verify code box
    private var curPosition: Int
    // current background state of verify code box
    private var curState: Int
    // the callback when the input is complete
    private var onCompleteListener: OnCompleteListener? = null

    init {
        val typedArray = context.obtainStyledAttributes(attr, R.styleable.OtpView)
        editNum = typedArray.getInt(R.styleable.OtpView_edit_num, 6)
        editWidth = typedArray.getDimension(R.styleable.OtpView_edit_width, 80f).toInt()
        editHeight = typedArray.getDimension(R.styleable.OtpView_edit_height, 80f).toInt()
        editSpace = typedArray.getDimension(R.styleable.OtpView_edit_space, 0f).toInt()
        editInputType = typedArray.getString(R.styleable.OtpView_edit_inputType)
        editTextSize = typedArray.getDimension(R.styleable.OtpView_edit_textSize, 24f).toInt()
        editTextColor = typedArray.getColor(R.styleable.OtpView_edit_textColor, Color.BLACK)
        editFontFamily = typedArray.getString(R.styleable.OtpView_edit_fontFamily)
        editNormalBg = typedArray.getDrawable(R.styleable.OtpView_edit_normalBg)
        editSuccessBg = typedArray.getDrawable(R.styleable.OtpView_edit_successBg)
        editErrorBg = typedArray.getDrawable(R.styleable.OtpView_edit_errorBg)
        typedArray.recycle()

        //default for inputting number
        if (editInputType == null) {
            editInputType = NUMBER_INPUT_TYPE
        }
        curState = NORMAL_STATE
        editTexts = ArrayList()
        curPosition = 0

        initView()
    }

    /**
     * @description init the view of OtpView
     *
     * @param
     *
     * @return
     */
    private fun initView() {
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    //current verify code box has been inputted, move focus to next and check Complete
                    forwardFocus()
                }
                checkComplete()
            }
        }

        val onKeyListener = OnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            //KEYCODE_DEL to delete input and back focus to previous verify code box
            if (KeyEvent.KEYCODE_DEL == keyCode && event.action == KeyEvent.ACTION_DOWN) {
                backFocus()
            }
            false
        }

        val onReplaceListener = object : OtpEditTextSpanBuilder.OnReplaceListener {
            override fun onReplace(s: CharSequence) {
                onVerifyCodeBoxPaste(s)
            }
        }

        for (index in 0 until editNum) {
            val editText = EditText(context)
            val layoutParams = LinearLayout.LayoutParams(editWidth, editHeight)
            layoutParams.gravity = Gravity.CENTER
            editText.layoutParams = layoutParams
            if (PASS_INPUT_TYPE == editInputType) {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
            } else if (TEXT_INPUT_TYPE == editInputType) {
                editText.inputType = InputType.TYPE_CLASS_TEXT
            } else if (PHONE_INPUT_TYPE == editInputType) {
                editText.inputType = InputType.TYPE_CLASS_PHONE
            } else {
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextSize.toFloat())
            editText.setTextColor(editTextColor)
            editText.typeface = Typeface.create(editFontFamily, Typeface.NORMAL)
            editNormalBg?.let {
                editText.background = it
            }
            editText.gravity = Gravity.CENTER
            //set the max length of the EditText to 1
            editText.filters = arrayOf<InputFilter>(LengthFilter(1))
            editText.addTextChangedListener(textWatcher)
            editText.setOnKeyListener(onKeyListener)
            editText.setEditableFactory(OtpEditableFactory(onReplaceListener))

            editText.isEnabled = index == 0
            editTexts.add(editText)
            addView(editText, index)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val layoutParams = layoutParams
        for (index in 0 until editNum) {
            measureChild(editTexts[index], widthMeasureSpec, heightMeasureSpec)
        }
        if (editNum > 0) {
            val editWidth = editTexts[0].measuredWidth
            if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                //layout_width is not wrap_content, reset editSpace to adapt parent width
                val layoutWidth = measuredWidth
                editSpace = (layoutWidth - editWidth * editNum) / (editNum - 1)
            }
            val editHeight = editTexts[0].measuredHeight
            val maxWidth = editWidth * editNum + editSpace * (editNum - 1)
            val maxHeight = editHeight
            setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                    resolveSize(maxHeight, heightMeasureSpec))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (index in 0 until editNum) {
            val measuredWidth = editTexts[index].measuredWidth
            val measuredHeight = editTexts[index].measuredHeight
            val left = index * (measuredWidth + editSpace)
            val right = left + measuredWidth
            val top = 0
            val bottom = top + measuredHeight
            editTexts[index].layout(left, top, right, bottom)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_UP) {
            if (curState == ERROR_STATE) {
                reset()
            }
            val curEditText = editTexts[curPosition]
            curEditText.requestFocus()
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(curEditText, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * @description move focus to next verify code box
     *
     * @param
     *
     * @return
     */
    private fun forwardFocus() {
        if (curPosition < editNum - 1) {
            curPosition += 1
            editTexts[curPosition].setEnabled(true)
            editTexts[curPosition].requestFocus()
            editTexts[curPosition - 1].setEnabled(false)
        }
    }

    /**
     * @description clear the last inputted verify code box and move focus to it
     *
     * @param
     *
     * @return
     */
    private fun backFocus() {
        if (curPosition > 0) {
            val curEditText = editTexts[curPosition]
            if (curEditText.getText()?.isEmpty() == true) {
                val preEditText = editTexts[curPosition - 1]
                preEditText.setEnabled(true)
                preEditText.setText("")
                preEditText.requestFocus()
                curEditText.setEnabled(false)
                curPosition -= 1
            } else {
                curEditText.setText("")
            }
        }
    }

    fun reset() {
        for (index in 0 until editNum) {
            val editText = editTexts[index]
            editText.setText("")
            editText.isEnabled = index == 0
            editNormalBg?.let {
                editText.background = it
            }
        }
        curPosition = 0
        curState = NORMAL_STATE
    }

    /**
     * @description check whether the verify code box have been all inputted or not.
     *
     * @param
     *
     * @return
     */
    private fun checkComplete() {
        //current verify code box is the last one and has been inputted, then it's completed
        if (curPosition == editNum - 1 && (editTexts[curPosition].getText()?.isNotEmpty() == true)) {
            val stringBuilder = StringBuilder()
            for (editText in editTexts) {
                stringBuilder.append(editText.getText().toString())
            }
            if (onCompleteListener != null) {
                onCompleteListener!!.onComplete(stringBuilder.toString())
            }
        } else {
            setState(NORMAL_STATE)
        }
    }

    /**
     * @description set the background of the verify code box for normal/success/error state
     *
     * @param state: NORMAL_STATE/SUCCESS_STATE/ERROR_STATE
     *
     * @return
     */
    fun setState(state: Int) {
        if (curState == state) {
            return
        }
        when (state) {
            NORMAL_STATE -> {
                if (setEditTextBg(editNormalBg)) {
                    curState = NORMAL_STATE
                }
            }

            SUCCESS_STATE -> {
                if (setEditTextBg(editSuccessBg)) {
                    curState = SUCCESS_STATE
                }
            }

            ERROR_STATE -> {
                if (setEditTextBg(editErrorBg)) {
                    curState = ERROR_STATE
                }
            }

            else -> {}
        }
    }

    /**
     * @description set the background of the verify code box
     *
     * @param bg the background
     *
     * @return true for success; false for fail
     */
    private fun setEditTextBg(bg: Drawable?): Boolean {
        if (bg == null) {
            return false
        }
        for (editText in editTexts) {
            editText.background = bg
        }
        return true
    }

    /**
     * @description: callback when some content is pasted to the verify box
     *
     * @param s: pasted content
     *
     */
    private fun onVerifyCodeBoxPaste(s: CharSequence) {
        if (s.isEmpty()) {
            return
        }

        // the first one is pasted by system
        val pastedIndex = curPosition - 1
        for (index in pastedIndex until editNum) {
            val textIndex = index - pastedIndex
            if (textIndex < s.length) {
                val editText = editTexts[index]
                editText.setText(s.subSequence(textIndex, textIndex + 1))
                editText.setSelection(1)
            } else {
                break
            }
        }
    }

    /**
     * @description: listener for notify completion
     */
    fun interface OnCompleteListener {
        fun onComplete(content: String?)
    }


    /**
     * @description: set complete listener
     *
     * @param onCompleteListener the complete listener
     */
    fun setOnCompleteListener(onCompleteListener: OnCompleteListener?) {
        this.onCompleteListener = onCompleteListener
    }

    companion object {
        const val NORMAL_STATE = 0
        const val SUCCESS_STATE = 1
        const val ERROR_STATE = 2
        private const val NUMBER_INPUT_TYPE = "number"
        private const val PASS_INPUT_TYPE = "password"
        private const val TEXT_INPUT_TYPE = "text"
        private const val PHONE_INPUT_TYPE = "phone"
    }

    class OtpEditableFactory(
        private val onReplaceListener: OtpEditTextSpanBuilder.OnReplaceListener?
    ) : Editable.Factory() {
        override fun newEditable(source: CharSequence): Editable {
            val builder = if (source is OtpEditTextSpanBuilder) {
                source
            } else {
                OtpEditTextSpanBuilder(source)
            }
            builder.onReplaceListener = onReplaceListener
            return builder
        }
    }

    class OtpEditTextSpanBuilder(text: CharSequence): SpannableStringBuilder(text) {
        var onReplaceListener: OnReplaceListener? = null

        interface OnReplaceListener {
            fun onReplace(s: CharSequence)
        }

        override fun replace(
            start: Int,
            end: Int,
            tb: CharSequence?,
            tbstart: Int,
            tbend: Int
        ): SpannableStringBuilder {
            val result = super.replace(start, end, tb, tbstart, tbend)
            onReplaceListener?.let {
                if (tb != null && tbend - tbstart > 1) {
                    it.onReplace(tb.subSequence(tbstart, tbend))
                }
            }
            return result
        }
    }
}
