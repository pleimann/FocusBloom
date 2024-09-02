/*
 * Copyright 2023 Joel Kanyi.
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
package com.joelkanyi.focusbloom.feature.statistics.component

import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joelkanyi.focusbloom.core.utils.aAllEntriesAreZero
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.bar.VerticalBarPlotEntry
import io.github.koalaplot.core.bar.DefaultVerticalBarPlotEntry
import io.github.koalaplot.core.bar.DefaultVerticalBar
import io.github.koalaplot.core.bar.DefaultVerticalBarPosition
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.bar.VerticalBarPosition
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.VerticalRotation
import io.github.koalaplot.core.util.VerticalRotation.COUNTER_CLOCKWISE
import io.github.koalaplot.core.util.rotateVertically
import io.github.koalaplot.core.util.toString
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.TickPosition
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle

internal val padding = 8.dp
internal val paddingMod = Modifier.padding(padding)

private const val BarWidth = 0.8f

@Composable
fun AxisTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
fun AxisLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

fun barChartEntries(fibonacci: List<Float>): List<VerticalBarPlotEntry<Float, Float>> {
    return buildList {
        fibonacci.forEachIndexed { index, fl ->
            add(
                DefaultVerticalBarPlotEntry(
                    x = (index + 1).toFloat(),
                    y = DefaultVerticalBarPosition(
                        yMin = 0f,
                        yMax = fl,
                    )
                ),
            )
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun BarChart(tickPositionState: TickPositionState, entries: List<Float>) {
    val yAxisRange = 0f..if (entries.aAllEntriesAreZero()) 20f else entries.maxOf { it }
    val xAxisRange = 0.5f..7.5f
    ChartLayout(
        modifier = paddingMod,
        title = { },
    ) {
        /*
            xAxisModel: AxisModel<X>,
            yAxisModel: AxisModel<Y>,
            modifier: Modifier = Modifier,
            xAxisStyle: AxisStyle = rememberAxisStyle(),
            xAxisLabels: (X) -> String = { it.toString() },
            xAxisTitle: String? = null,
            yAxisStyle: AxisStyle = rememberAxisStyle(),
            yAxisLabels: (Y) -> String = { it.toString() },
            yAxisTitle: String? = null,
            horizontalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
            horizontalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
            verticalMajorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.majorGridlineStyle,
            verticalMinorGridLineStyle: LineStyle? = KoalaPlotTheme.axis.minorGridlineStyle,
            panZoomEnabled: Boolean = true,
         */
        XYGraph(
            xAxisModel = FloatLinearAxisModel(
                xAxisRange,
                minimumMajorTickIncrement = 1f,
                minimumMajorTickSpacing = 10.dp,
                zoomRangeLimit = 3f,
                minorTickCount = 0,
            ),
            yAxisModel = FloatLinearAxisModel(
                yAxisRange,
                minimumMajorTickIncrement = 1f,
                minorTickCount = 0,
            ),
            xAxisStyle = rememberAxisStyle(
                tickPosition = tickPositionState.horizontalAxis,
                color = Color.LightGray,
            ),
            xAxisLabels = { tick: Float ->
                AxisLabel(
                    label = when(tick) {
                        1f -> "Mon"
                        2f -> "Tue"
                        3f -> "Wed"
                        4f -> "Thu"
                        5f -> "Fri"
                        6f -> "Sat"
                        7f -> "Sun"
                        else -> ""
                    }
                )
            },
            xAxisTitle = {
                AxisTitle(
                    "Day of the Week",
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
            yAxisStyle = rememberAxisStyle(
                tickPosition = tickPositionState.verticalAxis
            ),
            yAxisLabels = { tick: Float ->
                AxisLabel(tick.toString(0), Modifier.absolutePadding(right = 2.dp))
            },
            yAxisTitle = {
                AxisTitle(
                    "Tasks Completed",
                    modifier = Modifier.rotateVertically(COUNTER_CLOCKWISE)
                        .padding(bottom = padding),
                )
            },
        ) {
            val barChartEntries = barChartEntries(
                fibonacci = entries,
            )

            VerticalBarPlot(
                xData = barChartEntries.map { entry -> entry.x },
                yData =  barChartEntries.map { entry -> entry.x },
                bar = {index ->
                    DefaultVerticalBar(
                        brush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(BarWidth),
                    ) {
                        HoverSurface { Text(barChartEntries.get(index).toString()) }
                    }
                },
            )
        }
    }
}

data class TickPositionState(
    val verticalAxis: TickPosition,
    val horizontalAxis: TickPosition,
)

@Composable
fun HoverSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = Color.LightGray,
        modifier = modifier.padding(padding),
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
