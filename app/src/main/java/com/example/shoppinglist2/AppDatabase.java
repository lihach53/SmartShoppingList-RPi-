package com.example.shoppinglist2;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;


@Database(entities = {ShoppingItem.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ShoppingItemDao shoppingItemDao();

    private static volatile AppDatabase INSTANCE;

    // Миграция с версии 2 на 3 для добавления полей дат
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Добавляем новые колонки
            database.execSQL("ALTER TABLE shopping_items ADD COLUMN createdAt TEXT");
            database.execSQL("ALTER TABLE shopping_items ADD COLUMN updatedAt TEXT");

            // Заполняем текущей датой существующие записи
            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date());
            database.execSQL("UPDATE shopping_items SET createdAt = ?, updatedAt = ?",
                    new Object[]{currentDate, currentDate});
        }
    };
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "shopping"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}