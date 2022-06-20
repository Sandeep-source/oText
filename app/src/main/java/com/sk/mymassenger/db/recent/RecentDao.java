package com.sk.mymassenger.db.recent;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.work.Data;

import java.util.List;

@Dao
public interface RecentDao {

     @Query( "select * from tbl_recent where m_user_id=:mUserId and recent_mode=:mode" )
     LiveData<List<Recent>> getRecent(String mUserId,String mode);


     @Query( "select * from tbl_recent where m_user_id=:mUserId and o_user_id=:oUserId limit 1" )
     Recent getSingleRecent(String mUserId,String oUserId);

     @Query("update tbl_recent set recent_mode=:mode where m_user_id=:mUserId and type=:type")
     void updateMode(String mUserId,String type,String mode);

     @Insert(onConflict = OnConflictStrategy.REPLACE)
     long insert(Recent recent);

     @Update
     void update(Recent recent);

     @Query( "Update tbl_recent set status=:status where m_user_id=:mUserId and o_user_id=:oUserId" )
     void updateStatus(String mUserId ,String oUserId,String status);

     @Query( "delete from tbl_recent where m_user_id=:mUserId and recent_mode=:mode and o_user_id=:oUserId" )
     int deleteAll(String mUserId , String mode,String oUserId);
}
