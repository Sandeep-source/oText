package com.sk.mymassenger.db.block;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sk.mymassenger.db.recent.Recent;

import java.util.List;

@Dao
public interface BlockDao {
    @Query( "select * from tbl_block where m_user_id=:mUserId and o_user_id=:oUserId " )
    Block getBlock(String mUserId,String oUserId);
    @Query( "select * from tbl_block where m_user_id=:mUserId and o_user_id=:oUserId and type=:type" )
    Block getBlockByMode(String mUserId,String oUserId,String type);
    @Insert
    void insert(Block block);
    @Query( "delete from tbl_block where o_user_id=:oUserId and m_user_id=:mUserId and type=:type" )

    void delete(String mUserId,String oUserId,String type);
    @Query( "delete from tbl_block" )
    void deleteAll();

    @Query( "select * from tbl_block where m_user_id=:mUserId and o_user_id=:oUserId " )
    LiveData<Block> getBlockLiveData(String mUserId,String oUserId);
}
