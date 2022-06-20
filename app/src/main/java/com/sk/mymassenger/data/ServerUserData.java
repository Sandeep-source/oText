package com.sk.mymassenger.data;



public class ServerUserData {
    private String email;
    private String phone;
    private String userId;

    public long getStatusCount() {
        return statusCount;
    }

    public ServerUserData() {

    }



    public void setStatusCount(long statusCount) {
        this.statusCount = statusCount;
    }

    private long statusCount;
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private String userName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getFreeTime() {
        return freeTime;
    }

    public void setFreeTime(String freeTime) {
        this.freeTime = freeTime;
    }

    public Mood getMood() {
        if(mood==null){
            mood=new Mood();
        }
        return mood;
    }

    public void setMood(Mood mood)
    {

        this.mood = mood;
    }

    public Changed getChanged() {
        if(changed==null){
            changed= new Changed();
        }
        return changed;
    }

    public void setChanged(Changed changed) {
        this.changed = changed;
    }

    private String profile;
    private String freeTime;
    private Mood mood;
    private Changed changed;

    public static class Mood {
        public Mood(){

        }

        public Mood(long since, String value) {
            this.since = since;
            this.value = value;
        }

        private long since;

        public long getSince() {
            return since;
        }

        public void setSince(long since) {
            this.since = since;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        private String value;
    }

    public static class Changed {
        public Changed(long lastChanged, long status, long profile) {
            this.lastUpdated = lastChanged;
            this.status = status;
            this.profile = profile;
        }
        public Changed(){}
        private long lastUpdated;
        private long status;

        public long getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(long lastChanged) {
            this.lastUpdated = lastChanged;
        }

        public long getStatus() {
            return status;
        }

        public void setStatus(long status) {
            this.status = status;
        }

        public long getProfile() {
            return profile;
        }

        public void setProfile(long profile) {
            this.profile = profile;
        }

        private long profile;
    }
}
