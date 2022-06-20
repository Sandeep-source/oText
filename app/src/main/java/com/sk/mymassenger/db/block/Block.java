package com.sk.mymassenger.db.block;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tbl_block")
public class Block {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "_id")
    private long id;
    @ColumnInfo(name = "o_user_id")
    private String oUserId;
    @ColumnInfo(name = "m_user_id")
    private String mUserId;
    @ColumnInfo(name = "type")
    private String type;
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOUserId() {
        return oUserId;
    }

    public void setOUserId(String oUserId) {
        this.oUserId = oUserId;
    }

    public String getMUserId() {
        return mUserId;
    }

    public void setMUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
