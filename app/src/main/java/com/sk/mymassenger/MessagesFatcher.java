package com.sk.mymassenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MessagesFatcher extends Worker {
    private final SQLiteDatabase readable;
    private final SQLiteDatabase writable;
    private final CollectionReference Messages;
    private final String userid;
    private final String localid;
    final ArrayList<String> changed;
    private final Context context;
    int ids = 0;
    final Context activityCompat;
    private boolean failed = false;
    final boolean get;
    private HashMap<String,Object> dm;

    public MessagesFatcher(Context context, WorkerParameters params) {
        super( context, params );
        oTextDb db = new oTextDb( context, Database.O_TEXT_DB, null, Database.DB_VERSION );
        this.context = context;
        changed = new ArrayList<>();
        readable = db.getReadableDatabase();
        writable = db.getWritableDatabase();
        dm=new HashMap<>(  );
        this.activityCompat = context;
        Messages = FirebaseFirestore.getInstance().collection( "Messages" );
        this.userid = getInputData().getString( "UserId" );
        this.get = getInputData().getBoolean( "get", false );
       /* Cursor localdata = db.getReadableDatabase().query( Database.User.TBL_USER, null,
                Database.User.USER_ID + " = ? ", new String[]{userid}, null, null, null );

        */
        User localdata=OTextDb.getInstance( getApplicationContext() ).userDao().getUser( userid );
        this.localid = localdata.getUserId();
    }


    @NonNull
    @Override
    public Result doWork() {
        if (get) {
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + userid ).get().addOnSuccessListener( this::work ).addOnFailureListener( e -> failed = true );
        } else {
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + userid ).addSnapshotListener( (value, error) -> {
                if (value != null) {
                    work( value );
                }
            } );
        }
        if (failed) {
            return Result.retry();
        }
        return Result.success();
    }

    public void work(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot != null && documentSnapshot.exists()) {
            System.out.println( "Fatcher " + documentSnapshot );

            Map<String, Object> valuemap = documentSnapshot.getData();

            if (valuemap != null) {
                for (String User : valuemap.keySet()) {
                    Map<String, String> map = (Map<String, String>) valuemap.get( User );
                    String key = null;
                    if (map != null) {
                        LinkedList<String> list = new LinkedList<>( map.keySet() );
                        key = list.get( 0 );

                    }
                    System.out.println( key );
                    String Senderid = null;
                    Senderid=key.substring( 0,key.indexOf( "to" ) );

                    System.out.println( "key : " + key );
                    /*Cursor c = readable.query( Database.User.TBL_USER, null,
                            Database.User.USER_ID + " = ? ",
                            new String[]{User}, null, null, null );

                     */
                    User userD=OTextDb.getInstance( getApplicationContext() ).userDao().getUser( User );
                    if (userD==null) {
                        String finalSenderid = Senderid;
                        String finalKey = key;
                        FirebaseFirestore.getInstance().collection( "users" ).whereEqualTo( Database.Server.USER_ID, User ).get().addOnCompleteListener( task -> {
                             new UserDataFetcher( context,false ).getUserData( task );
                            /*Cursor cur = readable.query( Database.User.TBL_USER, null,
                                    Database.User.USER_ID + " = ? ",
                                    new String[]{User}, null, null, null );
                            cur.moveToFirst();

                             */
                            User user=OTextDb.getInstance( getApplicationContext()).userDao().getUser( User );
                            String userMode=user.getUserMode();
                            getMsg( finalSenderid, finalKey,  map , userMode);
                        } );
                    } else {

                         String userMode=userD.getUserMode();
                        getMsg( Senderid, key,  map, userMode );
                    }



                }
            }
        }


    }

    private void getMsg(String Senderid, String key, Map<String, String> map, String userMode) {

        System.out.println( "user : " + userid + " Sender id : " + Senderid );
        if (OTextDb.getInstance( getApplicationContext() ).messageDao().get( userid,Senderid,key )==null) {

            System.out.println( "Senderid : " + Senderid );
            System.out.println( key );
            System.out.println( Senderid + "to" + userid );

            String finalKey = key;
            String finalSenderid = Senderid;
            Messages.document( Senderid + "to" + userid ).get().addOnSuccessListener( (OnSuccessListener<DocumentSnapshot>) documentSnapshot1 -> {

                if (documentSnapshot1 != null) {
                    Map<String, Object> newdocvalue = documentSnapshot1.getData();
                    if (newdocvalue != null) {
                        for (String msgid : newdocvalue.keySet()) {
                            System.out.println( "msgidid : " + msgid );
                            if (OTextDb.getInstance( getApplicationContext() ).messageDao().get( userid, Senderid, key ) == null) {

                                Message message=MessageData.toMessage( (Map<String, Object>) newdocvalue.get( msgid ),userid,Senderid,userMode);
                                message.setMsgMode( userMode );
                                OTextDb.getInstance( getApplicationContext() ).messageDao().insertAll( message );
                                User sender=OTextDb.getInstance( getApplicationContext() ).userDao().getUser( Senderid );
                                Recent recent=MessageData.getRecent();
                                recent.setLocalId( String.valueOf( sender.getLocalId() ) );
                                recent.setName( sender.getUserName() );
                                OTextDb.getInstance( getApplicationContext() ).recentDao().insert( recent );
                                notify(Senderid,sender.getUserName(),recent.getMsg(),sender.getProfilePicture());
/*
                                for (Map.Entry<String, String> entry : msg.entrySet()) {
                                    Cursor c = readable.query( Database.User.TBL_USER, null,
                                            Database.User.USER_ID + " = ? ",
                                            new String[]{Senderid}, null, null, null );
                                    c.moveToFirst();
                                    String sendername=c.getString(2);
                                    String profilepath=c.getString( 5);
                                    c.close();
                                    String time = entry.getKey();
                                    String value = entry.getValue();
                                    if (value.endsWith( "(img)(/img)" )) {

                                        String url = value.substring( 0, value.indexOf( "(img)(/img)" ) );


                                        File dirfile=new File( getApplicationContext().getFilesDir(),"imagesThumb/");
                                        if(!dirfile.exists()){
                                            dirfile.mkdirs();
                                        }
                                        dirfile=new File( getApplicationContext().getFilesDir(),"receive/gifs/");
                                        if(!dirfile.exists()){
                                            dirfile.mkdirs();
                                        }
                                        File file=null;
                                        String status;
                                        String download="";
                                        System.out.println( "download : "+url );
                                        url=url.trim();
                                        if(url.endsWith( "gif" )) {
                                            file = new File( getApplicationContext().getFilesDir(), "gifs/GIF" + msgid + ".gif" );

                                        }else{
                                            file=new File( getApplicationContext().getFilesDir(),"imagesThumb/IMG"+msgid+".png"  );

                                        }
                                        Message message=new Message();
                                        message.setMessage( file.getAbsolutePath() );
                                        message.setType( Database.Msg.TYPE_REC );
                                        message.setMUserId( userid );
                                        message.setOUserId( finalSenderid );
                                        message.setMsgId( msgid );
                                       // message.setTime( time );
                                        message.setMsgMode( userMode );
                                        message.setMediaType( Database.Msg.MEDIA_IMG );
                                        message.setMediaStatus( Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED );
                                        message.setStatus( Database.Msg.STATUS_NOT_SEEN );
                                        OTextDb.getInstance( getApplicationContext() ).messageDao().insertAll( message );
                                         ContentValues contentValuesMsg = new ContentValues();
                                        contentValuesMsg.put( Database.Msg.MSG,file.getAbsolutePath());
                                        contentValuesMsg.put( Database.Msg.TYPE, Database.Msg.TYPE_REC );
                                        contentValuesMsg.put( Database.Msg.MUSER_ID, userid );
                                        contentValuesMsg.put( Database.Msg.OUSER_ID, finalSenderid );
                                        contentValuesMsg.put( Database.Msg.MSG_ID, msgid );
                                        contentValuesMsg.put( Database.Msg.TIME, time );
                                        contentValuesMsg.put( Database.Msg.MSG_MODE,userMode );
                                        contentValuesMsg.put( Database.Msg.MEDIA_TYPE, Database.Msg.MEDIA_IMG );
                                        contentValuesMsg.put( Database.Msg.MEDIA_STATUS, Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED );
                                        contentValuesMsg.put( Database.Msg.STATUS, Database.Msg.STATUS_NOT_SEEN );
                                        writable.insert( Database.Msg.TBL_MSG, null, contentValuesMsg );
                                        System.out.println( "download : "+url );

                                        if(url.endsWith( "png" )) {
                                            File finalFile = file;
                                            String finalUrl = url;
                                            FirebaseStorage.getInstance().getReference( "imagesThumb/" + url ).getFile( file).addOnSuccessListener( taskSnapshot -> {
                                                complete( finalFile, finalUrl,Senderid,sendername,msgid,time,userMode,profilepath );
                                            } );
                                        }
                                        else{
                                            complete( file,url,Senderid,sendername,msgid,time,userMode,profilepath );
                                        }
                                        System.out.println( "Key : " + time + " value :" + value );

                                    }else if (value.endsWith( "(file)(/file)" )) {
                                        String url = value.substring( 0, value.indexOf( "(file)(/file)" ) );

                                       // File file=new File( getApplicationContext().getFilesDir(),"file/"+url );

                                        Message message=new Message();
                                        message.setMessage("files/"+url);
                                        message.setType( Database.Msg.TYPE_REC );
                                        message.setMUserId( userid );
                                        message.setOUserId( finalSenderid );
                                        message.setMsgId( msgid );
                                     //   message.setTime( time );
                                        message.setMsgMode( userMode );
                                        message.setMediaType( Database.Msg.MEDIA_FILE );
                                        message.setMediaStatus( Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED );
                                        message.setStatus( Database.Msg.STATUS_NOT_SEEN );
                                        OTextDb.getInstance( getApplicationContext() ).messageDao().insertAll( message );
                                        ContentValues contentValuesMsg = new ContentValues();
                                        contentValuesMsg.put( Database.Msg.MSG,"files/"+url);
                                        contentValuesMsg.put( Database.Msg.TYPE, Database.Msg.TYPE_REC );
                                        contentValuesMsg.put( Database.Msg.MUSER_ID, userid );
                                        contentValuesMsg.put( Database.Msg.OUSER_ID, finalSenderid );
                                        contentValuesMsg.put( Database.Msg.MSG_ID, msgid );
                                        contentValuesMsg.put( Database.Msg.TIME, time );
                                        contentValuesMsg.put( Database.Msg.MSG_MODE,userMode );
                                        contentValuesMsg.put( Database.Msg.MEDIA_TYPE, Database.Msg.MEDIA_FILE );
                                        contentValuesMsg.put( Database.Msg.STATUS, Database.Msg.STATUS_NOT_SEEN );
                                        contentValuesMsg.put( Database.Msg.MEDIA_STATUS, Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED );
                                        writable.insert( Database.Msg.TBL_MSG, null, contentValuesMsg );
                                        HashMap<String, Object> status_info = new HashMap<>();
                                        status_info.put( userid, Arrays.asList( new Timestamp( new Date().getTime() ).toString(), Database.Msg.STATUS_NOT_SEEN ) );
                                        FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + Senderid ).set( status_info, SetOptions.mergeFields( userid ) );
                                        ContentValues contentValues = new ContentValues();
                                        Recent recent=new Recent();
                                        recent.setMsg( value );
                                        recent.setType( Database.Recent.TYPE_USER );
                                        recent.setLocalId( localid );
                                        recent.setMUserId( userid );
                                        recent.setOUserId( Senderid );
                                        recent.setName( sendername );
                                        recent.setMsgId( msgid );
                                      //  recent.setTime( time );
                                        recent.setRecentMode( userMode );
                                        recent.setMediaType( Database.Msg.MEDIA_FILE );
                                        recent.setStatus( Database.Recent.STATUS_NOT_SEEN );

                                        if(url.length()>20)
                                            contentValues.put( Database.Recent.MSG, url.substring( 0,20 )+"..." );
                                        else
                                            contentValues.put( Database.Recent.MSG, url);
                                        contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_USER );
                                        contentValues.put( Database.Recent.LOCAL_ID, localid );
                                        contentValues.put( Database.Recent.MUSER_ID, userid );
                                        contentValues.put( Database.Recent.OUSER_ID, finalSenderid );
                                        contentValues.put( Database.Recent.NAME, sendername );
                                        contentValues.put( Database.Recent.MSG_ID, msgid );
                                        contentValues.put( Database.Recent.TIME, time );
                                        contentValues.put( Database.Recent.RECENT_MODE,userMode );
                                        contentValues.put( Database.Recent.MEDIA_TYPE, Database.Recent.MEDIA_FILE);
                                        contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_NOT_SEEN );
                                        if (readable.query( Database.Recent.TBL_RECENT, null,
                                                Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?", new String[]{userid, finalSenderid}
                                                , null, null, null ).getCount() < 1) {

                                            writable.insert( Database.Recent.TBL_RECENT, null, contentValues );
                                            OTextDb.getInstance( getApplicationContext()).recentDao().insert( recent );
                                             System.out.println("Recent Inserted");
                                        } else {
                                            int done = writable.update( Database.Recent.TBL_RECENT, contentValues,
                                                    Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?", new String[]{userid, finalSenderid} );
                                            OTextDb.getInstance( getApplicationContext() ).recentDao().update( recent );
                                            System.out.println("Recent Updated");
                                        }
                                        changed.add( finalSenderid );
                                        Intent intent = new Intent( activityCompat, ConversationActivity.class );
                                        intent.putExtra( "User", finalSenderid );
                                        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                                        PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );

                                        Notification noti = new NotificationCompat.Builder( context, finalSenderid )
                                                .setContentText( sendername + " Sent You A File" ).setContentTitle( sendername )
                                                .setContentIntent( pendingIntent )
                                                .setAutoCancel( true )
                                                .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                                                .setSmallIcon( R.drawable.noti_icon )
                                                .setLargeIcon( BitmapFactory.decodeFile( profilepath ) )
                                                .setPriority( NotificationCompat.PRIORITY_MIN )
                                                .build();
                                        NotificationManager manager = (NotificationManager) activityCompat.getSystemService( Context.NOTIFICATION_SERVICE );
                                        NotificationChannel channel;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            channel = new NotificationChannel( finalSenderid, "Noti", NotificationManager.IMPORTANCE_HIGH );
                                            manager.createNotificationChannel( channel );
                                        }
                                        Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
                                        intentbroad.putExtra( "User", finalSenderid );
                                        context.sendBroadcast( intentbroad );

                                        manager.notify( ids++, noti );
                                        // FirebaseStorage.getInstance().getReference( "files/" + url ).delete();
                                        System.out.println( "Key : " + time + " value :" + value );

                                    }else{

                                        Message message=new Message();
                                        message.setMessage(value);
                                        message.setType( Database.Msg.TYPE_REC );
                                        message.setMUserId( userid );
                                        message.setOUserId( finalSenderid );
                                        message.setMsgId( msgid );
                                    //    message.setTime( time );
                                        message.setMsgMode( userMode );
                                        message.setMediaType( Database.Msg.MEDIA_TEXT );
                                        message.setStatus( Database.Msg.STATUS_NOT_SEEN );
                                        OTextDb.getInstance( getApplicationContext() ).messageDao().insertAll( message );
                                        ContentValues contentValuesMsg = new ContentValues();
                                        contentValuesMsg.put( Database.Msg.MSG, value );
                                        contentValuesMsg.put( Database.Msg.TYPE, Database.Msg.TYPE_REC );
                                        contentValuesMsg.put( Database.Msg.MUSER_ID, userid );
                                        contentValuesMsg.put( Database.Msg.OUSER_ID, finalSenderid );
                                        contentValuesMsg.put( Database.Msg.MSG_ID, msgid );
                                        contentValuesMsg.put( Database.Msg.TIME, time );
                                        contentValuesMsg.put( Database.Msg.STATUS, Database.Msg.STATUS_NOT_SEEN );
                                        contentValuesMsg.put( Database.Msg.MSG_MODE,userMode );
                                        contentValuesMsg.put( Database.Msg.MEDIA_TYPE, Database.Msg.MEDIA_TEXT );
                                        writable.insert( Database.Msg.TBL_MSG, null, contentValuesMsg );

                                        HashMap<String, Object> status_info = new HashMap<>();
                                        status_info.put( userid, Arrays.asList( new Timestamp( new Date().getTime() ).toString(), Database.Msg.STATUS_NOT_SEEN ) );
                                        FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + Senderid ).set( status_info, SetOptions.mergeFields( userid ) );
                                        ContentValues contentValues = new ContentValues();
                                        Recent recent=new Recent();
                                        recent.setMsg( value );
                                        recent.setType( Database.Recent.TYPE_USER );
                                        recent.setLocalId( localid );
                                        recent.setMUserId( userid );
                                        recent.setOUserId( Senderid );
                                        recent.setName( sendername );
                                        recent.setMsgId( msgid );
                                       // recent.setTime( time );
                                        recent.setRecentMode( userMode );
                                        recent.setMediaType( Database.Msg.MEDIA_IMG );
                                        recent.setStatus( Database.Recent.STATUS_NOT_SEEN );


                                        if(value.length()>20)
                                            contentValues.put( Database.Recent.MSG, value.substring( 0,20 )+"..." );
                                        else
                                            contentValues.put( Database.Recent.MSG, value);
                                        contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_USER );
                                        contentValues.put( Database.Recent.LOCAL_ID, localid );
                                        contentValues.put( Database.Recent.MUSER_ID, userid );
                                        contentValues.put( Database.Recent.OUSER_ID, finalSenderid );
                                        contentValues.put( Database.Recent.NAME, sendername );
                                        contentValues.put( Database.Recent.MSG_ID, msgid );
                                        contentValues.put( Database.Recent.TIME, time );
                                        contentValues.put( Database.Recent.RECENT_MODE,userMode );
                                        contentValues.put( Database.Recent.MEDIA_TYPE, Database.Msg.MEDIA_TEXT );
                                        contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_NOT_SEEN );
                                        if (readable.query( Database.Recent.TBL_RECENT, null,
                                                Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?", new String[]{userid, finalSenderid}
                                                , null, null, null ).getCount() < 1) {

                                            writable.insert( Database.Recent.TBL_RECENT, null, contentValues );

                                            OTextDb.getInstance( getApplicationContext() ).recentDao().insert( recent );
                                            System.out.println("Recent Inserted");
                                        } else {
                                           writable.update( Database.Recent.TBL_RECENT, contentValues,
                                                    Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?", new String[]{userid, finalSenderid} );

                                            OTextDb.getInstance( getApplicationContext() ).recentDao().update( recent );
                                           System.out.println("Recent Updated");
                                        }
                                        changed.add( finalSenderid );
                                        Intent intent = new Intent( activityCompat, ConversationActivity.class );
                                        intent.putExtra( "User", finalSenderid );
                                        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                                        PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );

                                        Notification noti = new NotificationCompat.Builder( context, finalSenderid )
                                                .setContentText( value ).setContentTitle( sendername )
                                                .setContentIntent( pendingIntent )
                                                .setAutoCancel( true )
                                                .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                                                .setLargeIcon( BitmapFactory.decodeFile( profilepath ) )
                                                .setSmallIcon( R.drawable.noti_icon )
                                                .setPriority( NotificationCompat.PRIORITY_HIGH )
                                                .build();
                                        NotificationManager manager = (NotificationManager) activityCompat.getSystemService( Context.NOTIFICATION_SERVICE );
                                        NotificationChannel channel;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            channel = new NotificationChannel( finalSenderid, "Noti", NotificationManager.IMPORTANCE_HIGH );
                                            manager.createNotificationChannel( channel );
                                        }
                                        Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
                                        intentbroad.putExtra( "User", finalSenderid );
                                        context.sendBroadcast( intentbroad );

                                        manager.notify( ids++, noti );
                                    }

                                    }

                                }
                            dm.put(msgid, FieldValue.delete() );

                            */
                            }
                        }
                        Messages.document( finalSenderid + "to" + userid ).update( dm );

                    }

                }
            } ).addOnFailureListener( e -> {
                failed = true;
                Notification noti = new NotificationCompat.Builder( context, "failed" )
                        .setContentText( "Failed to Fetch Msg due to no connection " ).setContentTitle( "Failed" )

                        .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                        .setSmallIcon( R.drawable.noti_icon )
                        .setPriority( 5 )
                        .build();
                NotificationManager manager = (NotificationManager) activityCompat.getSystemService( Context.NOTIFICATION_SERVICE );
                NotificationChannel channel;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channel = new NotificationChannel( "failed", "Noti", NotificationManager.IMPORTANCE_HIGH );
                    manager.createNotificationChannel( channel );
                }
                manager.notify( ids++, noti );

            } );
        }
    }
    private  void complete(File file,String url,String Senderid,String sendername,String msgid,String time,String userMode,String profilepath){
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        HashMap<String, Object> status_info = new HashMap<>();
        status_info.put( userid, Arrays.asList( new Timestamp( new Date().getTime() ).toString(), Database.Msg.STATUS_NOT_SEEN ) );
        FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + Senderid ).set( status_info, SetOptions.mergeFields( userid ) );
        Recent recent=new Recent();
        recent.setMsg( url );
        recent.setType( Database.Recent.TYPE_USER );
        recent.setLocalId( localid );
        recent.setMUserId( userid );
        recent.setOUserId( Senderid );
        recent.setName( sendername );
        recent.setMsgId( msgid );
       // recent.setTime( time );
        recent.setRecentMode( userMode );
        recent.setMediaType( Database.Msg.MEDIA_IMG );
        recent.setStatus( Database.Recent.STATUS_NOT_SEEN );
       /* ContentValues contentValues = new ContentValues();
        if(url.length()>20)
            contentValues.put( Database.Recent.MSG, url.substring( 0,20 )+"..." );
        else
            contentValues.put( Database.Recent.MSG, url);
        contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_USER );
        contentValues.put( Database.Recent.LOCAL_ID, localid );
        contentValues.put( Database.Recent.MUSER_ID, userid );
        contentValues.put( Database.Recent.OUSER_ID, Senderid );
        contentValues.put( Database.Recent.NAME, sendername );
        contentValues.put( Database.Recent.MSG_ID, msgid );
        contentValues.put( Database.Recent.TIME, time );
        contentValues.put( Database.Recent.RECENT_MODE,userMode );
        contentValues.put( Database.Recent.MEDIA_TYPE, Database.Recent.MEDIA_IMG );
        contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_NOT_SEEN );
        if (readable.query( Database.Recent.TBL_RECENT, null,
                Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?", new String[]{userid, Senderid}
                , null, null, null ).getCount() < 1) {

            writable.insert( Database.Recent.TBL_RECENT, null, contentValues );
            OTextDb.getInstance( getApplicationContext() ).recentDao().insert( recent );
            System.out.println("Recent Inserted");

        } else {
            writable.update( Database.Recent.TBL_RECENT, contentValues,
                    Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?", new String[]{userid, Senderid} );

            OTextDb.getInstance( getApplicationContext() ).recentDao().update( recent );
            System.out.println("Recent Updated");

        }

        */
        /*
        changed.add( Senderid );
        Intent intent = new Intent( activityCompat, ConversationActivity.class );
        intent.putExtra( "User", Senderid );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );

        NotificationCompat.Builder notib = new NotificationCompat.Builder( context, Senderid )
                .setContentText( sendername + " Sent You Picture" ).setContentTitle( sendername )
                .setContentIntent( pendingIntent )
                .setAutoCancel( true )
                .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                .setSmallIcon( R.drawable.noti_icon )

                .setLargeIcon( BitmapFactory.decodeFile( profilepath ) )
                .setPriority( NotificationCompat.PRIORITY_HIGH );
       /* if(url.endsWith( "gif" ))
            notib.setStyle(new  NotificationCompat.BigPictureStyle().bigPicture( BitmapFactory.decodeFile( file.getAbsolutePath()) ));


        */
        /*
            Notification noti=notib.build();
        NotificationManager manager = (NotificationManager) activityCompat.getSystemService( Context.NOTIFICATION_SERVICE );
        NotificationChannel channel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel( Senderid, "Noti", NotificationManager.IMPORTANCE_HIGH );
            manager.createNotificationChannel( channel );
        }
        Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
        intentbroad.putExtra( "User", Senderid );
        context.sendBroadcast( intentbroad );

        manager.notify( ids++, noti );
        FirebaseStorage.getInstance().getReference( "imagesThumb/" + url ).delete();


         */
    }
    public void notify(String finalSenderid,String sendername,String value,String profilepath){
        Intent intent = new Intent( activityCompat, ConversationActivity.class );
        intent.putExtra( "User", finalSenderid );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );

        Notification noti = new NotificationCompat.Builder( context, finalSenderid )
                .setContentText( value ).setContentTitle( sendername )
                .setContentIntent( pendingIntent )
                .setAutoCancel( true )
                .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                .setLargeIcon( BitmapFactory.decodeFile( profilepath ) )
                .setSmallIcon( R.drawable.noti_icon )
                .setPriority( NotificationCompat.PRIORITY_HIGH )
                .build();
        NotificationManager manager = (NotificationManager) activityCompat.getSystemService( Context.NOTIFICATION_SERVICE );
        NotificationChannel channel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel( finalSenderid, "Noti", NotificationManager.IMPORTANCE_HIGH );
            manager.createNotificationChannel( channel );
        }
        Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
        intentbroad.putExtra( "User", finalSenderid );
        context.sendBroadcast( intentbroad );

        manager.notify( ids++, noti );
    }
}
