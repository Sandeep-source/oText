package com.sk.mymassenger;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class FilesSender extends Worker {
    private final String type;
    private final String filethumb;
    private String userid;
    private String msgid;
    private String recieverid;
    private Context context;
    private String file;
    private String time;
   // private oTextDb finalDb;
    private String serverpath;
    private String servervalue;
    private String serverpaththumb;

    public FilesSender(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        Data data=getInputData();
        this.userid = data.getString(  "userid");
        this.msgid = data.getString(  "msgid");;
        this.recieverid = data.getString(  "recieverid");;
        this.context = context;
        this.file = data.getString(  "file");
        this.time = data.getString(  "time");
        filethumb=data.getString( "fileBlur" );
        type=data.getString( "type" );
        serverpaththumb=data.getString( "serverPathThumb" );
       // this.finalDb =new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION );
        this.servervalue= data.getString(  "servervalue");
        this.serverpath = data.getString(  "serverpath");
    }


    @NonNull
    @Override
    public Result doWork() {
        Uri uriThumb = null;
        if(type.equals( "img" ))
         uriThumb=FileProvider.getUriForFile( context,Database.FILE_ATHORITY,new File( filethumb ) );
        Uri uri= FileProvider.getUriForFile( context,Database.FILE_ATHORITY,new File( file ) );
        UploadTask task= FirebaseStorage.getInstance().getReference(serverpath).putFile( uri );
        Uri finalUriThumb = uriThumb;
        task.addOnSuccessListener( taskSnapshot -> {
          /*  ContentValues values = new ContentValues();
            values.put( Database.Msg.STATUS, Database.Msg.STATUS_SENT );
            finalDb.getWritableDatabase().update( Database.Msg.TBL_MSG, values, Database.Msg.MUSER_ID + " = ? AND " +
                            Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.MSG_ID + "= ?"
                    , new String[]{userid, recieverid, msgid} );
            Intent intent = new Intent( Database.DB_DATA_CHANGED );
            intent.putExtra( "User", recieverid );
            context.sendBroadcast( intent );

           */
            MessageDao messageDao= OTextDb.getInstance( getApplicationContext() ).messageDao();
            Message message=messageDao.get( userid,recieverid,msgid );
            message.setStatus( Database.Msg.STATUS_SENT);
            messageDao.update( message );
            HashMap<String,Object> valueforserver= new HashMap<>();
            MessageData data=new MessageData( message );
            data.setMessage(servervalue);
            valueforserver.put( msgid,data );
            HashMap<String,Object> status_info=new HashMap<>(  );
            status_info.put(recieverid, Arrays.asList( time,Database.Msg.STATUS_SENT ) );
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "info"+userid).set(status_info, SetOptions.mergeFields(recieverid));
            FirebaseFirestore.getInstance().collection( "Messages" ).document(userid+"to"+recieverid).set(valueforserver, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
                /*Intent intent1 =new Intent( Database.DB_DATA_CHANGED );
                intent1.putExtra( "User", recieverid );
                context.sendBroadcast( intent1 );

                 */
            } );
            HashMap<String,Object> val=new HashMap<>();
            valueforserver.clear();
            val.clear();
            val.put(msgid,time);
            valueforserver.put( userid,val );
             if(type.equals( "img" )){
                 FirebaseStorage.getInstance().getReference(serverpaththumb).putFile( finalUriThumb ).addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                         try {
                             new File( filethumb ).delete();

                         } catch (Exception ex) {
                             ex.printStackTrace();
                         }
                     }
                 } );
             }
            FirebaseFirestore.getInstance().collection( "Messages" ).document("to"+recieverid).set(valueforserver,SetOptions.mergeFields( userid ) );


        } );
        return Result.success();
    }
}
