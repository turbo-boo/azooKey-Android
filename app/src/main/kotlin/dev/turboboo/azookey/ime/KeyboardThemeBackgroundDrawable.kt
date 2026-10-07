package dev.turboboo.azookey.ime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlin.math.roundToInt

/*
 * azooKey renders a selected theme picture with scaledToFill behind the
 * keyboard. This drawable provides the same center-crop behavior on Android.
 */
internal class KeyboardThemeBackgroundDrawable(
    context: Context,
    private val theme: KeyboardTheme,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bitmap: Bitmap? = theme.backgroundImageUri
        ?.let(Uri::parse)
        ?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(theme.backgroundColor)
        val bitmap = bitmap ?: return
        if (bounds.isEmpty) {
            return
        }

        val source = centerCropSource(
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            targetWidth = bounds.width(),
            targetHeight = bounds.height(),
        )
        canvas.drawBitmap(
            bitmap,
            source,
            bounds,
            paint,
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int =
        if (theme.backgroundImageUri == null && android.graphics.Color.alpha(theme.backgroundColor) == 255) {
            PixelFormat.OPAQUE
        } else {
            PixelFormat.TRANSLUCENT
        }

    private fun centerCropSource(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Rect {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return Rect(0, 0, bitmapWidth, bitmapHeight)
        }

        val sourceRatio = bitmapWidth.toFloat() / bitmapHeight
        val targetRatio = targetWidth.toFloat() / targetHeight

        return if (sourceRatio > targetRatio) {
            val cropWidth = (bitmapHeight * targetRatio).roundToInt()
            val left = ((bitmapWidth - cropWidth) / 2f).roundToInt()
            Rect(
                left,
                0,
                (left + cropWidth).coerceAtMost(bitmapWidth),
                bitmapHeight,
            )
        } else {
            val cropHeight = (bitmapWidth / targetRatio).roundToInt()
            val top = ((bitmapHeight - cropHeight) / 2f).roundToInt()
            Rect(
                0,
                top,
                bitmapWidth,
                (top + cropHeight).coerceAtMost(bitmapHeight),
            )
        }
    }
}
