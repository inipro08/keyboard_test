@file:Suppress("DEPRECATION")

package com.inis.keyboard_test

import android.content.Context
import android.inputmethodservice.Keyboard

class Keyboard(context: Context?, xmlLayoutResId: Int) : Keyboard(context, xmlLayoutResId) {
    val layoutId = xmlLayoutResId
}