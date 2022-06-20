package com.sk.mymassenger.db.status;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Map;

@Entity(tableName = "tbl_status")
public class Status {
    private static final String TAG = "Status";
    public static final String STATUS_TYPE_IMAGE="status_image";

    public static final String STATUS_TYPE_VIDEO="status_video";

    public static final String MEDIA_STATUS_LOCAL="local_media";

    public static final String MEDIA_STATUS_NOT_LOCAL="not_local_media";
    public static final String STATUS_TYPE_AUDIO = "status_audio";

    @PrimaryKey
    @NonNull
    private String statusId;

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    @ColumnInfo(name = "statusText")
    String statusText;

    public String getMediaStatus() {
        return mediaStatus;
    }

    public void setMediaStatus(String mediaStatus) {
        this.mediaStatus = mediaStatus;
    }

    @ColumnInfo(name="mediaStatus")
    private String mediaStatus;

    @ColumnInfo(name = "statusType")
    private String statusType;

    @ColumnInfo(name = "statusUri")
    private String statusUri;

    public static Status toStatus(Map<String, Object> o) {
        Status status=new Status();
        status.setMediaStatus(MEDIA_STATUS_NOT_LOCAL);
        status.setStatusUri((String) o.get("statusId"));
        status.setStatusType((String) o.get("statusType"));
        Log.d(TAG, "toStatus: timeStatus"+o.get("statusTime"));
        status.setStatusTime((Long)o.get("statusTime"));
        status.setUserId((String) o.get("userId"));
        status.setStatusId((String) o.get("statusId"));
        status.setStatusText((String) o.get("statusText"));
      
        return status;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getStatusType() {
        return statusType;
    }

    public void setStatusType(String statusType) {
        this.statusType = statusType;
    }

    public String getStatusUri() {
        return statusUri;
    }

    public void setStatusUri(String statusUri) {
        this.statusUri = statusUri;
    }

    public long getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(long statusTime) {
        this.statusTime = statusTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    @ColumnInfo(name = "statusTime")
    private long statusTime;
    @ColumnInfo(name = "userId")
    private String userId;

}
