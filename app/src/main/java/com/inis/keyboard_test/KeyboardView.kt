@file:Suppress("DEPRECATION")

package com.inis.keyboard_test

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet

@Suppress("DEPRECATION")
class KeyboardView(context: Context?, attrs: AttributeSet?) : KeyboardView(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sb = StringBuilder()

    private val globeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_language_64)
    private val shiftFilledIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_shift_filled)

    init {
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.WHITE
        paint.strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(resources.getColor(R.color.background))
        val npd = context.resources.getDrawable(R.drawable.key_selector, null) as StateListDrawable

        val keys = keyboard.keys
        for (key in keys) {
            npd.setBounds(key.x, key.y, key.x + key.width, key.y + key.height)
            val drawableState = key.currentDrawableState
            npd.state = drawableState
            npd.draw(canvas)

            paint.textSize = 60f

            var x0 = (key.x + key.width / 2).toFloat()

            if (key.edgeFlags and Keyboard.EDGE_LEFT == Keyboard.EDGE_LEFT) {
                x0 += 10
            }
            if (key.edgeFlags and Keyboard.EDGE_RIGHT == Keyboard.EDGE_RIGHT) {
                x0 -= 10
            }

            if (key.label != null) {
                canvas.drawText(
                    key.label.toString(),
                    x0 - 80,
                    (key.y + key.height - 40).toFloat(), paint
                )
                var s: String
                sb.setLength(0)
                for (code in key.codes) {
                    if (code > 0 && (code < 48 || code > 57) && code != 32) {
                        sb.append(code.toChar())
                    }
                }
                s = sb.toString()

                if (s.isNotEmpty()) {
                    if (isShifted) {
                        s = s.toUpperCase()
                    }
                    paint.textSize = 45f
                    canvas.drawText(
                        s,
                        x0 - 30,
                        (key.y + key.height - 47).toFloat(), paint
                    )
                }
            } else {
                if (key.icon is BitmapDrawable) {
                    val bitmap: Bitmap = if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {
                        if (isShifted) {
                            shiftFilledIcon
                        } else {
                            (key.icon as BitmapDrawable).bitmap
                        }
                    } else {
                        (key.icon as BitmapDrawable).bitmap
                    }
                    var x1 = (key.x + key.width / 2 - bitmap.width / 2).toFloat()
                    var y1 = (key.y + key.height / 2 - bitmap.height / 2).toFloat()

                    if (key.edgeFlags and Keyboard.EDGE_LEFT == Keyboard.EDGE_LEFT) {
                        x1 += 10
                    }
                    if (key.edgeFlags and Keyboard.EDGE_RIGHT == Keyboard.EDGE_RIGHT) {
                        x1 -= 15
                        y1 -= 10
                    }

                    canvas.drawBitmap(bitmap, x1 - 25, y1 + 25, null)
                    key.icon.setBounds(
                        key.x + key.width,
                        key.y + key.height,
                        key.x + key.width / 2,
                        key.y + key.height / 2
                    )
                    key.icon.draw(canvas)

                    if (key.codes[0] == Keyboard.KEYCODE_SHIFT) {

                        val x2 = (key.x + key.width / 2 - bitmap.width / 2).toFloat()
                        val y2 = (key.y + key.height / 2 - bitmap.height / 2).toFloat()
                        canvas.drawBitmap(globeIcon, x2 + 25, y2 + 20, null)
                    }
                }
            }
        }
    }

    override fun onLongPress(popupKey: Keyboard.Key?): Boolean {
        if ((popupKey?.codes?.get(0) ?: -1) == Keyboard.KEYCODE_SHIFT) {
            onKeyboardActionListener.onKey(Keyboard.KEYCODE_MODE_CHANGE, null)
            return true
        }
        // if key is special code or space(one without label) then act like long press
        else if ((popupKey?.codes?.get(0)
                ?: -1) < 0 || (popupKey?.codes?.get(0) == 32 && popupKey.label == null)
        ) {
            return super.onLongPress(popupKey)
        } else if (popupKey != null) {
            onKeyboardActionListener.onText(popupKey.label)
        }
        return true
    }
}