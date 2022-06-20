package com.sk.mymassenger.db.message;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("Select * From tbl_messages where m_user_id=:mUserId and o_user_id=:oUserId and msg_mode=:mode order by time ")
    LiveData<List<Message>> getAll(String mUserId,String oUserId,String mode);

    @Query("Select * From tbl_messages where m_user_id=:mUserId and status=:status")
    List<Message> getFailed(String mUserId,String status);

    @Query("select * from tbl_messages where m_user_id=:mUserId and msg_id=:msgId")
    Message getMessage(String mUserId,String msgId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(Message ... msg);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(Message msg);

    @Delete
    int delete(Message msg);

    @Query("delete from tbl_messages where m_user_id=:mUserId and o_user_id=:oUserId and msg_id=:msgId")
    int deleteMessage(String mUserId,String oUserId,String msgId);

    @Query("Select * From tbl_messages where m_user_id=:mUserId and o_user_id=:oUserId and msg_id=:msgId")
    Message get(String mUserId,String oUserId,String msgId);

    @Query( "delete from tbl_messages where m_user_id=:mUserId and msg_mode=:mode and o_user_id=:oUserId" )
    int deleteAll(String mUserId , String mode,String oUserId);

    @Query( "update tbl_messages set status=:status where status==:preStatus and m_user_id=:mUserId and time<:time and o_user_id=:oUserId" )
    void updateStatus(String mUserId , String oUserId,String time,String preStatus,String status);
}
