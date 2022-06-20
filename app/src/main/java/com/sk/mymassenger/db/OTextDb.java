package com.sk.mymassenger.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.block.BlockDao;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.recent.RecentDao;
import com.sk.mymassenger.db.status.Status;
import com.sk.mymassenger.db.status.StatusDao;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.db.user.UserDao;

@Database(
        entities = { User.class, Recent.class,Block.class, Message.class, Status.class},
        version = 1,
        exportSchema = true)
public abstract class OTextDb extends RoomDatabase {
    private static OTextDb instance;

    public static OTextDb getInstance(Context context) {
        if(instance==null){
            instance= Room.databaseBuilder(context,OTextDb.class,"otext").fallbackToDestructiveMigration().build();
        }
        return instance;
    }
    public abstract UserDao userDao();
    public abstract RecentDao recentDao();
    public abstract BlockDao blockDao();
    public abstract MessageDao messageDao();
    public abstract StatusDao statusDao();

}
