package com.example.cse5351;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.cse5351.constants.Constants;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "cse535.sqlite";
    private static final String SUPER_TABLE = "ONLY_TABLE";
    private static final int DB_VERSION = 1;
    // class specific values
    private static String timestamp;
    private String heart_rate, respiratory_rate;
    private Map<String, String> map = new HashMap<>();

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        SQLiteDatabase db = this.getWritableDatabase();
        if(TextUtils.isEmpty(timestamp)){
            this.timestamp = Instant.now().toString();
        }
    }

    public void setMap(Map<String, String> map) {this.map = new HashMap<>(map);}
    public void setRespiratory_rate(String val) {this.respiratory_rate = val;}
    public void setHeart_rate(String val) {this.heart_rate = val;}

    public void addItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timestamp", this.timestamp);
        values.put(Constants.HEART_RATE_COL, this.heart_rate);
        values.put(Constants.RESPIRATORY_RATE_COL, this.respiratory_rate);
        values.put(Constants.Cough, map.get(replaceUnderscoresWithSpaces(Constants.Cough)));
        values.put(Constants.Fever, map.get(replaceUnderscoresWithSpaces(Constants.Fever)));
        values.put(Constants.Diarrhea, map.get(replaceUnderscoresWithSpaces(Constants.Diarrhea)));
        values.put(Constants.Feeling_tired, map.get(replaceUnderscoresWithSpaces(Constants.Feeling_tired)));
        values.put(Constants.Headache, map.get(replaceUnderscoresWithSpaces(Constants.Headache)));
        values.put(Constants.Loss_of_smell_or_taste, map.get(replaceUnderscoresWithSpaces(Constants.Loss_of_smell_or_taste)));
        values.put(Constants.Muscle_Ache, map.get(replaceUnderscoresWithSpaces(Constants.Muscle_Ache)));
        values.put(Constants.Shortness_of_Breath, map.get(replaceUnderscoresWithSpaces(Constants.Shortness_of_Breath)));
        values.put(Constants.Soar_Throat, map.get(replaceUnderscoresWithSpaces(Constants.Soar_Throat)));
        values.put(Constants.Nausea, map.get(replaceUnderscoresWithSpaces(Constants.Nausea)));

        // update entry with same primary key on conflict
        db.insertWithOnConflict(SUPER_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public String replaceUnderscoresWithSpaces(String input) {
        // Use the replaceAll method with a regular expression to replace underscores with spaces
        String result = input.replaceAll("_", " ");
        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        /**
         * create rates table with timestamp primary key and the following columns
         */
        String tableCreation = "CREATE TABLE "+ SUPER_TABLE + "("
                + "timestamp" + " TEXT PRIMARY KEY,"
                + Constants.HEART_RATE_COL + " TEXT,"
                + Constants.RESPIRATORY_RATE_COL + " TEXT,"
                + Constants.Cough + " TEXT,"
                + Constants.Fever + " TEXT,"
                + Constants.Diarrhea + " TEXT,"
                + Constants.Feeling_tired + " TEXT,"
                + Constants.Headache + " TEXT,"
                + Constants.Loss_of_smell_or_taste + " TEXT,"
                + Constants.Muscle_Ache + " TEXT,"
                + Constants.Shortness_of_Breath + " TEXT,"
                + Constants.Soar_Throat + " TEXT,"
                + Constants.Nausea + " TEXT)";

        sqLiteDatabase.execSQL(tableCreation);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // for now since there is no newer version planned, we can keep this as empty
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+SUPER_TABLE);
        onCreate(sqLiteDatabase);
    }
}
