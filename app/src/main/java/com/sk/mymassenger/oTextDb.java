package com.sk.mymassenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class oTextDb extends SQLiteOpenHelper {
    public oTextDb(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super( context, name, factory, version );
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
      /*  sqLiteDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS "+ Database.User.TBL_USER
                +" ( "+
                        Database.User.LOCAL_ID +" INTEGER PRIMARY KEY AUTOINCREMENT," +
                        Database.User.USER_ID +" TEXT UNIQUE,"+
                        Database.User.USER_NAME+" TEXT,"+
                        Database.User.PHONE_NO+" TEXT,"+
                        Database.User.EMAIL+" TEXT,"+
                        Database.User.PROFILE_PICTURE+" TEXT,"+
                        Database.User.USER_MODE+" TEXT,"+
                        Database.User.TYPE+" TEXT"+")"
        );
        sqLiteDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS "+ Database.Block.TBL_BLOCK
                        +" ( "+
                        Database.Block.MUSER_ID +" TEXT ," +
                        Database.Block.OUSER_ID +" TEXT," +
                        Database.Block.TYPE +" TEXT)"
        );

        sqLiteDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS "+ Database.Msg.TBL_MSG
                        +" ( "+
                        Database.Msg.MSG_ID +" TEXT ," +
                        Database.Msg.MSG +" TEXT,"+
                        Database.Msg.MUSER_ID+" TEXT,"+
                        Database.Msg.OUSER_ID+" TEXT,"+
                        Database.Msg.TYPE+" TEXT,"+
                        Database.Msg.TIME+" TEXT,"+
                        Database.Msg.STATUS+" TEXT,"+
                        Database.Msg.TIME_SEEN+" TIMESTAMP,"+
                        Database.Msg.MEDIA_TYPE+" TEXT,"+
                        Database.Msg.MSG_MODE+" TEXT,"+
                        Database.Msg.MEDIA_STATUS+" TEXT"
                        +")"
        );
        sqLiteDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS "+ Database.Recent.TBL_RECENT
                        +" ( "+
                        Database.Recent.TYPE +" TEXT," +

                        Database.Recent.MSG +" TEXT,"+
                        Database.Recent.NAME+" TEXT,"+
                        Database.Recent.MSG_ID+" TEXT,"+
                         Database.Recent.TIME+" TIMESTAMP,"+
                        Database.Recent.LOCAL_ID+" TEXT,"+
                        Database.Recent.MUSER_ID+" TEXT ,"+
                        Database.Recent.OUSER_ID+" TEXT ,"+
                        Database.Recent.TIME_SEEN+" TIMESTAMP,"+
                        Database.Recent.STATUS+" TEXT,"+
                        Database.Recent.MEDIA_TYPE+" TEXT,"+
                        Database.Recent.RECENT_MODE+" TEXT"
                        +")"
        );
*/

    }




    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int old_ver, int new_ver) {
      /*   sqLiteDatabase.execSQL( "DROP TABLE IF EXISTS "+ Database.User.TBL_USER );
        sqLiteDatabase.execSQL( "DROP TABLE IF EXISTS "+ Database.Recent.TBL_RECENT);
        sqLiteDatabase.execSQL( "DROP TABLE IF EXISTS "+ Database.Msg.TBL_MSG);
        onCreate( sqLiteDatabase );

       */
    }


    public void insert(String table, ContentValues values){
        getWritableDatabase().insert(table,null,values);
    }
}
