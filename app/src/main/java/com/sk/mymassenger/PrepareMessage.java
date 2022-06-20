package com.sk.mymassenger;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class PrepareMessage{

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
        blocked=b;
    }


    private  class Run extends Thread{
       public void run() {
           User rec=OTextDb.getInstance( context ).userDao().getUser( message.getOUserId() );
           long time= new Date( ).getTime() ;
           message.setTime( time );

           OTextDb.getInstance( context ).messageDao().insertAll( message );
         /*  ContentValues contentValuesMsg=new ContentValues(  );
           contentValuesMsg.put( Database.Msg.MSG, finalMsg );
           contentValuesMsg.put( Database.Msg.TYPE, Database.Msg.TYPE_SENT );
           contentValuesMsg.put(Database.Msg.OUSER_ID,recieverid);
           contentValuesMsg.put(Database.Msg.MUSER_ID,userid);
           contentValuesMsg.put( Database.Msg.MSG_ID,newmsgid);
           contentValuesMsg.put( Database.Msg.TIME, time);
           contentValuesMsg.put( Database.Msg.MSG_MODE,userMode );
           contentValuesMsg.put( Database.Msg.STATUS, Database.Msg.STATUS_NOT_SENT);
           contentValuesMsg.put( Database.Msg.MEDIA_TYPE, Database.Msg.MEDIA_TEXT );
           writable.insert(Database.Msg.TBL_MSG,null,contentValuesMsg);
           MessageData msg=new MessageData(newmsgid,finalMsg,userid,recieverid, Database.Msg.TYPE_SENT,time,Database.Msg.STATUS_NOT_SENT,time, Database.Msg.MEDIA_TEXT,userMode, Database.Msg.MEDIA_STATUS_DOWNLOADED);
           data.add( msg );
           adapter.setMsgs(data);
           handler.post(()-> {
                adapter.notifyDataSetChanged();

               recyclerView.scrollToPosition( adapter.getMsgs().size()-1 );
           });
           Cursor ct=readable.query( Database.Recent.TBL_RECENT,null, Database.Msg.OUSER_ID+" = ? AND "+Database.Msg.MUSER_ID+" = ? ",new String[]{recieverid,userid}
                   ,null,null,null);
           if(ct.getCount()<1) {
               ct.close();



               ContentValues contentValues = new ContentValues();




          */
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
 /*              if(finalMsg.length()>20)
                   contentValues.put( Database.Recent.MSG, finalMsg.substring( 0,20 )+"..." );
               else
                   contentValues.put( Database.Recent.MSG, finalMsg );
               contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_GROUP );
               contentValues.put( Database.Recent.LOCAL_ID, Recieverlocalid );
               contentValues.put( Database.Recent.MUSER_ID, userid );
               contentValues.put( Database.Recent.OUSER_ID ,recieverid);
               contentValues.put( Database.Recent.NAME, Recievername );
               contentValues.put( Database.Recent.MSG_ID, newmsgid );
               contentValues.put( Database.Recent.RECENT_MODE,userMode );
               contentValues.put( Database.Recent.MEDIA_TYPE, Database.Recent.MEDIA_TEXT );
               contentValues.put( Database.Recent.TIME,time );
               contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_SEEN );
               writable.insert( Database.Recent.TBL_RECENT, null, contentValues );
               handler.post( ()->System.out.println("Recent Inserted in conversation"));
           }else{
               ContentValues contentValues = new ContentValues();
               Recent recent=new Recent();
               recent.setMsg( finalMsg );
               recent.setType( Database.Recent.TYPE_USER );
               recent.setLocalId( Recieverlocalid );
               recent.setMUserId( userid );
               recent.setOUserId( recieverid );
               recent.setName( Recievername );
               recent.setMsgId( newmsgid );
               recent.setRecentMode( userMode );
               recent.setMediaType( Database.Recent.MEDIA_TEXT );
               recent.setTime( time );
               recent.setStatus( Database.Recent.STATUS_SEEN );
               OTextDb.getInstance( context ).recentDao().insert( recent );
               if(finalMsg.length()>20) {
                   String tmpmsg=finalMsg.substring( 0, 20 ) + "...";
                   contentValues.put( Database.Recent.MSG, tmpmsg.replaceAll( "\n","" ) );
               }
               else
                   contentValues.put( Database.Recent.MSG, finalMsg );
               contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_USER);
               contentValues.put( Database.Recent.LOCAL_ID, Recieverlocalid );
               contentValues.put( Database.Recent.MUSER_ID, userid );
               contentValues.put( Database.Recent.OUSER_ID ,recieverid);
               contentValues.put( Database.Recent.NAME, Recievername );
               contentValues.put( Database.Recent.RECENT_MODE,userMode );

               contentValues.put( Database.Recent.MSG_ID, newmsgid );

               contentValues.put( Database.Recent.MEDIA_TYPE, Database.Recent.MEDIA_TEXT );
               contentValues.put( Database.Recent.TIME, String.valueOf( new Timestamp( new Date().getTime() ) ) );
               contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_SEEN);
               writable.update( Database.Recent.TBL_RECENT,contentValues,
                       Database.Recent.OUSER_ID+" = ? AND "+ Database.Recent.MUSER_ID+" = ?"
                       ,new String[]{recieverid,userid});
               handler.post( ()->System.out.println("Recent Updated in conversation"));
           }


  */
           if(!blocked) {
               HashMap<String, Object> valueforserver = new HashMap<>();
               HashMap<String, Object> val = new HashMap<>();
               val.put( String.valueOf( time ), message.getMessage() );
               valueforserver.put( message.getMsgId(), val );
               HashMap<String, Object> status_info = new HashMap<>();
               status_info.put( rec.getUserId(), Arrays.asList( time, Database.Msg.STATUS_SENT ) );
               FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + message.getMUserId() ).set( status_info, SetOptions.mergeFields( rec.getUserId() ) );

               valueforserver.put(message.getMsgId(),new MessageData(message) );

               FirebaseFirestore.getInstance().collection( "Messages" ).document( message.getMUserId()+ "to" +message.getOUserId() ).set( valueforserver, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
                   /*ContentValues contentValuesMsg1 = new ContentValues();
                   contentValuesMsg1.put( Database.Msg.STATUS, Database.Msg.STATUS_SENT );
                   new oTextDb( context, Database.O_TEXT_DB, null, Database.DB_VERSION ).getWritableDatabase().update( Database.Msg.TBL_MSG, contentValuesMsg1, Database.Msg.MSG_ID + " = ? AND " +
                           Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ?", new String[]{newmsgid, recieverid, userid} );
                   Intent intent = new Intent( Database.DB_DATA_CHANGED );
                   intent.putExtra( "User", recieverid );
                   context.sendBroadcast( intent );*/
                   message.setStatus( Database.Msg.STATUS_SENT );
                   OTextDb.getInstance( context ).messageDao().update( message );
               } );
               valueforserver.clear();
               val.clear();
               val.put( message.getMsgId(), time );
               valueforserver.put( message.getMUserId(), val );

               FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + rec.getUserId()).set( valueforserver, SetOptions.mergeFields( message.getMUserId() ) );
           }
       }
   }
}
