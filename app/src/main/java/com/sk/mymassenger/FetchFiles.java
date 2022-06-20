package com.sk.mymassenger;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;

import java.io.File;
import java.io.IOException;

public class FetchFiles extends Worker {
    private final String muser;
    private final String filep;
    private final String ouser;
    private final String path;
    private final int index;
    private String msgid;

    public FetchFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        Data data=getInputData();
        filep=data.getString( "file" );
        ouser=data.getString( "ouser" );
        muser=data.getString( "muser" );
        path= data.getString( "serverPath" );
        msgid=data.getString( "msgid" );
        index=data.getInt( "index" ,0);
    }

    @NonNull
    @Override
    public Result doWork() {
        File file=new File(filep );
        System.out.println( "file p : "+filep );
        if(!file.exists()){
            try {
               File parent=new File (file.getParent());
               if(!parent.exists()){
                   parent.mkdirs();
               }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println( "path  : "+path );
        FirebaseStorage.getInstance().getReference(path ).getFile( file ).addOnSuccessListener( new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
               /* ContentValues contentValues=new ContentValues(  );
                contentValues.put( Database.Msg.MSG,file.getAbsolutePath() );
                contentValues.put( Database.Msg.MEDIA_STATUS, Database.Msg.MEDIA_STATUS_DOWNLOADED );
                new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION ).getWritableDatabase().update( Database.Msg.TBL_MSG
                ,contentValues, Database.Msg.MSG_ID+" = ? AND "+ Database.Msg.MUSER_ID+" = ? AND "+ Database.Msg.OUSER_ID+" = ? ",new String[]{msgid,muser,ouser});
                Intent intent=new Intent( Database.DB_DATA_CHANGED );
                intent.putExtra( "User",ouser );
                getApplicationContext().sendBroadcast( intent );

                */
                MessageDao messageDao=OTextDb.getInstance( getApplicationContext() ).messageDao();
                Message message=messageDao.get( muser,ouser,msgid );
                message.setMessage(file.getAbsolutePath());
                messageDao.update( message );
                FirebaseStorage.getInstance().getReference(path ).delete();
            }
        } ).addOnFailureListener( new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
               /* Intent intent=new Intent( Database.DB_DATA_CHANGED );
                intent.putExtra( "User",ouser );
                intent.putExtra( "index",index);
                intent.putExtra( "msgid",msgid);
                getApplicationContext().sendBroadcast( intent );

                */
            }
        } );
        return Result.success();
    }
}
