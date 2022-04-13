/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package id.mncinnovation.ocr.analyzer

import android.graphics.*
import android.util.Log
import id.mncinnovation.identification.core.GraphicOverlay

/** Draw the detected object info in preview.  */
class ObjectGraphic constructor(
  overlay: GraphicOverlay,
  private val originalBitmap: Bitmap,
  private val points: List<PointF>
) : GraphicOverlay.Graphic(overlay) {

  private val numColors = COLORS.size

  private val boxPaints = Array(numColors) { Paint() }
  private val textPaints = Array(numColors) { Paint() }
  private val labelPaints = Array(numColors) { Paint() }

  init {
    for (i in 0 until numColors) {
      textPaints[i] = Paint()
      textPaints[i].color = COLORS[i][0]
      textPaints[i].textSize = TEXT_SIZE
      boxPaints[i] = Paint()
      boxPaints[i].color = COLORS[i][1]
      boxPaints[i].style = Paint.Style.FILL
      boxPaints[i].strokeWidth = STROKE_WIDTH
      labelPaints[i] = Paint()
      labelPaints[i].color = COLORS[i][1]
      labelPaints[i].style = Paint.Style.FILL
    }
  }

  override fun draw(canvas: Canvas) {
    // Decide color based on object tracking ID
    val colorID = 0
//      if (detectedObject.trackingId == null) 0
//      else abs(detectedObject.trackingId!! % NUM_COLORS)
//    var textWidth =
//      textPaints[colorID].measureText("Tracking ID: " + detectedObject.trackingId)
//    val lineHeight = TEXT_SIZE + STROKE_WIDTH
//    var yLabelOffset = -lineHeight
//
//    // Calculate width and height of label box
//    for (label in detectedObject.labels) {
//      textWidth =
//        max(textWidth, textPaints[colorID].measureText(label.text))
//      textWidth = max(
//        textWidth,
//        textPaints[colorID].measureText(
//          String.format(
//            Locale.US,
//            LABEL_FORMAT,
//            label.confidence * 100,
//            label.index
//          )
//        )
//      )
//      yLabelOffset -= 2 * lineHeight
//    }

    // Draws the bounding box.
    val pointer = getOrderedPoints(points)
    if(isValidShape(pointer)) {
    val path = Path()
      path.moveTo(translateX(pointer[0]!!.x), translateY(pointer[0]!!.y))
      path.lineTo(translateX(pointer[1]!!.x), translateY(pointer[1]!!.y))
      path.lineTo(translateX(pointer[3]!!.x), translateY(pointer[3]!!.y))
      path.lineTo(translateX(pointer[2]!!.x), translateY(pointer[2]!!.y))
      path.close()

      canvas.drawPath(path, boxPaints[1].apply {
        alpha = 100
      })
//      val rect = RectF(pointer[0]!!.x, pointer[0]!!.y, pointer[2]!!.x,pointer[2]!!.y)
//      val x0 = translateX(rect.left)
//      val x1 = translateX(rect.right)
//      rect.left = min(x0, x1)
//      rect.right = max(x0, x1)
//      rect.top = translateY(rect.top)
//      rect.bottom = translateY(rect.bottom)
//      canvas.drawRect(rect, boxPaints[colorID])
      Log.d("Object Graphic", "pointer $pointer")
      canvas.drawLine(
        translateX(pointer[0]!!.x),
        translateY(pointer[0]!!.y),
        translateX(pointer[1]!!.x),
        translateY(pointer[1]!!.y),
        boxPaints[colorID])

      canvas.drawLine(
        translateX(pointer[0]!!.x),
        translateY(pointer[0]!!.y),
        translateX(pointer[2]!!.x),
        translateY(pointer[2]!!.y),
        boxPaints[colorID])

      canvas.drawLine(
        translateX(pointer[1]!!.x),
        translateY(pointer[1]!!.y),
        translateX(pointer[3]!!.x),
        translateY(pointer[3]!!.y),
        boxPaints[colorID])

      canvas.drawLine(
        translateX(pointer[2]!!.x),
        translateY(pointer[2]!!.y),
        translateX(pointer[3]!!.x),
        translateY(pointer[3]!!.y),
        boxPaints[colorID])
    }
    else{
      Log.d(TAG,"Invalid Shape $pointer")
    }

//    // Draws other object info.
//    canvas.drawRect(
//      rect.left - STROKE_WIDTH,
//      rect.top + yLabelOffset,
//      rect.left + textWidth + 2 * STROKE_WIDTH,
//      rect.top,
//      labelPaints[colorID]
//    )
//    yLabelOffset += TEXT_SIZE
//    canvas.drawText(
//      "Tracking ID: " + detectedObject.trackingId,
//      rect.left,
//      rect.top + yLabelOffset,
//      textPaints[colorID]
//    )
//    yLabelOffset += lineHeight
//    for (label in detectedObject.labels) {
//      canvas.drawText(
//        label.text + " (index: " + label.index + ")",
//        rect.left,
//        rect.top + yLabelOffset,
//        textPaints[colorID]
//      )
//      yLabelOffset += lineHeight
//      canvas.drawText(
//        String.format(
//          Locale.US,
//          LABEL_FORMAT,
//          label.confidence * 100,
//          label.index
//        ),
//        rect.left,
//        rect.top + yLabelOffset,
//        textPaints[colorID]
//      )
//      yLabelOffset += lineHeight
//    }
  }

  fun isValidShape(pointFMap: Map<Int, PointF>): Boolean {
    return pointFMap.size == 4
  }

  private fun getOrderedPoints(points: List<PointF>): Map<Int, PointF> {
    val centerPoint = PointF()
    val size = points.size
    for (pointF in points) {
      centerPoint.x += pointF.x / size
      centerPoint.y += pointF.y / size
    }
    val orderedPoints: MutableMap<Int, PointF> = HashMap()
    for (pointF in points) {
      var index = -1
      if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
        index = 0
      } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
        index = 1
      } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
        index = 2
      } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
        index = 3
      }
      orderedPoints[index] = pointF
    }
    return orderedPoints
  }

  companion object {
    const val TAG = "Object Graphic"
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
    private const val NUM_COLORS = 10
    private val COLORS =
      arrayOf(
        intArrayOf(Color.BLACK, Color.WHITE),
        intArrayOf(Color.WHITE, Color.MAGENTA),
        intArrayOf(Color.BLACK, Color.LTGRAY),
        intArrayOf(Color.WHITE, Color.RED),
        intArrayOf(Color.WHITE, Color.BLUE),
        intArrayOf(Color.WHITE, Color.DKGRAY),
        intArrayOf(Color.BLACK, Color.CYAN),
        intArrayOf(Color.BLACK, Color.YELLOW),
        intArrayOf(Color.WHITE, Color.BLACK),
        intArrayOf(Color.BLACK, Color.GREEN)
      )
    private const val LABEL_FORMAT = "%.2f%% confidence (index: %d)"
  }
}
