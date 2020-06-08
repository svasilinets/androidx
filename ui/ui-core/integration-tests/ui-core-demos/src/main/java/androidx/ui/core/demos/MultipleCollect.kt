/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PLUGIN_WARNING")

package androidx.ui.core.demos

import androidx.compose.Composable
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.id
import androidx.ui.core.layoutId
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize

@Composable
fun HeaderFooterLayout(
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Layout({
        Box(Modifier.layoutId("header"), children = header)
        Box(Modifier.layoutId("footer"), children = footer)
        content()
    }) { measurables, constraints, _ ->
        val headerPlaceable = measurables.first { it.id == "header" }.measure(
            Constraints.fixed(constraints.maxWidth, 100)
        )
        val footerPadding = 50
        val footerPlaceable = measurables.first { it.id == "footer" }.measure(
            Constraints.fixed(constraints.maxWidth - footerPadding * 2, 100)
        )

        val contentMeasurables = measurables.filter { it.id == null }
        val itemHeight =
            (constraints.maxHeight - headerPlaceable.height - footerPlaceable.height) /
                    contentMeasurables.size
        val contentPlaceables = contentMeasurables.map { measurable ->
            measurable.measure(Constraints.fixed(constraints.maxWidth, itemHeight))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.place(0, 0)
            footerPlaceable.place(footerPadding, constraints.maxHeight - footerPlaceable.height)
            var top = headerPlaceable.height
            contentPlaceables.forEach { placeable ->
                placeable.place(0, top)
                top += itemHeight
            }
        }
    }
}

@Composable
fun MultipleCollectTest() {
    val header = @Composable {
        Box(Modifier.fillMaxSize(), backgroundColor = Color(android.graphics.Color.GRAY))
    }
    val footer = @Composable {
        Box(Modifier.fillMaxSize(), backgroundColor = Color(android.graphics.Color.BLUE))
    }
    HeaderFooterLayout(header = header, footer = footer) {
        Box(Modifier.fillMaxSize(), backgroundColor = Color(android.graphics.Color.GREEN))
        Box(Modifier.fillMaxSize(), backgroundColor = Color(android.graphics.Color.YELLOW))
    }
}
