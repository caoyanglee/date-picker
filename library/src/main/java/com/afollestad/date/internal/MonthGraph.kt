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
package com.afollestad.date.internal

import androidx.annotation.CheckResult
import com.afollestad.date.MonthDef
import com.afollestad.date.dayOfMonth
import com.afollestad.date.dayOfWeek
import com.afollestad.date.decrementMonth
import com.afollestad.date.incrementMonth
import com.afollestad.date.month
import com.afollestad.date.totalDaysInMonth
import com.afollestad.date.year
import java.util.Calendar
import kotlin.properties.Delegates

/** The date of an empty date, a placeholder in the graph. */
internal const val NO_DATE: Int = -1

/** @author Aidan Follestad (@afollestad) */
internal data class Date(
  val dayOfWeek: DayOfWeek,
  val date: Int = NO_DATE
)

/** @author Aidan Follestad (@afollestad) */
internal data class Week(
  @MonthDef val month: Int,
  val year: Int,
  val dates: List<Date>
)

/** @author Aidan Follestad (@afollestad) */
internal class MonthGraph(
  val calendar: Calendar
) {
  var daysInMonth: Int by Delegates.notNull()
  lateinit var firstWeekDayInMonth: DayOfWeek
  lateinit var orderedWeekDays: List<DayOfWeek>

  init {
    calendar.dayOfMonth = 1
    invalidateData()
  }

  @CheckResult fun currentMonth(): Int = calendar.month

  fun previousMonth() {
    calendar.decrementMonth()
    invalidateData()
  }

  fun canGoBack(minDate: DateSnapshot?): Boolean {
    if (minDate == null) {
      return true
    }
    return true // TODO different logic
  }

  fun nextMonth() {
    calendar.incrementMonth()
    invalidateData()
  }

  fun canGoForward(maxDate: DateSnapshot?): Boolean {
    if (maxDate == null) {
      return true
    }
    return true // TODO different logic
  }

  @CheckResult fun getWeeks(): List<Week> {
    val weeks = mutableListOf<Week>()
    val datesBuffer = mutableListOf<Date>()

    // Add prefix days first, days the lead up from last month to the first day of this
    orderedWeekDays
        .takeWhile { it != firstWeekDayInMonth }
        .forEach { datesBuffer.add(Date(it)) }

    for (date in 1..daysInMonth) {
      calendar.dayOfMonth = date
      datesBuffer.add(
          Date(
              dayOfWeek = calendar.dayOfWeek,
              date = date
          )
      )
      if (datesBuffer.size == DAYS_IN_WEEK) {
        // We've reached another week
        weeks.add(
            Week(
                month = calendar.month,
                year = calendar.year,
                dates = datesBuffer.toList()
            )
        )
        datesBuffer.clear()
      }
    }

    if (datesBuffer.isNotEmpty()) {
      // Fill in remaining days of week
      val loopTarget = orderedWeekDays.last()
          .nextDayOfWeek()
      datesBuffer.last()
          .dayOfWeek
          .nextDayOfWeek()
          .andTheRest()
          .takeWhile { it != loopTarget }
          .forEach { datesBuffer.add(Date(it)) }
      // Add any left over as a last week
      weeks.add(
          Week(
              month = calendar.month,
              year = calendar.year,
              dates = datesBuffer.toList()
          )
      )
      datesBuffer.clear()
    }
    // Make sure we always come out to 6 weeks at least
    while (weeks.size < TOTAL_WEEKS) {
      weeks.add(
          Week(
              month = calendar.month,
              year = calendar.year,
              dates = getEmptyDates()
          )
      )
    }

    require(weeks.size == TOTAL_WEEKS) { "${weeks.size} must equal $TOTAL_WEEKS" }
    return weeks
  }

  private fun getEmptyDates(): List<Date> {
    return orderedWeekDays.map { Date(it, NO_DATE) }
  }

  private fun invalidateData() {
    daysInMonth = calendar.totalDaysInMonth
    firstWeekDayInMonth = calendar.dayOfWeek
    orderedWeekDays = calendar.firstDayOfWeek
        .asDayOfWeek()
        .andTheRest()
  }

  private companion object {
    const val DAYS_IN_WEEK: Int = 7
    const val TOTAL_WEEKS: Int = 6
  }
}
