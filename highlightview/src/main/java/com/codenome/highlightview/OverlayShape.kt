package com.codenome.highlightview

import androidx.annotation.DimenRes

sealed class OverlayShape {
    data object OverlayEmpty : OverlayShape()
    data object OverlayRect : OverlayShape()
    data object OverlayOval : OverlayShape()

    class OverlayRoundRect private constructor(val radiusPair: Pair<Float, Float>? = null, val radiusResPair: Pair<Int, Int>? = null) :
        OverlayShape() {
        constructor(radiusX: Float, radiusY: Float) : this(radiusPair = Pair(radiusX, radiusY))
        constructor(@DimenRes radiusXRes: Int, @DimenRes radiusYRes: Int) : this(radiusResPair = Pair(radiusXRes, radiusYRes))
    }

    class OverlayCircle private constructor(
        public val radius: Float? = null,
        public val radiusRes: Int? = null,
    ) : OverlayShape() {
        public constructor(radius: Float) : this(radius, null)
        public constructor(@DimenRes radiusRes: Int) : this(null, radiusRes)
    }
}