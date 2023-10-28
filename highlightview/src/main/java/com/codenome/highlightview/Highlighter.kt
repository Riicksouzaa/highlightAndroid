package com.codenome.highlightview

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.ColorInt

class Highlighter constructor(
    private val context: Context,
    private val highlighterConfig: HighlighterConfig,
    private val onDismissListener: (() -> Unit)? = null
) {
    private var popupWindow: PopupWindow? = null
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var tooltip: Tooltip = Tooltip(context, highlighterConfig.tooltipConfig)
    private var rectangle: Rect = Rect()

    fun drawHighlight(viewToHighlight: View, tooltipConfig: TooltipConfig) {
        viewToHighlight.post {
            draw(viewToHighlight, tooltipConfig)
        }
    }

    private fun draw(view: View, tooltipConfig: TooltipConfig) {
        val popupView = object : View(view.context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                canvas.drawColor(highlighterConfig.overlayColor)

                clearPaint.apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    style = Paint.Style.FILL
                }

                // Desenha o retângulo transparente
                view.getGlobalVisibleRect(rectangle)
                val rect =
                    RectF(
                        rectangle.left - highlighterConfig.overlayPadding,
                        rectangle.top - highlighterConfig.overlayPadding,
                        rectangle.right + highlighterConfig.overlayPadding,
                        rectangle.bottom + highlighterConfig.overlayPadding
                    )

                val halfOfOverlayPadding = highlighterConfig.overlayPadding / 2
                val anchorPaddingRect = RectF(rect).apply {
                    inset(halfOfOverlayPadding, halfOfOverlayPadding)
                }

                canvas.drawRoundRect(rect, highlighterConfig.overlayRadius, highlighterConfig.overlayRadius, clearPaint)
                canvas.drawRoundRect(anchorPaddingRect, highlighterConfig.overlayRadius - halfOfOverlayPadding, highlighterConfig.overlayRadius - halfOfOverlayPadding, clearPaint)

                // Desenha tooltip caso haja texto
                if (tooltipConfig.title.isNotEmpty()) {
                    tooltip.drawTooltip(rect, canvas)
                }
            }
        }

        // O highlight some quando clicado
        popupView.setOnClickListener {
            popupWindow?.dismiss()
            popupWindow = null
            onDismissListener?.invoke()
        }

        popupWindow?.dismiss()
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            false
        )
        popupWindow?.isClippingEnabled = false
        popupWindow?.animationStyle = R.style.Balloon_Fade_Anim

        popupWindow?.showAtLocation(view, Gravity.NO_GRAVITY, 0, 0)
    }


    private fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        val context = context
        return if (context is Activity) {
            context.window.decorView.getWindowVisibleDisplayFrame(rectangle)
            rectangle.top
        } else {
            0
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }
}

data class HighlighterConfig(
    @ColorInt
    var overlayColor: Int = Color.TRANSPARENT,
    var overlayRadius: Float = 16f,
    var overlayPadding: Float = 20f,
    var tooltipConfig: TooltipConfig
)

fun View.drawHighlight(context: Context, highlighterConfig: HighlighterConfig, dismissListener: (() -> Unit)? = null) {
    Highlighter(context, highlighterConfig) { dismissListener?.invoke() }.drawHighlight(this, highlighterConfig.tooltipConfig)
}

class Tooltip constructor(private val context: Context, private val tooltipConfig: TooltipConfig) {

    private val textPaint = Paint()
    private val balloonPaint = Paint()
    private val tooltipPath = Path()

