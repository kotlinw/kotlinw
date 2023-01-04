package kotlinw.util.datetime

import kotlinx.datetime.Month
import kotlinx.serialization.Serializable

@Serializable
data class MonthDay(val month: Month, val dayOfMonth: DayOfMonth)
