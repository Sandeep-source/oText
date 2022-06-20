package com.sk.mymassenger.db.status;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.LinkedList;
import java.util.List;

@Dao
public interface StatusDao {
    @Query("select * from tbl_status where userId=:userId")
    LiveData<List<Status>> getStatus(String userId);

    @Query("select * from tbl_status where statusId=:statusId")
    Status getStatusById(String statusId);

    @Query("delete from tbl_status where statusId=:statusId")
    void delete(String statusId);

    @Delete
    void delete(Status status);

    @Query("delete from tbl_status where userId=:userId and statusId not in (:statusIds)")
    void deleteNotPresent(String userId,List<String> statusIds);

    @Insert(onConflict=OnConflictStrategy.REPLACE)
    long setStatus(Status status);

    @Query("select * from tbl_status where userId=:userId order by statusTime desc limit 1")
    Status getLastStatus(String userId);

    @Query("select * from tbl_status")
    List<Status> getAll();

    @Query("delete from tbl_status where userId=:userId ")
    void deleteAll(String userId);
}
