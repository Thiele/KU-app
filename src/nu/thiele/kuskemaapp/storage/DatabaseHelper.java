package nu.thiele.kuskemaapp.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {    
    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase db) {
    	db.execSQL(DatabaseContract.CourseEntry.CREATE_TABLE);
    	db.execSQL(DatabaseContract.CourseClassEntry.CREATE_TABLE);
    	db.execSQL(DatabaseContract.UserPreferenceEntry.CREATE_TABLE);
    }

    // Method is called during an upgrade of the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	db.execSQL(DatabaseContract.CourseEntry.DELETE_TABLE);
    	db.execSQL(DatabaseContract.CourseClassEntry.DELETE_TABLE);
        onCreate(db);
    }
}