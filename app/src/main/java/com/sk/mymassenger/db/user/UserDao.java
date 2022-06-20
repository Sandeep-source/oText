package com.sk.mymassenger.db.user;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {

    @Query("select * from tbl_users where user_id=:userId and type=:type")
    User getUserByType(String userId,String type);

    @Query( "select * from tbl_users where user_id=:userId " )
    User getUser(String userId);

    @Query( "select * from tbl_users where user_id=:userId " )
    LiveData<User> getLiveUser(String userId);

    @Query( "select * from tbl_users where user_id=:userId And user_mode=:userMode" )
    User getUserWithMode(String userId,String userMode);

    @Query( "select * from tbl_users where phone_no=:phone or email=:email" )
    User getUserByContactDetails(String phone,String email);

    @Query("select DIstinct * from tbl_users where (phone_no like '%' || :searchKey || '%' " +
            "or user_name like '%' || :searchKey || '%' " +
            "or email like '%' || :searchKey || '%') and  user_id!=:userId")
    List<User> searchUser(String searchKey,String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(User ... users);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(User user);

    @Query( "select * from tbl_users where type=:type" )
    List<User> getAllUser(String type);

}
