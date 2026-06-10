// OrthoComplicationService.kt
package wadotu.orthodoxy.orthowear.presentation.watch

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import wadotu.orthodoxy.orthowear.R
import wadotu.orthodoxy.orthowear.presentation.BookNameMapper
import wadotu.orthodoxy.orthowear.presentation.LocaleHelper
import wadotu.orthodoxy.orthowear.presentation.OrthoCalendar
import wadotu.orthodoxy.orthowear.presentation.OrthoDay

class OrthoComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.LONG_TEXT  -> buildLongText(feastText = "성령 축일", fastText = "금식 없음")
        ComplicationType.SHORT_TEXT -> buildShortText(readingText = "요 3:16", label = "복음")
        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val day = fetchDay() ?: return fallback(request.complicationType)

        return when (request.complicationType) {
            ComplicationType.LONG_TEXT  -> buildLongText(
                feastText = resolveFeast(day),
                fastText  = OrthoCalendar.translateFasting(this, day)
            )
            ComplicationType.SHORT_TEXT -> buildShortText(
                readingText = resolveReading(day),
                label       = resolveReadingLabel(day)
            )
            else -> null
        }
    }

    // ── 1. 축일명 ──────────────────────────────────────────────────────────
    // feasts[] 우선 → 없으면 titles[] → 둘 다 없으면 watch_no_feast
    private fun resolveFeast(day: OrthoDay): String {
        val sources = listOf(day.feasts, day.titles)
        for (source in sources) {
            if (source.isNullOrEmpty()) continue
            for (raw in source) {
                val translated = OrthoCalendar.translateApiTitle(this, raw)
                if (!translated.isNullOrEmpty()) return translated
            }
        }
        return getString(R.string.watch_no_feast)
    }

    // ── 2. 독서 (복음경 → 사도경 → 사도행전 우선순위) ─────────────────────
    // 워치 출력 예: "요 3:16", "로마 5:1"
    private fun resolveReading(day: OrthoDay): String {
        if (day.readings.isNullOrEmpty()) return ""

        val priority = listOf("gospel", "epistle", "apostle", "acts")
        val reading = day.readings
            .sortedBy { r ->
                val desc = r.description?.lowercase() ?: ""
                priority.indexOfFirst { desc.contains(it) }
                    .let { if (it == -1) Int.MAX_VALUE else it }
            }
            .firstOrNull() ?: return ""

        val passage = reading.passage?.firstOrNull()
        return if (passage != null && passage.verse > 0) {
            val bookName = BookNameMapper.get(passage.book)
            "$bookName ${passage.chapter}:${passage.verse}"
        } else {
            reading.display ?: ""
        }
    }

    // ── 3. 독서 레이블 (복음경이면 "복음", 나머지는 "독서") ─────────────────
    private fun resolveReadingLabel(day: OrthoDay): String {
        if (day.readings.isNullOrEmpty()) {
            val lang = LocaleHelper.getLanguage(this)
            return when (lang) {
                "ko" -> "독서"
                "el" -> "Ανάγνωση"
                "cu" -> "Чтенїе"
                else -> "Reading"
            }
        }

        val priority = listOf("gospel", "epistle", "apostle", "acts")
        val reading = day.readings
            .sortedBy { r ->
                val desc = r.description?.lowercase() ?: ""
                priority.indexOfFirst { desc.contains(it) }
                    .let { if (it == -1) Int.MAX_VALUE else it }
            }
            .firstOrNull() ?: run {
            val lang = LocaleHelper.getLanguage(this)
            return when (lang) {
                "ko" -> "독서"
                "el" -> "Ανάγνωση"
                "cu" -> "Чтенїе"
                else -> "Reading"
            }
        }

        val desc = reading.description?.lowercase() ?: ""
        val lang = LocaleHelper.getLanguage(this)

        return if (desc.contains("gospel")) {
            when (lang) {
                "ko" -> "복음경"
                "el" -> "Ευαγγέλιο"
                "cu" -> "Єѵⷢ҇лїе"
                else -> "Gospel"
            }
        } else {
            when (lang) {
                "ko" -> "독서"
                "el" -> "Ανάγνωση"
                "cu" -> "Чтенїе"
                else -> "Reading"
            }
        }
    }

    // ── API 호출 ───────────────────────────────────────────────────────────
    private suspend fun fetchDay(): OrthoDay? = suspendCancellableCoroutine { cont ->
        OrthoCalendar.getFullCalendarDay(
            this, "gregorian", 0, 0, 0,
            object : OrthoCalendar.FullCalendarCallback {
                override fun onDataLoaded(day: OrthoDay) { if (cont.isActive) cont.resume(day) }
                override fun onError(message: String)    { if (cont.isActive) cont.resume(null) }
            }
        )
    }

    // ── 빌더 ──────────────────────────────────────────────────────────────
    // LONG_TEXT  : title(상단 작은 글씨) = 금식정보 / longText(큰 글씨) = 축일명
    // SHORT_TEXT : title = "복음" or "독서" / shortText = "요 3:16"
    private fun buildLongText(feastText: String, fastText: String) =
        LongTextComplicationData.Builder(
            PlainComplicationText.Builder(feastText).build(),
            PlainComplicationText.Builder("Orthodox Calendar").build()
        )
            .setTitle(PlainComplicationText.Builder(fastText).build())
            .build()

    private fun buildShortText(readingText: String, label: String) =
        ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(readingText.take(7)).build(),
            PlainComplicationText.Builder("Orthodox").build()
        )
            .setTitle(PlainComplicationText.Builder(label.take(7)).build())
            .build()

    private fun fallback(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.LONG_TEXT  -> buildLongText(getString(R.string.watch_no_feast), "")
        ComplicationType.SHORT_TEXT -> buildShortText("—", "")
        else -> null
    }
}