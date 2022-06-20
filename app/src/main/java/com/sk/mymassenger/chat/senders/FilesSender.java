package com.sk.mymassenger.chat.senders;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;
import com.sk.mymassenger.HomeActivity;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.oTextMessenger;
import com.sk.mymassenger.viewmodels.HomeViewModel;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class FilesSender extends Worker {
    private final String type;
    private final String filethumb;
    private final String userid;
    private final String msgid;
    private final String recieverid;
    private final Context context;
    private final String file;
    private final String time;

    private final String serverpath;
    private final String servervalue;
    private final String serverpaththumb;

    public FilesSender(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        Data data=getInputData();
        this.userid = data.getString(  "userid");
        this.msgid = data.getString(  "msgid");
        this.recieverid = data.getString(  "recieverid");
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
        if(type==null) return Result.failure();
        try {
            if (type.equals("img"))
                uriThumb = FileProvider.getUriForFile(context, Database.FILE_ATHORITY, new File(filethumb));
        }catch (IllegalArgumentException ex) {
           ex.printStackTrace();
        }
        Uri uri= FileProvider.getUriForFile( context,Database.FILE_ATHORITY,new File( file ) );
        UploadTask task= FirebaseStorage.getInstance().getReference(serverpath).putFile( uri );
        Uri finalUriThumb = uriThumb;

        MessageDao messageDao= OTextDb.getInstance( getApplicationContext() ).messageDao();
        Message message=messageDao.get( userid,recieverid,msgid );
        HomeViewModel viewModel=HomeViewModel.getInstance();
        if(viewModel!=null) {
            MutableLiveData<Integer> liveData = viewModel.getLiveData((int) message.getTransferIndex());

            task.addOnProgressListener(snapshot -> {
                int per = (int) ((snapshot.getBytesTransferred() * 100) / snapshot.getTotalByteCount());
                liveData.postValue(per);
            });
        }
        

        task.addOnSuccessListener( taskSnapshot -> new Thread(()->{
          message.setStatus( Database.Msg.STATUS_SENT);
          message.setMediaStatus(Database.Msg.MEDIA_STATUS_DOWNLOADED);
          messageDao.update( message );
          HashMap<String,Object> valueforserver= new HashMap<>();
          MessageData data=new MessageData( message,true );
          data.setMessage(servervalue);
          valueforserver.put( msgid,data );
          HashMap<String,Object> status_info=new HashMap<>(  );
          status_info.put(recieverid, Arrays.asList( time,Database.Msg.STATUS_SENT ) );
          FirebaseFirestore.getInstance().collection( "Messages" ).document( "info"+userid).set(status_info, SetOptions.mergeFields(recieverid));
          FirebaseFirestore.getInstance().collection( "Messages" ).document(userid+"to"+recieverid).set(valueforserver, SetOptions.merge() ).addOnSuccessListener( aVoid -> {

          } );
          HashMap<String,Object> val=new HashMap<>();
          valueforserver.clear();
          val.clear();
          val.put(msgid,time);
          valueforserver.put( userid,val );
           if(type.equals( "img" )&&finalUriThumb!=null){

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

      } ).start());
        return Result.success();
    }
}
