package com.sk.mymassenger.db.status;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.db.OTextDb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StatusUploadWorker extends Worker {

    private final Uri uri;
    private final String userId;
    private final String mediaType;
    private final String extension;
    private final String statusText;
    private long count;

    public StatusUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Data data=getInputData();
        extension=data.getString("extension");
        mediaType=data.getString("mediaType");
        userId=data.getString("userId");
        uri=Uri.parse(data.getString("data"));
        statusText=data.getString("statusText");
        count=data.getLong("id",0);

    }

    @NonNull
    @Override
    public Result doWork() {
        StatusDao statusDao= OTextDb.getInstance(getApplicationContext()).statusDao();


        Status status=new Status();

                File dir=new File(getApplicationContext().getFilesDir(),"statuses");
                if(!dir.exists()){
                    dir.mkdirs();
                }
                File file=new File(dir,userId+count+extension);

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        assert uri != null;
                        Files.copy(getApplicationContext().getContentResolver().openInputStream(uri),file.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                status.setStatusId(userId+count);
                status.setStatusType(mediaType);
                status.setStatusUri(file.getAbsolutePath());
                status.setMediaStatus(Status.MEDIA_STATUS_LOCAL);
                status.setUserId(userId);
                status.setStatusText(statusText);
                long time=new Date().getTime();
                status.setStatusTime(time);

                assert uri != null;
                HashMap<String,Object> map=new HashMap<>();
                map.put(status.getStatusId(),status);
                FirebaseFirestore.getInstance().collection("statuses/").document(userId)
                        .set(map,SetOptions.mergeFields(status.getStatusId()));
                FirebaseStorage.getInstance().getReference("statuses/"+userId+count+extension)
                        .putFile(uri).addOnSuccessListener(taskSnapshot -> {
                    HashMap<String,Object> changed=new HashMap<>();
                    HashMap<String,Object> statusChanged=new HashMap<>();
                    statusChanged.put("status",time);
                    statusChanged.put("lastUpdated",time);
                    changed.put("changed",statusChanged);
                    FirebaseFirestore.getInstance().collection("users/").document(userId).set(changed,SetOptions.mergeFields("changed"))
                            .addOnSuccessListener(aVoid -> {
                                new Thread(()-> {
                                    long id = statusDao.setStatus(status);
                                    Looper.prepare();
                                    Toast.makeText(getApplicationContext(), "Status Updated " + id, Toast.LENGTH_SHORT).show();
                                }).start();
                            });
                });


        return Result.success();
    }
}
