package dev.turboboo.azookey.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import dev.turboboo.azookey.core.FlickDirection
import dev.turboboo.azookey.core.FlickKey
import kotlin.math.max

/*
 * The all-direction cross and directional pentagon geometry are based on
 * azooKey's UnifiedFlickSuggestView.
 *
 * Upstream:
 *   azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 *   AzooKeyCore/Sources/KeyboardViews/View/UnifiedKey/Suggest/UnifiedFlickSuggestView.swift
 * Original implementation author: Keita Miwa (ensan) and azooKey contributors
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
internal class FlickSuggestionPopup(
    context: Context,
) {
    private val content = FlickSuggestionCanvasView(context)
    private val popup = PopupWindow(
        content,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        false,
    ).apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isTouchable = false
        isFocusable = false
        isOutsideTouchable = false
        isClippingEnabled = false
        elevation = 0f
    }

    fun update(
        anchor: View,
        key: FlickKey,
        state: FlickSuggestionState?,
    ) {
        if (state == null) {
            dismiss()
            return
        }
        if (anchor.width <= 0 || anchor.height <= 0 || !anchor.isAttachedToWindow) {
            return
        }

        val width = max(anchor.width * 3, 1)
        val height = max(anchor.height * 3, 1)
        content.bind(key, state)

        val xOffset = -anchor.width
        val yOffset = -(anchor.height * 2)

        if (popup.isShowing) {
            popup.update(
                anchor,
                xOffset,
                yOffset,
                width,
                height,
            )
        } else {
            popup.width = width
            popup.height = height
            popup.showAsDropDown(
                anchor,
                xOffset,
                yOffset,
            )
        }
    }

    fun dismiss() {
        if (popup.isShowing) {
            popup.dismiss()
        }
    }
}

private class FlickSuggestionCanvasView(
    context: Context,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private var key: FlickKey? = null
    private var state: FlickSuggestionState? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 20f * scaledDensity
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL,
        )
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun bind(
        key: FlickKey,
        state: FlickSuggestionState,
    ) {
        this.key = key
        this.state = state
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val key = key ?: return
        val state = state ?: return

        val cellWidth = width / 3f
        val cellHeight = height / 3f
        if (cellWidth <= 0f || cellHeight <= 0f) {
            return
        }

        val palette = AzooKeyViewStyle.palette(context)
        when (state) {
            FlickSuggestionState.All -> {
                fillPaint.clearShadowLayer()
                drawRoundedCell(
                    canvas = canvas,
                    rect = RectF(
                        cellWidth,
                        cellHeight,
                        cellWidth * 2f,
                        cellHeight * 2f,
                    ),
                    color = 0xFF007AFF.toInt(),
                    label = key.label,
                    textColor = Color.WHITE,
                )
                drawAllDirection(
                    canvas,
                    FlickDirection.TOP,
                    key.top,
                    cellWidth,
                    cellHeight,
                    palette.normalKey,
                    palette.text,
                )
                drawAllDirection(
                    canvas,
                    FlickDirection.LEFT,
                    key.left,
                    cellWidth,
                    cellHeight,
                    palette.normalKey,
                    palette.text,
                )
                drawAllDirection(
                    canvas,
                    FlickDirection.RIGHT,
                    key.right,
                    cellWidth,
                    cellHeight,
                    palette.normalKey,
                    palette.text,
                )
                drawAllDirection(
                    canvas,
                    FlickDirection.BOTTOM,
                    key.bottom,
                    cellWidth,
                    cellHeight,
                    palette.normalKey,
                    palette.text,
                )
            }

            is FlickSuggestionState.Direction -> {
                drawRoundedCell(
                    canvas = canvas,
                    rect = RectF(
                        cellWidth,
                        cellHeight,
                        cellWidth * 2f,
                        cellHeight * 2f,
                    ),
                    color = palette.specialKey,
                    label = key.label,
                    textColor = palette.text,
                )
                val label = key.output(state.direction) ?: return
                drawDirectionalPentagon(
                    canvas = canvas,
                    direction = state.direction,
                    label = label,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    fillColor = palette.normalKey,
                    textColor = palette.text,
                )
            }
        }
    }

    private fun drawAllDirection(
        canvas: Canvas,
        direction: FlickDirection,
        label: String?,
        cellWidth: Float,
        cellHeight: Float,
        fillColor: Int,
        textColor: Int,
    ) {
        if (label == null) {
            return
        }

        val rect = when (direction) {
            FlickDirection.TOP ->
                RectF(cellWidth, 0f, cellWidth * 2f, cellHeight)
            FlickDirection.LEFT ->
                RectF(0f, cellHeight, cellWidth, cellHeight * 2f)
            FlickDirection.RIGHT ->
                RectF(cellWidth * 2f, cellHeight, cellWidth * 3f, cellHeight * 2f)
            FlickDirection.BOTTOM ->
                RectF(cellWidth, cellHeight * 2f, cellWidth * 2f, cellHeight * 3f)
            FlickDirection.CENTER -> return
        }

        fillPaint.setShadowLayer(
            3f * density,
            0f,
            1f * density,
            0x66000000,
        )
        drawRoundedCell(
            canvas = canvas,
            rect = rect,
            color = fillColor,
            label = label,
            textColor = textColor,
        )
        fillPaint.clearShadowLayer()
    }

    private fun drawDirectionalPentagon(
        canvas: Canvas,
        direction: FlickDirection,
        label: String,
        cellWidth: Float,
        cellHeight: Float,
        fillColor: Int,
        textColor: Int,
    ) {
        val rect = when (direction) {
            FlickDirection.TOP ->
                RectF(
                    cellWidth * 0.9f,
                    0f,
                    cellWidth * 2.1f,
                    cellHeight * 1.5f,
                )
            FlickDirection.LEFT ->
                RectF(
                    0f,
                    cellHeight * 0.9f,
                    cellWidth * 1.5f,
                    cellHeight * 2.1f,
                )
            FlickDirection.RIGHT ->
                RectF(
                    cellWidth * 1.5f,
                    cellHeight * 0.9f,
                    cellWidth * 3f,
                    cellHeight * 2.1f,
                )
            FlickDirection.BOTTOM ->
                RectF(
                    cellWidth * 0.9f,
                    cellHeight * 1.5f,
                    cellWidth * 2.1f,
                    cellHeight * 3f,
                )
            FlickDirection.CENTER -> return
        }

        val path = pentagonPath(
            rect = rect,
            direction = direction,
        )

        fillPaint.color = fillColor
        fillPaint.setShadowLayer(
            10f * density,
            0f,
            5f * density,
            0x55000000,
        )
        canvas.drawPath(path, fillPaint)
        fillPaint.clearShadowLayer()

        drawLabel(
            canvas = canvas,
            label = label,
            x = rect.centerX(),
            y = rect.centerY(),
            color = textColor,
        )
    }

    private fun pentagonPath(
        rect: RectF,
        direction: FlickDirection,
    ): Path {
        val points = when (direction) {
            FlickDirection.TOP -> arrayOf(
                rect.centerX() to rect.bottom,
                rect.right to (rect.top + rect.height() * 2f / 3f),
                rect.right to rect.top,
                rect.left to rect.top,
                rect.left to (rect.top + rect.height() * 2f / 3f),
            )
            FlickDirection.LEFT -> arrayOf(
                rect.left to rect.top,
                (rect.left + rect.width() * 2f / 3f) to rect.top,
                rect.right to rect.centerY(),
                (rect.left + rect.width() * 2f / 3f) to rect.bottom,
                rect.left to rect.bottom,
            )
            FlickDirection.RIGHT -> arrayOf(
                (rect.left + rect.width() / 3f) to rect.top,
                rect.left to rect.centerY(),
                (rect.left + rect.width() / 3f) to rect.bottom,
                rect.right to rect.bottom,
                rect.right to rect.top,
            )
            FlickDirection.BOTTOM -> arrayOf(
                rect.centerX() to rect.top,
                rect.left to (rect.top + rect.height() / 3f),
                rect.left to rect.bottom,
                rect.right to rect.bottom,
                rect.right to (rect.top + rect.height() / 3f),
            )
            FlickDirection.CENTER -> emptyArray()
        }

        return Path().apply {
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
    }

    private fun drawRoundedCell(
        canvas: Canvas,
        rect: RectF,
        color: Int,
        label: String,
        textColor: Int,
    ) {
        fillPaint.color = color
        val radius = 5f * density
        canvas.drawRoundRect(
            rect,
            radius,
            radius,
            fillPaint,
        )
        drawLabel(
            canvas = canvas,
            label = label,
            x = rect.centerX(),
            y = rect.centerY(),
            color = textColor,
        )
    }

    private fun drawLabel(
        canvas: Canvas,
        label: String,
        x: Float,
        y: Float,
        color: Int,
    ) {
        textPaint.color = color
        val baseline =
            y - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(
            label,
            x,
            baseline,
            textPaint,
        )
    }
}
