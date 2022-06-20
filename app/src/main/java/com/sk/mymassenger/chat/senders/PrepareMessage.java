package com.sk.mymassenger.chat.senders;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class PrepareMessage{

    private static final String TAG = "Prepare Message";

    private Message message;
     public Message getMessage(){
         return  message;
     }
    private boolean blocked;



    private Context context;


    public PrepareMessage(Message message,boolean blocked, Context context) {

        this.blocked = blocked;
        this.context = context;
        this.message=message;

    }

    public void start() {
        new Run().start();

    }

    public void setBlocked(boolean b) {
        Log.d(TAG, "setBlocked: Blocked "+b);

        this.blocked=b;
    }

    public void setReplyOf(String msgId) {
         message.setReplyOf(msgId);
    }


    private  class Run extends Thread{
       public void run() {
           Log.d(TAG, "run: Started process");
           User rec=OTextDb.getInstance( context ).userDao().getUser( message.getOUserId() );
           long time= new Date( ).getTime() ;
           String msgId=message.getMsgId();
           message.setTime( time );

           OTextDb.getInstance( context ).messageDao().insertAll( message );

               Recent recent=new Recent();
               recent.setMsg(message.getMessage() );
               recent.setType( Database.Recent.TYPE_GROUP );
               recent.setLocalId(String.valueOf(rec.getLocalId()));
               recent.setMUserId( message.getMUserId() );
               recent.setOUserId(message.getOUserId());
               recent.setName(rec.getUserName());
               recent.setMsgId(message.getMsgId());
               recent.setRecentMode(rec.getUserMode() );
               recent.setMediaType( Database.Recent.MEDIA_TEXT );
               recent.setTime(time);
               recent.setStatus( Database.Recent.STATUS_SEEN );
               OTextDb.getInstance( context ).recentDao().insert( recent );
           Log.d(TAG, "run: Messege inserted : "+message.getMessage());
           if(!blocked) {
               Log.d(TAG, "run: Not blocked ");
               HashMap<String, Object> valueforserver = new HashMap<>();
               HashMap<String, Object> val = new HashMap<>();
               val.put( String.valueOf( time ), message.getMessage() );
               valueforserver.put( message.getMsgId(), val );
               HashMap<String, Object> status_info = new HashMap<>();
               status_info.put( rec.getUserId(), Arrays.asList( time, Database.Msg.STATUS_SENT ) );
               valueforserver.put(message.getMsgId(),new MessageData(message,true) );
               Log.d(TAG, "run: Sending Message");
               FirebaseFirestore.getInstance().collection( "Messages" ).document( message.getMUserId()+ "to" +message.getOUserId() ).set( valueforserver, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
                   Log.d(TAG, "run: Sent to server ");
                   new Thread(()->{
                   Message message1=OTextDb.getInstance(context).messageDao().get(message.getMUserId(),message.getOUserId(),msgId);
                   message1.setStatus(Database.Msg.STATUS_SENT);
                    OTextDb.getInstance( context ).messageDao().insertAll( message1 );
                       Log.d(TAG, "run: saved to local storage ");
                }).start();
               } );
               valueforserver.clear();
               val.clear();
               val.put( message.getMsgId(), time );
               valueforserver.put( message.getMUserId(), val );
               Log.d(TAG, "run: receiver info updated ");
               FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + rec.getUserId()).set( valueforserver, SetOptions.mergeFields( message.getMUserId() ) );
           }else{
               Log.d(TAG, "run: not run due to blocking.");
           }
       }
   }
}
