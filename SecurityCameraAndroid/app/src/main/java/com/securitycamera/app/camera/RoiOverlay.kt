package com.securitycamera.app.camera

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.securitycamera.app.data.AppSettings
import kotlin.math.max
import kotlin.math.min

/**
 * Maps normalized ROI (same axes as [roiRectNormalized] / ImageAnalysis buffer) onto the
 * [PreviewView] box with FIT_CENTER, using the same rotation as [androidx.camera.core.ImageProxy.imageInfo].
 *
 * Implementation: transform ROI corners + full-frame corners with [Matrix.postRotate] around the buffer
 * center, take axis-aligned bounds, then letterbox into the view (matches PreviewView FIT_CENTER behavior).
 */
fun roiOverlayFitCenterWithRotation(
    settings: AppSettings,
    viewWidthPx: Float,
    viewHeightPx: Float,
    bufferWidth: Int,
    bufferHeight: Int,
    rotationDegrees: Int,
): Pair<Offset, Size> {
    val bw = bufferWidth.toFloat().coerceAtLeast(1f)
    val bh = bufferHeight.toFloat().coerceAtLeast(1f)

    val roiLeft = settings.roiLeft * bw
    val roiTop = settings.roiTop * bh
    val roiRight = (settings.roiLeft + settings.roiWidth) * bw
    val roiBottom = (settings.roiTop + settings.roiHeight) * bh

    val roiPts = floatArrayOf(
        roiLeft,
        roiTop,
        roiRight,
        roiTop,
        roiRight,
        roiBottom,
        roiLeft,
        roiBottom,
    )

    val imgPts = floatArrayOf(
        0f,
        0f,
        bw,
        0f,
        bw,
        bh,
        0f,
        bh,
    )

    val rot = ((rotationDegrees % 360) + 360) % 360
    if (rot != 0) {
        val cx = bw / 2f
        val cy = bh / 2f
        val m = Matrix().apply { postRotate(-rot.toFloat(), cx, cy) }
        m.mapPoints(roiPts)
        m.mapPoints(imgPts)
    }

    fun bounds(pts: FloatArray): FloatArray {
        var minX = pts[0]
        var maxX = pts[0]
        var minY = pts[1]
        var maxY = pts[1]
        var i = 0
        while (i < pts.size) {
            val x = pts[i]
            val y = pts[i + 1]
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
            i += 2
        }
        return floatArrayOf(minX, maxX, minY, maxY)
    }

    val rb = bounds(roiPts)
    val ib = bounds(imgPts)
    val roiMinX = rb[0]
    val roiMaxX = rb[1]
    val roiMinY = rb[2]
    val roiMaxY = rb[3]
    val imgMinX = ib[0]
    val imgMaxX = ib[1]
    val imgMinY = ib[2]
    val imgMaxY = ib[3]

    val contentW = (imgMaxX - imgMinX).coerceAtLeast(1f)
    val contentH = (imgMaxY - imgMinY).coerceAtLeast(1f)

    val scale = min(viewWidthPx / contentW, viewHeightPx / contentH)
    val ox = (viewWidthPx - contentW * scale) * 0.5f
    val oy = (viewHeightPx - contentH * scale) * 0.5f

    val left = ox + (roiMinX - imgMinX) * scale
    val top = oy + (roiMinY - imgMinY) * scale
    val rw = (roiMaxX - roiMinX) * scale
    val rh = (roiMaxY - roiMinY) * scale

    return Offset(left, top) to Size(rw, rh)
}

fun roiOverlayFitCenter(
    settings: AppSettings,
    viewWidthPx: Float,
    viewHeightPx: Float,
    bufferWidth: Int,
    bufferHeight: Int,
): Pair<Offset, Size> =
    roiOverlayFitCenterWithRotation(
        settings,
        viewWidthPx,
        viewHeightPx,
        bufferWidth,
        bufferHeight,
        0,
    )
