package com.securitycamera.app.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.securitycamera.app.data.AppSettings
import java.io.ByteArrayOutputStream

fun ImageProxy.toBitmapRgba(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    planes[0].buffer.rewind()
    bitmap.copyPixelsFromBuffer(planes[0].buffer)
    return bitmap
}

fun ImageProxy.toJpegBytes(quality: Int = 85): ByteArray {
    if (format == ImageFormat.JPEG) {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }
    val bmp = toBitmapRgba()
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}

fun roiRectNormalized(settings: AppSettings, width: Int, height: Int): Rect {
    val left = (settings.roiLeft * width).toInt().coerceIn(0, width - 1)
    val top = (settings.roiTop * height).toInt().coerceIn(0, height - 1)
    val right = ((settings.roiLeft + settings.roiWidth) * width).toInt().coerceIn(1, width)
    val bottom = ((settings.roiTop + settings.roiHeight) * height).toInt().coerceIn(1, height)
    return Rect(left, top, right, bottom)
}

fun boundingBoxCenterInsideRoi(faceBox: Rect, roi: Rect): Boolean {
    val cx = (faceBox.left + faceBox.right) / 2
    val cy = (faceBox.top + faceBox.bottom) / 2
    return roi.contains(cx, cy)
}
