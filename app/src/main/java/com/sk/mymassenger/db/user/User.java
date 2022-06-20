package com.sk.mymassenger.db.user;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "tbl_users",primaryKeys = {"user_id","type"})
public class User implements Cloneable {
    @ColumnInfo(name = "localId")
    private long localId;
    @ColumnInfo(name = "user_id")
    @NonNull
    private String userId;
    @ColumnInfo(name = "user_name")
    private String userName;
    @ColumnInfo(name = "phone_no")
    private String phoneNo;
    @ColumnInfo(name = "email")
    private String email;

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @ColumnInfo(name = "last_updated")
    private long lastUpdated;
    @ColumnInfo(name = "profile_picture")
    private String profilePicture;
    @ColumnInfo(name = "user_mode")
    private String userMode;
    @ColumnInfo(name = "type")
    @NonNull
    private String type;

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getFreeTime() {
        return freeTime;
    }

    public void setFreeTime(String freeTime) {
        this.freeTime = freeTime;
    }

    @ColumnInfo(name = "mood",defaultValue = "😀")
    private  String mood;

    public long getMoodSince() {
        return moodSince;
    }

    public void setMoodSince(long moodSince) {
        this.moodSince = moodSince;
    }

    @ColumnInfo(name="mood_since",defaultValue = "0")
    private long moodSince;
    @ColumnInfo(name = "freeTime",defaultValue = "no time")
    private String freeTime;

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getUserMode() {
        return userMode;
    }

    public void setUserMode(String userMode) {
        this.userMode = userMode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User copy(){
        try {
            return (User) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
