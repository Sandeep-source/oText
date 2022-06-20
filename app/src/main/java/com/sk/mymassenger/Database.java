package com.sk.mymassenger;


public class Database {
    public static final String FILE_ATHORITY="com.sk.mymassenger.fileProvider";

    public static final String DB_DATA_CHANGED="com.sk.mymassenger.otext_db_data_changed";
    public static final int DB_VERSION=1;
    public static final String O_TEXT_DB="otext";
    static  final class Server{
        public static final String EMAIL="Email";
        public static final String PHONE="Phone";
        public static final String USER_NAME="Username";
        public static final String PROFILE="Profile";
        public static final String USER_ID="User Id";
        public static final String BLOCK_BY_ME="block_by_me";
        public static final String BLOCK_BY_THEM="block_by_them";


    }
    static  final class Block{
        public static final String BLOCK_STATUS_CHANGED="tbl_block";
        public static final String TBL_BLOCK="tbl_block";
        public static final String OUSER_ID="ouser_id";
        public static final String MUSER_ID="muser_id";
        public static final String TYPE="type";
        public static final String BY_ME="by_me";
        public static final String BY_OTHER="by_other";


    }
    static final class User{
        public static final String TBL_USER="User";
        public static final String USER_ID="UserId";
        public static final String LOCAL_ID="LocalId";
        public static final String EMAIL="Email";
        public static final String PHONE_NO="phone";
        public static final String USER_NAME="username";
        public static final String TYPE="type";
        public static final String PROFILE_PICTURE="profile";
        public static final String TYPE_SELF="self";
        public static final String TYPE_FRIEND="friend";
        public static final String USER_MODE="user_mode";
        public static final String MODE_PUBLIC="mode_public";
        public static final String MODE_PRIVATE="mode_private";



    }
    public static final class Msg{
        public static final String TBL_MSG="tbl_msg";
        public static final String MSG_ID="msg_id";
        public static final String MSG="msg";
        public static final String MUSER_ID="muser_id";
        public static final String OUSER_ID="ouser_id";
        public static final String TYPE="type";
        public static final String MEDIA_TYPE="media_type";
        public static final String MEDIA_TEXT="media_text";
        public static final String MEDIA_FILE="media_file";
        public static final String MEDIA_IMG="media_img";
        public static final String EXTRA_MEDIA_DATA="extra_media_data";
        public static final String TYPE_SENT="sent";
        public static final String TIME="time";
        public static final String MSG_MODE="msg_mode";
        public static final String MEDIA_STATUS="media_status";
        public static final String MEDIA_STATUS_NOT_DOWNLOADED="media_status_not_downloaded";
        public static final String MEDIA_STATUS_DOWNLOADED="media_status_downloaded";
        public static final String MODE_PUBLIC="mode_public";
        public static final String MODE_PRIVATE="mode_private";
        public static final String TYPE_REC="rec";
        public static final String STATUS="status";
        public static final String STATUS_SEEN="seen";
        public static final String TIME_SEEN="seentime";
        public static final String STATUS_NOT_SENT="not_sent";
        public static final String STATUS_SENT="Status_sent";
        public static final String STATUS_NOT_SEEN="not_seen";



    }
    static final class GroupMember{
        public static final String MENBER_ID="member_id";
        public static final String LOCAL_ID="local_id";
        public static final String USER_ID="user_id";

    }
    static final class Group_MSG{
        public static final String MSG_ID="msg_id";
        public static final String BY="By";
        public static final String MSG="msg";
        public static final String TIME="time";
    }
   public static final class Recent{
        public static final String MUSER_ID="muser_id";
        public static final String OUSER_ID="ouser_id";
        public static final  String TBL_RECENT="recent";
        public static final  String MSG_ID="rc_id";
        public static final  String TYPE="type";
        public static final String TYPE_GROUP="group";
        public static final String TYPE_USER="user";
        public static final String MSG="msg";
        public static final String NAME="name";
        public static final String LOCAL_ID="local_id";
        public static final String STATUS="status";
        public static final String STATUS_SEEN="seen";
        public static final String TIME_SEEN="seentime";
        public static final String STATUS_NOT_SEEN="not_seen";
        public static final String TIME="time";

        public static final String RECENT_MODE="recent_mode";
        public static final String MODE_PUBLIC="mode_public";
        public static final String MODE_PRIVATE="mode_private";
        public static final String MEDIA_TYPE="media_type";
        public static final String MEDIA_TEXT="media_text";
        public static final String MEDIA_IMG="media_img";
        public static final String MEDIA_FILE="media_file";
    }



}
