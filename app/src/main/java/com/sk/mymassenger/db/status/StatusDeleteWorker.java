package com.sk.mymassenger.db.status;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.db.OTextDb;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class StatusDeleteWorker extends Worker {
    private final String userId;
    private StatusDao statusDao;
    private List<Status> statuses;
    public StatusDeleteWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
       statusDao=OTextDb.getInstance(getApplicationContext()).statusDao();
       statuses=statusDao.getAll();
       userId= Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    @NonNull
    @Override
    public Result doWork() {
        for (Status status :
                statuses) {
            long now=new Date().getTime();
            if (status.getStatusTime()<(now-(24*60*60*1000))){
                statusDao.delete(status.getStatusId());
                File file=new File(status.getStatusUri());
                if(file.exists()){
                    file.delete();
                }
                if(status.getUserId().equals(userId)){
                    HashMap<String,Object> map=new HashMap<>();
                    map.put(status.getStatusId(), FieldValue.delete());
                    FirebaseFirestore.getInstance().collection("statuses/").document(userId)
                            .set(map, SetOptions.mergeFields(status.getStatusId()));
                    FirebaseStorage.getInstance().getReference("statuses/"+file.getName()).delete();
                }
            }
        }
        return Result.success();
    }
}
