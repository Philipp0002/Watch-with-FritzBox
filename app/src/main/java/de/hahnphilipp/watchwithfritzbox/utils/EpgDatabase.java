package de.hahnphilipp.watchwithfritzbox.utils;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EpgUtils.EpgEvent.class}, version = 1)
public abstract class EpgDatabase extends RoomDatabase {
    public abstract EpgDao epgDao();
}
