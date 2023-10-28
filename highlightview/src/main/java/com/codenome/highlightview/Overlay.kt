package com.codenome.highlightview

import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.DefaultLifecycleObserver
import com.codenome.highlightview.databinding.LayoutOverlayBinding

class Overlay private constructor(private val context: Context) : DefaultLifecycleObserver {
    private val overlayBinding: LayoutOverlayBinding = LayoutOverlayBinding.inflate(LayoutInflater.from(context), null, false)
}