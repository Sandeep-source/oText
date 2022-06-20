package com.sk.mymassenger;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;

import java.util.List;


public class FailedMessageSender extends Worker {
    String Muser;
    List<Message> messages;
    public FailedMessageSender(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        Muser=getInputData().getString( "User" );
        messages= OTextDb.getInstance( context ).messageDao().getFailed( Muser, Database.Msg.STATUS_NOT_SENT );

    }

    @NonNull
    @Override
    public Result doWork() {
           int count=messages.size();
           for(int i=0;i<count;i++){
               Message message=messages.get( i );
               String msgid=message.getMsgId();
               String msg=message.getMessage();
               String userid=message.getMUserId();
               String recieverid=message.getOUserId();
              /* Cursor cur=new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION ).getReadableDatabase().query(
                       Database.Block.TBL_BLOCK, null,Database.Block.OUSER_ID+" = ? ",new String[]{recieverid},null,null,null,null );
               if(cur.getCount()>0){
                   cur.close();
                   continue;
               }

               */
               String type=message.getType();
               String time= String.valueOf( message.getTime() );
               if(type.equals( Database.Msg.MEDIA_IMG )){
                   Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();

                   OneTimeWorkRequest request=new OneTimeWorkRequest.Builder( FilesSender.class ).setConstraints( constraints )
                           .setInputData( new Data.Builder().putString( "userid",userid ).putString( "recieverid",recieverid ).putString( "msgid",msgid )
                                   .putString( "serverpath","images/"+msg.substring( msg.lastIndexOf( "/" )+1 ) ).putString( "servervalue",msgid+".png(img)(/img)")
                                   .putString( "file",msg)
                                   .putString( "time",time ).build()).build();
                   WorkManager.getInstance(getApplicationContext() ).enqueue( request );
               }else{
                   Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();

                   OneTimeWorkRequest request=new OneTimeWorkRequest.Builder( FilesSender.class ).setConstraints( constraints )
                           .setInputData( new Data.Builder().putString( "userid",userid ).putString( "recieverid",recieverid ).putString( "msgid",msgid )
                                   .putString( "serverpath","files/"+msg.substring( msg.lastIndexOf( "/" )+1 ) ).putString( "servervalue",msgid+".png(file)(/file)")
                                   .putString( "file",msg)
                                   .putString( "time",time ).build()).build();
                   WorkManager.getInstance(getApplicationContext() ).enqueue( request );
               }
           }
        return Result.success();
    }
}
