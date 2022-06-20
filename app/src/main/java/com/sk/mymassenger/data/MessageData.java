package com.sk.mymassenger.data;

import android.util.Log;

import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;

import java.util.Map;

public class MessageData {
    public String getReact() {
        return react;
    }

    public void setReact(String react) {
        this.react = react;
    }

    private  String react;

    public static Recent getRecent() {
        return recent;
    }

    private static Recent recent;

    public String getReplyOf() {
        return replyOf;
    }

    public void setReplyOf(String replyOf) {
        this.replyOf = replyOf;
    }

    private  String replyOf;
    private String msgId;
    private String message;
    private String type;


    private long time;
    private String status;

    public String getMsgId() {
        return msgId;
    }

    public String getMessage() {
        return message;
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


    public String getMediaType() {
        return mediaType;
    }

    public String getMsgMode() {
        return msgMode;
    }

    private String mediaType;
    private String msgMode;
    private String mediaStatus;

    public String getMUserId() {
        return mUserId;
    }

    public String getOUserId() {
        return oUserId;
    }

    private String mUserId;
    private String oUserId;

    public MessageData(Message messageData,boolean change) {
        this.msgId = messageData.getMsgId();
        this.message = messageData.getMessage();
        this.time = messageData.getTime() ;
        this.status ="not_seen";
        this.replyOf=messageData.getReplyOf();
        this.mediaType = messageData.getMediaType();
        this.msgMode = messageData.getMsgMode();
        this.mediaStatus = messageData.getMediaStatus();
        this.react=messageData.getReact();
        if(change){
            this.mUserId=messageData.getMUserId();
            this.oUserId=messageData.getOUserId();
            this.type= Database.Msg.TYPE_REC;
        }else{
            this.mUserId=messageData.getOUserId();
            this.oUserId=messageData.getMUserId();
            if(messageData.getType().equals(Database.Msg.TYPE_SENT)){
                this.type= Database.Msg.TYPE_REC;
            }else{
                this.type= Database.Msg.TYPE_SENT;
            }
        }
    }
  public static Message toMessage(Map<String, Object> map,String userMode){
        String value= map.get("message").toString();
        String msgid=map.get("msgId").toString();
        Object obj=map.get("time");
        Log.d( "TAG", "toMessage: obj=" +obj+" value="+value+" msgId="+msgid);
        String mediaType= map.get("mediaType").toString();
        Long time= (Long) map.get("time");

        recent=new Recent();
        recent.setMsg( value );
        recent.setType( Database.Recent.TYPE_USER );
         String mUserId= (String) map.get("ouserId");
         String oUserId= (String) map.get("muserId");
        recent.setMUserId(mUserId  );
        recent.setOUserId( oUserId );

        recent.setMsgId( msgid );
        recent.setTime( time );
        recent.setRecentMode( userMode );
        recent.setMediaType( mediaType);
        recent.setStatus( Database.Recent.STATUS_NOT_SEEN );
        Message message=new Message();
        message.setMessage( value );
        message.setReact((String) map.get("react"));
        message.setMsgId( msgid );
        message.setMUserId( mUserId );
        message.setOUserId( oUserId );
        message.setType((String) map.get("type"));
        message.setReplyOf( (String) map.get("replyOf") );
        message.setTime(time );
        message.setStatus(Database.Recent.STATUS_NOT_SEEN);
        message.setMsgMode( userMode);
        message.setMediaType( mediaType  );

        message.setMediaStatus( "media_status_not_downloaded"   );
        return message;
  }


    public void setMessage(String message) {
        this.message=message;
    }
}
