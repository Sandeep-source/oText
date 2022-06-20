package com.sk.mymassenger.db.message;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tbl_messages")
public class Message {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "_id")
    private long id;

    public long getTransferIndex() {
        return transferIndex;
    }

    public void setTransferIndex(long transferIndex) {
        this.transferIndex = transferIndex;
    }

    public String getReact() {
        return react;
    }

    public void setReact(String react) {
        this.react = react;
    }

    @ColumnInfo(name = "react")
    private String react;
    @ColumnInfo(name = "transferIndex")
    private long transferIndex;
    @ColumnInfo(name = "msg_id")
    private String msgId;
    @ColumnInfo(name = "msg")
    private String message;
    @ColumnInfo(name = "m_user_id")
    private String mUserId;
    @ColumnInfo(name = "o_user_id")
    private String oUserId;
    @ColumnInfo(name = "type")
    private String type;

    public String getReplyOf() {
        return replyOf;
    }

    public void setReplyOf(String replyOf) {
        this.replyOf = replyOf;
    }

    @ColumnInfo(name = "reply_of")
    private String replyOf;
    @ColumnInfo(name = "time")
    private long time;
    @ColumnInfo(name = "status")
    private String status;
    @ColumnInfo(name = "time_seen")
    private String timeSeen;
    @ColumnInfo(name = "media_type")
    private String mediaType;
    @ColumnInfo(name = "msg_mode")
    private String msgMode;
    @ColumnInfo(name = "media_status")
    private String mediaStatus;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getMessage() {
        return message;
    }

    public String getMUserId() {
        return mUserId;
    }

    public String getOUserId() {
        return oUserId;
    }

    public String getType() {
        return type;
    }

    public long getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getTimeSeen() {
        return timeSeen;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public void setOUserId(String oUserId) {
        this.oUserId = oUserId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimeSeen(String timeSeen) {
        this.timeSeen = timeSeen;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setMsgMode(String msgMode) {
        this.msgMode = msgMode;
    }

    public void setMediaStatus(String mediaStatus) {
        this.mediaStatus = mediaStatus;
    }

    public String getMsgMode() {
        return msgMode;
    }

    public String getMediaStatus() {
        return mediaStatus;
    }

    @NonNull
    @Override
    public String toString() {
        return getMsgId()+getMediaStatus()+getStatus()+getReact();
    }
}
