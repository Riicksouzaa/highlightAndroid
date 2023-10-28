package com.codenome.highlightview.internal

import android.view.View
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@JvmSynthetic
internal fun <T : Any?> View.viewProperty(defaultValue: T): ViewPropertyDelegate<T> {
    return ViewPropertyDelegate(defaultValue) {
        invalidate()
    }
}

internal class ViewPropertyDelegate<T : Any?>(
    defaultValue: T,
    private val invalidator: () -> Unit,
) : ReadWriteProperty<Any?, T> {

    private var propertyValue: T = defaultValue

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return propertyValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (propertyValue != value) {
            propertyValue = value
            invalidator()
        }
    }
}