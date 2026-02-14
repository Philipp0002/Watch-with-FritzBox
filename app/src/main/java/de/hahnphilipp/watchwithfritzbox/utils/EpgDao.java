package de.hahnphilipp.watchwithfritzbox.utils;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface  EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EpgUtils.EpgEvent event);

    @Query("SELECT * FROM EpgEvent WHERE channelNumber = :channelNumber ORDER BY startTime")
    List<EpgUtils.EpgEvent> getEventsForChannel(int channelNumber);

    @Query("SELECT * FROM EpgEvent ORDER BY startTime")
    List<EpgUtils.EpgEvent> getAllEvents();

    @Query("SELECT * FROM EpgEvent WHERE channelNumber = :channelNumber AND startTime + duration >= :time ORDER BY startTime")
    List<EpgUtils.EpgEvent> getEventsForChannelEndingAfter(int channelNumber, long time);

    @Transaction
    default void swapChannelEvents(int from, int to) {
        updateChannelNumber(from, -1);
        updateChannelNumber(to, from);
        updateChannelNumber(-1, to);
    }

    @Transaction
    default void moveChannelEvents(int from, int to) {
        if (from == to) {
            return; // Keine Änderung notwendig
        }

        if (from < to) {
            // Kanal nach oben verschieben (z.B. 2 → 5)
            // Zuerst den zu verschiebenden Kanal auf temporären Wert
            updateChannelNumber(from, -1);
            // Alle Kanäle zwischen from+1 und to um 1 nach unten
            updateChannelNumberRange(from + 1, to, -1);
            // Dann den temporären Kanal auf Zielposition
            updateChannelNumber(-1, to);
        } else {
            // Kanal nach unten verschieben (z.B. 5 → 2)
            // Zuerst den zu verschiebenden Kanal auf temporären Wert
            updateChannelNumber(from, -1);
            // Alle Kanäle zwischen to und from-1 um 1 nach oben
            updateChannelNumberRange(to, from - 1, 1);
            // Dann den temporären Kanal auf Zielposition
            updateChannelNumber(-1, to);
        }
    }

    @Query("UPDATE EpgEvent SET channelNumber = channelNumber + :offset WHERE channelNumber BETWEEN :start AND :end")
    void updateChannelNumberRange(int start, int end, int offset);

    @Query("UPDATE EpgEvent SET channelNumber = :newChannel WHERE channelNumber = :oldChannel")
    void updateChannelNumber(int oldChannel, int newChannel);

    @Query("DELETE FROM EpgEvent")
    void clear();

    @Query("""
        SELECT * FROM EpgEvent
        WHERE channelNumber = :channelNumber
          AND :timeInSec BETWEEN startTime AND (startTime + duration)
        LIMIT 1
    """)
    EpgUtils.EpgEvent getEventAtTime(int channelNumber, long timeInSec);

    @Query("""
        SELECT * FROM EpgEvent
        WHERE :timeInSec BETWEEN startTime AND (startTime + duration)
    """)
    List<EpgUtils.EpgEvent> getEventsAtTime(long timeInSec);

    @Query("""
        DELETE FROM EpgEvent
        WHERE channelNumber = :channelNumber
          AND (
               -- Alte Events entfernen
               (eitReceivedTimeMillis + (:removeEventTimeSec * 1000)) <= :currentTimeMillis
               -- Oder überlappende Events entfernen
               OR NOT (
                    startTime >= (:newEventStart + :newEventDuration)
                 OR :newEventStart >= (startTime + duration)
               )
          )
    """)
    void deleteExpiredAndOverlappingEvents(
            int channelNumber,
            long newEventStart,
            long newEventDuration,
            long removeEventTimeSec,
            long currentTimeMillis
    );

    @Query("DELETE FROM EpgEvent")
    void deleteAll();
}
