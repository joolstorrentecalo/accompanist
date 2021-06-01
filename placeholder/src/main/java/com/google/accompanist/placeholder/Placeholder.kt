/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.accompanist.placeholder

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun Placeholder(
    modifier: Modifier,
    color: Color = PlaceholderDefaults.PlaceholderColor,
    shape: Shape = RectangleShape
) {
    BasePlaceholder(
        modifier = modifier.placeholder(
            visible = true,
            color = color,
            shape = shape,
        )
    )
}

@Composable
fun Placeholder(
    modifier: Modifier,
    animatedBrush: PlaceholderAnimatedBrush,
    shape: Shape = RectangleShape
) {
    BasePlaceholder(
        modifier = modifier.placeholder(
            visible = true,
            animatedBrush = animatedBrush,
            shape = shape,
        )
    )
}

@Composable
private fun BasePlaceholder(
    modifier: Modifier
) {
    Layout({}, modifier) { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

fun Modifier.placeholder(
    visible: Boolean,
    color: Color = PlaceholderDefaults.PlaceholderColor,
    shape: Shape = RectangleShape
) = takeIf { visible.not() } ?: this.then(
    PlaceholderModifier(
        color = color,
        shape = shape,
        inspectorInfo = debugInspectorInfo {
            name = "placeholder"
            properties["visible"] = visible
            properties["color"] = color
            properties["shape"] = shape
        }
    )
)

fun Modifier.placeholder(
    visible: Boolean,
    animatedBrush: PlaceholderAnimatedBrush,
    shape: Shape = RectangleShape
) = takeIf { visible.not() } ?: composed {
    val infiniteTransition = rememberInfiniteTransition()
    val progress by infiniteTransition.animateFloat(
        initialValue = animatedBrush.initialValue(),
        targetValue = animatedBrush.targetValue(),
        animationSpec = animatedBrush.animationSpec()
    )
    PlaceholderModifier(
        brush = animatedBrush.brush(progress),
        shape = shape,
        inspectorInfo = debugInspectorInfo {
            name = "placeholder"
            properties["visible"] = visible
            properties["animatedBrush"] = animatedBrush
            properties["shape"] = shape
        }
    )
}

private class PlaceholderModifier(
    private val color: Color? = null,
    private val brush: Brush? = null,
    private val shape: Shape,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawModifier, InspectorValueInfo(inspectorInfo) {

    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            drawOutline()
        }
    }

    private fun ContentDrawScope.drawRect() {
        color?.let { drawRect(color = it) }
        brush?.let { drawRect(brush = it) }
    }

    private fun ContentDrawScope.drawOutline() {
        val outline =
            lastOutline.takeIf { size == lastSize && layoutDirection == lastLayoutDirection }
                ?: shape.createOutline(size, layoutDirection, this)
        color?.let { drawOutline(outline, color = color) }
        brush?.let { drawOutline(outline, brush = brush) }
        lastOutline = outline
        lastSize = size
    }

    override fun hashCode(): Int {
        var result = color?.hashCode() ?: 0
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? PlaceholderModifier ?: return false
        return color == otherModifier.color &&
            brush == otherModifier.brush &&
            shape == otherModifier.shape
    }

    override fun toString(): String =
        "PlaceholderModifier(color=$color, brush=$brush, shape=$shape)"
}
