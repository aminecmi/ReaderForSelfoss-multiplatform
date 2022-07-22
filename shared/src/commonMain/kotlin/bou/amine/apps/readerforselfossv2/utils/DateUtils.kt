package bou.amine.apps.readerforselfossv2.utils

import android.text.format.DateUtils
import bou.amine.apps.readerforselfossv2.rest.SelfossModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun SelfossModel.Item.parseDate(dateUtils: bou.amine.apps.readerforselfossv2.utils.DateUtils): Instant =
    dateUtils.parseDate(this.datetime)

fun SelfossModel.Item.parseRelativeDate(dateUtils: bou.amine.apps.readerforselfossv2.utils.DateUtils): String =
    dateUtils.parseRelativeDate(this.datetime)

class DateUtils(private val apiMajorVersion: Int) {
    fun parseDate(dateString: String): Instant {

        val FORMATTERV1 = "yyyy-MM-dd HH:mm:ss"

        return if (apiMajorVersion >= 4) {
            OffsetDateTime.parse(dateString).toInstant()
        } else {
            LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(FORMATTERV1)).toInstant(ZoneOffset.UTC)
        }
    }

    fun parseRelativeDate(dateString: String): String {

        val date = parseDate(dateString)

        return " " + DateUtils.getRelativeTimeSpanString(
            date.toEpochMilli(),
            Instant.now().toEpochMilli(),
            60000L, // DateUtils.MINUTE_IN_MILLIS,
            262144 // DateUtils.FORMAT_ABBREV_RELATIVE
        )
    }
}