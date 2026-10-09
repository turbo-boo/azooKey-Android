package dev.turboboo.azookey.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/*
 * Expanded candidate rows mirror azooKey's ExpandedResultView: candidates
 * wrap by measured text width and rows are separated by a subtle divider.
 *
 * Upstream:
 *   azooKey/azooKey@b50db4aec1069a8d2341f70415da3bdc61f1fce6
 *   AzooKeyCore/Sources/KeyboardViews/View/Components/ExpandedResultView.swift
 * Original implementation author: Keita Miwa (ensan)
 *
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */
internal class CandidateFlowLayout(
    context: Context,
) : ViewGroup(context) {
    private val density = resources.displayMetrics.density
    private val horizontalSpacing = dp(10f)
    private val verticalSpacing = dp(3f)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = max(1f, density)
    }
    private val dividerYs = mutableListOf<Float>()

    init {
        refreshStyle()
        setWillNotDraw(false)
        setPadding(
            dp(15f),
            dp(3f),
            dp(10f),
            dp(3f),
        )
    }

    fun refreshStyle() {
        dividerPaint.color =
            if (
                AzooKeyViewStyle.palette(context).text ==
                AzooKeyVisualDesign.DARK.text
            ) {
                0x33FFFFFF
            } else {
                0x33000000
            }
        invalidate()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val availableWidth =
            View.MeasureSpec.getSize(widthMeasureSpec) -
                paddingLeft -
                paddingRight

        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = paddingTop + paddingBottom

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) {
                continue
            }

            measureChild(
                child,
                widthMeasureSpec,
                heightMeasureSpec,
            )
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            val requiredWidth =
                if (lineWidth == 0) childWidth else lineWidth + horizontalSpacing + childWidth
            if (lineWidth != 0 && requiredWidth > availableWidth) {
                totalHeight += lineHeight + verticalSpacing
                lineWidth = childWidth
                lineHeight = childHeight
            } else {
                lineWidth = requiredWidth
                lineHeight = max(lineHeight, childHeight)
            }
        }

        if (lineHeight > 0) {
            totalHeight += lineHeight
        }

        setMeasuredDimension(
            View.resolveSize(
                View.MeasureSpec.getSize(widthMeasureSpec),
                widthMeasureSpec,
            ),
            View.resolveSize(
                totalHeight,
                heightMeasureSpec,
            ),
        )
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        val availableWidth = right - left - paddingLeft - paddingRight
        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0
        dividerYs.clear()

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) {
                continue
            }

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            val nextRight = x + childWidth

            if (x > paddingLeft && nextRight > paddingLeft + availableWidth) {
                y += lineHeight + verticalSpacing
                dividerYs += y - verticalSpacing / 2f
                x = paddingLeft
                lineHeight = 0
            }

            child.layout(
                x,
                y,
                x + childWidth,
                y + childHeight,
            )
            x += childWidth + horizontalSpacing
            lineHeight = max(lineHeight, childHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dividerYs.forEach { y ->
            canvas.drawLine(
                paddingLeft.toFloat(),
                y,
                (width - paddingRight).toFloat(),
                y,
                dividerPaint,
            )
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )

    override fun generateLayoutParams(
        params: LayoutParams?,
    ): LayoutParams =
        params?.let(::LayoutParams) ?: generateDefaultLayoutParams()

    override fun checkLayoutParams(
        params: LayoutParams?,
    ): Boolean = params != null

    private fun dp(value: Float): Int =
        (value * density).toInt()
}
