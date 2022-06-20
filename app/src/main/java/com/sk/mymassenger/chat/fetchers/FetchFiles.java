package com.sk.mymassenger.chat.fetchers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.viewmodels.HomeViewModel;

import java.io.File;
import java.io.IOException;

public class FetchFiles extends Worker {
    private final String muser;
    private final String filep;
    private final String ouser;
    private final String path;
    private final String msgid;
    private MutableLiveData<Integer> liveData;
    private static final String TAG = "FetchFiles";

    public FetchFiles(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );

        Data data=getInputData();
        filep=data.getString( "file" );
        ouser=data.getString( "ouser" );
        int liveDataIndex = data.getInt("liveDataIndex", -1);
        muser=data.getString( "muser" );
        path= data.getString( "serverPath" );
        msgid=data.getString( "msgid" );
        HomeViewModel viewModel=HomeViewModel.getInstance();
        if(viewModel!=null) {
            liveData = viewModel.getLiveData(liveDataIndex);
        }
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
        FileDownloadTask task = FirebaseStorage.getInstance().getReference(path).getFile(file);
        Log.d(TAG, "doWork: LiveData : "+liveData);
       if(liveData!=null) {
           task.addOnProgressListener(snapshot -> {
               long total = snapshot.getTotalByteCount();
               long done = snapshot.getBytesTransferred();
               int val=(int) ((done * 100) / total);
               Log.d(TAG, "doWork: progress : "+val);
               liveData.postValue(val);
           });
       }


        task.addOnSuccessListener(taskSnapshot -> {
          new Thread(()-> {
              MessageDao messageDao = OTextDb.getInstance(getApplicationContext()).messageDao();
              Message message = messageDao.get(muser, ouser, msgid);
              message.setMessage(file.getAbsolutePath());
              message.setMediaStatus(Database.Msg.MEDIA_STATUS_DOWNLOADED);
              messageDao.update(message);

              FirebaseStorage.getInstance().getReference(path).delete();
          }).start();
        }).addOnFailureListener(Throwable::printStackTrace);
        return Result.success();
    }
}
