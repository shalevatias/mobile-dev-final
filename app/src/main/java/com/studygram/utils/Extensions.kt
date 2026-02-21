package com.studygram.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun ImageView.loadImage(url: String?) {
    if (url.isNullOrEmpty()) return
    Picasso.get()
        .load(url)
        .into(this)
}

fun ImageView.loadImage(url: String?, placeholder: Int) {
    if (url.isNullOrEmpty()) {
        setImageResource(placeholder)
        return
    }
    Picasso.get()
        .load(url)
        .placeholder(placeholder)
        .error(placeholder)
        .into(this)
}

fun String?.isValidEmail(): Boolean {
    if (this == null) return false
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String?.isValidPassword(): Boolean {
    if (this == null) return false
    return this.length >= 6
}

/**
 * Hide the soft keyboard
 */
fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}
