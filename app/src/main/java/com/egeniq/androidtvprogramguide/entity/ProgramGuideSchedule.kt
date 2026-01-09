/*
 * Copyright (c) 2020, Egeniq
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

package com.egeniq.androidtvprogramguide.entity

import com.egeniq.androidtvprogramguide.util.ProgramGuideUtil
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils
import org.threeten.bp.Instant

/**
 * This class represents a programme in the EPG.
 * The program you associate with it can be your own class where you put the relevant values.
 * The ID should be unique across all schedules used in this app.
 * The start and end time are defined in UTC milliseconds. Overlapping times (within one channel) are not allowed
 * and will be corrected by the manager.
 * Is clickable defines if the user can click on this schedule, and so will trigger onScheduleClicked(schedule).
 * The displayTitle property is the string which is visible to the user in the EPG.
 */
data class ProgramGuideSchedule(
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val originalTimes: OriginalTimes,
    val isClickable: Boolean,
    val program: EpgUtils.EpgEvent?
) {

    /**
     * Used internally. We make some fixes and adjustments to the times in the program manager,
     * but for consistency we keep the original times here as well.
     */
    data class OriginalTimes(
        val startsAtMillis: Long,
        val endsAtMillis: Long
    )

    companion object {
        private const val GAP_ID = -1L

        fun createGap(from: Long, to: Long): ProgramGuideSchedule {
            return ProgramGuideSchedule(
                from,
                to,
                OriginalTimes(from, to),
                false,
                null
            )
        }

        fun <T> createScheduleWithProgram(
            startsAt: Instant,
            endsAt: Instant,
            isClickable: Boolean,
            program: EpgUtils.EpgEvent
        ): ProgramGuideSchedule {
            return ProgramGuideSchedule(
                startsAt.toEpochMilli(),
                endsAt.toEpochMilli(),
                OriginalTimes(startsAt.toEpochMilli(), endsAt.toEpochMilli()),
                isClickable,
                program
            )
        }
    }

    val id = program?.id ?: GAP_ID
    val displayTitle = program?.title ?: ""
    val width = ProgramGuideUtil.convertMillisToPixel(startsAtMillis, endsAtMillis)
    val isGap = program == null
    val isCurrentProgram: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startsAtMillis..endsAtMillis
        }
}