/**
 * Designed and developed by Aidan Follestad (@afollestad)
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
package com.afollestad.date.renderers

import android.view.Gravity.CENTER
import android.view.View
import android.widget.TextView
import com.afollestad.date.DatePickerConfig
import com.afollestad.date.data.DayOfWeek
import com.afollestad.date.data.MonthItem
import com.afollestad.date.data.MonthItem.DayOfMonth
import com.afollestad.date.data.MonthItem.WeekHeader
import com.afollestad.date.data.NO_DATE
import com.afollestad.date.dayOfWeek
import com.afollestad.date.util.Util.createCircularSelector
import com.afollestad.date.util.Util.createTextSelector
import com.afollestad.date.util.onClickDebounced
import com.nlf.calendar.Lunar
import com.nlf.calendar.util.HolidayUtil
import com.nlf.calendar.util.LunarUtil
import com.nlf.calendar.util.SolarUtil
import java.util.*

/** @author Aidan Follestad (@afollestad) */
internal class MonthItemRenderer(private val config: DatePickerConfig) {
    private val calendar = Calendar.getInstance()

    fun render(
        item: MonthItem,
        rootView: View,
        textView: TextView,
        onSelection: (DayOfMonth) -> Unit
    ) {
        when (item) {
            is WeekHeader -> renderWeekHeader(item.dayOfWeek, textView)
            is DayOfMonth -> renderDayOfMonth(item, rootView, textView, onSelection)
        }
    }

    private fun renderWeekHeader(
        dayOfWeek: DayOfWeek,
        textView: TextView
    ) {
        textView.apply {
            setTextColor(config.textIconColor)
            calendar.dayOfWeek = dayOfWeek
            text = config.dateFormatter.weekdayAbbreviation(calendar)
            typeface = config.normalFont
        }
    }

    private fun renderDayOfMonth(
        dayOfMonth: DayOfMonth,
        rootView: View,
        textView: TextView,
        onSelection: (DayOfMonth) -> Unit
    ) {
        rootView.background = null

        textView.apply {
            setTextColor(createTextSelector(context, config.selectionColor, config.textIconColor))
            //是否开启农历
            if (config.enableChineseCalendar) {
                val year = dayOfMonth.month.year
                val month = dayOfMonth.month.month//后续计算需要+1
                val day = dayOfMonth.date
                var dayInMonthStr = dayOfMonth.date.positiveOrEmptyAsString()

                if (dayInMonthStr.isNotBlank()) {

                    //节日
                    var targetStr = ""
                    val lunar = Lunar(Calendar.getInstance().apply {
                        set(year, month, day)
                    }.time)
                    val jieQiStr = if (lunar.jie.isNotBlank()) lunar.jie else lunar.qi//节气
                    val festivalStr = LunarUtil.FESTIVAL["${lunar.month}-${lunar.day}"]//农历节日
                    val solarStr = SolarUtil.FESTIVAL["${month+1}-$day"]//阳历节日
                    val chineseDay = lunar.dayInChinese//中文日
                    targetStr = when {
                        !jieQiStr.isNullOrBlank() -> jieQiStr
                        !festivalStr.isNullOrBlank() -> festivalStr
                        !solarStr.isNullOrBlank() -> solarStr
                        !chineseDay.isNullOrBlank() -> chineseDay
                        else -> ""
                    }
                    dayInMonthStr += "\n${targetStr}"
                }
                text = dayInMonthStr
            } else {
                text = dayOfMonth.date.positiveOrEmptyAsString()
            }

            typeface = config.normalFont
            gravity = CENTER
            background = null
            setOnClickListener(null)
        }

        if (dayOfMonth.date == NO_DATE) {
            rootView.isEnabled = false
            textView.isSelected = false
            textView.isActivated = false
            return
        }

        rootView.isEnabled = textView.text.toString()
            .isNotEmpty()
        textView.apply {
            isSelected = dayOfMonth.isSelected
            isActivated = dayOfMonth.isToday
            background = createCircularSelector(
                context = context,
                selectedColor = config.selectionColor,
                todayStrokeColor = config.todayStrokeColor
            )
            onClickDebounced { onSelection(dayOfMonth) }
        }
    }

    private fun Int.positiveOrEmptyAsString(): String {
        return if (this < 1) "" else toString()
    }
}
