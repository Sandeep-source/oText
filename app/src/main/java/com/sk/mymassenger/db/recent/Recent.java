package com.sk.mymassenger.db.recent;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tbl_recent",primaryKeys = {"m_user_id","o_user_id"})
public class Recent {
    @ColumnInfo(name = "type")
    private String type;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "msg_id")
    private  String msgId;

    @ColumnInfo(name = "msg")
    private  String msg;
    @ColumnInfo(name="time")
    private long time;
    @ColumnInfo(name="local_id")
    private String localId;
    @ColumnInfo(name="m_user_id")
    @NonNull
    private String mUserId;
    @NonNull
    @ColumnInfo(name = "o_user_id")
    private String oUserId;
    @ColumnInfo(name = "time_seen")
    private long timeSeen;
    @ColumnInfo(name = "status")
    private String status;
    @ColumnInfo(name = "media_type")
    private String mediaType;

    @ColumnInfo(name = "recent_mode")
    private String recentMode;


    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getRecentMode() {
        return recentMode;
    }

    public void setRecentMode(String recentMode) {
        this.recentMode = recentMode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getMUserId() {
        return mUserId;
    }

    public void setMUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public String getOUserId() {
        return oUserId;
    }

    public void setOUserId(String oUserId) {
        this.oUserId = oUserId;
    }

    public long getTimeSeen() {
        return timeSeen;
    }

    public void setTimeSeen(long timeSeen) {
        this.timeSeen = timeSeen;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

}
