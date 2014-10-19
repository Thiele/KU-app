package nu.thiele.kuskemaapp.storage;

import android.provider.BaseColumns;

public final class DatabaseContract {
	public static final  String    DATABASE_NAME   = "database.sql";
	public static final  int    DATABASE_VERSION   = 1;
    private static final String INTEGER_TYPE          = " INTEGER";
    private static final String TEXT_TYPE          = " TEXT";
    private static final String COMMA_SEP          = ",";

	
	private DatabaseContract(){}
	
	public static abstract class CourseEntry implements BaseColumns {
        public static final String TABLE_NAME = "course";
        public static final String COURSE_LINK = "link";
        public static final String COURSE_STUDY_NAEVN = "studie_naevn";
        public static final String COURSE_LEVEL = "level";
        public static final String COURSE_TITLE = "title";
        public static final String COURSE_ECTS = "ects";
        public static final String COURSE_YEAR = "year";
        public static final String COURSE_LANGUAGE = "language";
        
        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COURSE_LINK + TEXT_TYPE + COMMA_SEP +
                COURSE_STUDY_NAEVN + TEXT_TYPE + COMMA_SEP +
                COURSE_LEVEL + TEXT_TYPE + COMMA_SEP +
                COURSE_ECTS + TEXT_TYPE + COMMA_SEP +
                COURSE_LANGUAGE + TEXT_TYPE + COMMA_SEP +
                COURSE_YEAR + TEXT_TYPE + COMMA_SEP +
                COURSE_TITLE + TEXT_TYPE + " )";
        public static final String DELETE_LEFTOVER_COURSES = "DELETE FROM "+TABLE_NAME+
        		" WHERE "+_ID+" NOT IN (SELECT DISTINCT "+CourseClassEntry.COURSE_ID+" FROM "+CourseClassEntry.TABLE_NAME+")";
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        
        public static final String DELETE_COURSE(int which){
        	return "DELETE FROM "+TABLE_NAME+" WHERE "+BaseColumns._ID+"="+which;
        }
        
        public static final String SELECT_ALL_COURSES = "SELECT * FROM "+TABLE_NAME+" ORDER BY "+COURSE_TITLE+" ASC";
	}
	
	public static abstract class CourseClassEntry implements BaseColumns {
        public static final String TABLE_NAME = "course_classes";
        public static final String COURSE_ID = "class_id";
        public static final String COURSE_DAY = "day";
        public static final String COURSE_START_TIME = "start_time";
        public static final String COURSE_END_TIME = "end_time";
        public static final String COURSE_TYPE = "type";
        public static final String COURSE_PLACE = "place";
        public static final String COURSE_TEACHER = "teacher";
        public static final String IS_READY = "is_ready";
        
        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COURSE_ID + INTEGER_TYPE + COMMA_SEP +
                COURSE_DAY + INTEGER_TYPE + COMMA_SEP +
                COURSE_START_TIME + TEXT_TYPE + COMMA_SEP +
                COURSE_END_TIME + TEXT_TYPE + COMMA_SEP +
                COURSE_TYPE + TEXT_TYPE + COMMA_SEP +
                COURSE_TEACHER + TEXT_TYPE + COMMA_SEP +
                IS_READY + INTEGER_TYPE + COMMA_SEP +
                COURSE_PLACE + TEXT_TYPE + " )";
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        
        public static final String DELETE_ALL_COURSE_CLASSES(int course){
        	return "DELETE FROM "+TABLE_NAME+" WHERE "+COURSE_ID+"="+course;
        }
        public static final String DELETE_COURSE_CLASS(int which){
        	return "DELETE FROM "+TABLE_NAME+" WHERE "+BaseColumns._ID+"="+which;
        }
        
        public static final String SELECT_ALL_CLASS_FOR_ALL_COURSES = "SELECT " +
    			"a."+DatabaseContract.CourseClassEntry.COURSE_ID +
    			", a."+BaseColumns._ID+
    			", a."+DatabaseContract.CourseClassEntry.COURSE_DAY +
    			", a."+DatabaseContract.CourseClassEntry.IS_READY +
    			", a."+DatabaseContract.CourseClassEntry.COURSE_START_TIME +
    			", a."+DatabaseContract.CourseClassEntry.COURSE_END_TIME +
    			", a."+DatabaseContract.CourseClassEntry.COURSE_PLACE +
    			", a."+DatabaseContract.CourseClassEntry.COURSE_TEACHER +
    			", a."+DatabaseContract.CourseClassEntry.COURSE_TYPE +
    			" FROM "+DatabaseContract.CourseClassEntry.TABLE_NAME+" a, "+DatabaseContract.CourseEntry.TABLE_NAME+" b "
    			+ "WHERE a."+DatabaseContract.CourseClassEntry.COURSE_ID+"=b."+BaseColumns._ID;
        
        public static final String SELECT_ALL_CLASS_FOR_ALL_COURSES (int dayId){
        	return SELECT_ALL_CLASS_FOR_ALL_COURSES+" AND a."+COURSE_DAY+"="+dayId;
        }
	}
	
	/**
	 * Table used to store priority queued info about user. Like preferred faculty, or block vs semester structure
	 * @author Andreas
	 *
	 */
	public static abstract class UserPreferenceEntry implements BaseColumns {
        public static final String TABLE_NAME = "user_preference";
        public static final String PREFERENCE_NAME = "key";
        public static final String PREFERENCE_VALUE = "value";
        
        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                PREFERENCE_NAME + TEXT_TYPE + COMMA_SEP +
                PREFERENCE_VALUE + TEXT_TYPE + " )";
        
        public static final String DELETE_PREFERENCE(int which){
        	return "DELETE FROM "+TABLE_NAME+" WHERE "+BaseColumns._ID+"="+which;
        }
        
        /**
         * 
         * @param key Please don't make sql injections with the key param
         * @param toKeep How many values to keep in DB
         * @return
         */
        public static final String DELETE_OBSELETE_PREFERENCES(String key, int toKeep){
        	return "DELETE FROM "+TABLE_NAME+" WHERE "+PREFERENCE_NAME+"='"+key+"' AND "+BaseColumns._ID+" NOT IN (SELECT "+BaseColumns._ID+" FROM "+TABLE_NAME+"" +
        			" WHERE "+PREFERENCE_NAME+"='"+key+"' ORDER BY "+BaseColumns._ID+" DESC LIMIT 0,"+toKeep+")";
        }
                
        public static final String SELECT_PREFERENCE_VALUES(String key){
        	return "SELECT * FROM "+TABLE_NAME+" WHERE "+PREFERENCE_NAME+"='"+key+"' ORDER BY "+BaseColumns._ID+" DESC";
        }
	}
}