    fun drawTooltip(rect: RectF, canvas: Canvas) {
        textPaint.apply {
            color = Color.WHITE
            textSize = tooltipConfig.textSize * context.resources.displayMetrics.scaledDensity
            isAntiAlias = true
        }

        val displayMetrics = Resources.getSystem().displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        val lines = tooltipConfig.title.split("\n")
        val textHeight = textPaint.textSize
        val maxWidth = lines.maxOf { textPaint.measureText(it) }
        val balloonWidth = maxWidth + 2 * tooltipConfig.textPadding
        val totalTextHeight = lines.size * textHeight + (lines.size - 1) * tooltipConfig.lineSpacing

        val balloonHeight = totalTextHeight + 2 * tooltipConfig.textPadding

        var balloonX = (rect.left + rect.width() / 2) - balloonWidth / 2
        var balloonY = rect.top - balloonHeight - tooltipConfig.textPadding - tooltipConfig.arrowHeight

        if (tooltipConfig.arrowDirection == null) {
            tooltipConfig.arrowDirection = when {
                balloonY - tooltipConfig.margin >= 0 -> {
                    ArrowDirection.TOP
                }

                balloonY + balloonHeight + rect.height() + 2 * tooltipConfig.textPadding + tooltipConfig.arrowHeight + tooltipConfig.margin <= screenHeight -> {
                    balloonY = rect.bottom + tooltipConfig.textPadding
                    ArrowDirection.BOTTOM
                }

                rect.right + balloonWidth + tooltipConfig.textPadding + tooltipConfig.margin <= screenWidth -> {
                    balloonX = rect.right + tooltipConfig.textPadding
                    balloonY = rect.top + (rect.height() - balloonHeight) / 2
                    ArrowDirection.RIGHT
                }

                rect.left - balloonWidth - tooltipConfig.textPadding - tooltipConfig.margin >= 0 -> {
                    balloonX = rect.left - balloonWidth - tooltipConfig.textPadding
                    balloonY = rect.top + (rect.height() - balloonHeight) / 2
                    ArrowDirection.LEFT
                }

                else -> ArrowDirection.TOP
            }
        }

        balloonX = balloonX.coerceAtLeast(tooltipConfig.margin)
            .coerceAtMost(screenWidth - balloonWidth - tooltipConfig.margin)
        balloonY = balloonY.coerceAtLeast(tooltipConfig.margin)
            .coerceAtMost(screenHeight - balloonHeight - tooltipConfig.margin)

        balloonPaint.apply {
            color = tooltipConfig.backgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val rectF = RectF(balloonX, balloonY, balloonX + balloonWidth, balloonY + balloonHeight)
        canvas.drawRoundRect(rectF, tooltipConfig.cornerRadius, tooltipConfig.cornerRadius, balloonPaint)

        makePath(rect, balloonY, balloonHeight, balloonX, balloonWidth)
        canvas.drawPath(tooltipPath, balloonPaint)

        drawText(balloonY, lines, balloonX, balloonWidth, canvas, textHeight)
    }

    private fun drawText(
        balloonY: Float,
        lines: List<String>,
        balloonX: Float,
        balloonWidth: Float,
        canvas: Canvas,
        textHeight: Float
    ) {
        val textStartY = balloonY + tooltipConfig.textPadding - textPaint.ascent()
        var currentTextY = textStartY
        for (line in lines) {
            val textWidth = textPaint.measureText(line)
            val textX = balloonX + (balloonWidth - textWidth) / 2
            canvas.drawText(line, textX, currentTextY, textPaint)
            currentTextY += textHeight + tooltipConfig.lineSpacing
        }
    }

    private fun makePath(
        rect: RectF,
        balloonY: Float,
        balloonHeight: Float,
        balloonX: Float,
        balloonWidth: Float
    ) {
        tooltipConfig.arrowDirection?.let { arrowDirection ->
            when (arrowDirection) {
                ArrowDirection.TOP -> {
                    tooltipPath.moveTo(
                        (rect.left + rect.width() / 2) - tooltipConfig.arrowWidth / 2,
                        balloonY + balloonHeight
                    )
                    tooltipPath.lineTo(
                        rect.left + rect.width() / 2f,
                        balloonY + balloonHeight + tooltipConfig.arrowHeight
                    )
                    tooltipPath.lineTo(
                        (rect.left + rect.width() / 2) + tooltipConfig.arrowWidth / 2,
                        balloonY + balloonHeight
                    )
                    tooltipPath.close()
                }

                ArrowDirection.BOTTOM -> {
                    tooltipPath.moveTo((rect.left + rect.width() / 2) - tooltipConfig.arrowWidth / 2, balloonY)
                    tooltipPath.lineTo(rect.left + rect.width() / 2f, balloonY - tooltipConfig.arrowHeight)
                    tooltipPath.lineTo((rect.left + rect.width() / 2) + tooltipConfig.arrowWidth / 2, balloonY)
                    tooltipPath.close()
                }

                ArrowDirection.LEFT -> {
                    tooltipPath.moveTo(
                        balloonX + balloonWidth,
                        (rect.top + rect.height() / 2) - tooltipConfig.arrowWidth / 2
                    )
                    tooltipPath.lineTo(
                        balloonX + balloonWidth + tooltipConfig.arrowHeight,
                        rect.top + rect.height() / 2f
                    )
                    tooltipPath.lineTo(
                        balloonX + balloonWidth,
                        (rect.top + rect.height() / 2) + tooltipConfig.arrowWidth / 2
                    )
                    tooltipPath.close()
                }

                ArrowDirection.RIGHT -> {
                    tooltipPath.moveTo(balloonX, (rect.top + rect.height() / 2) - tooltipConfig.arrowWidth / 2)
                    tooltipPath.lineTo(balloonX - tooltipConfig.arrowHeight, rect.top + rect.height() / 2f)
                    tooltipPath.lineTo(balloonX, (rect.top + rect.height() / 2) + tooltipConfig.arrowWidth / 2)
                    tooltipPath.close()
                }
            }
        }

    }
}

data class TooltipConfig(
    var title: String,
    @ColorInt var backgroundColor: Int = Color.BLACK,
    var textSize: Float = 12f,
    var textPadding: Float = 20f,
    var lineSpacing: Float = 5f,
    var cornerRadius: Float = 15f,
    var margin: Float = 10f,
    var arrowDirection: ArrowDirection? = null,
    var arrowWidth: Float = 20f,
    var arrowHeight: Float = 15f
)

enum class ArrowDirection {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}