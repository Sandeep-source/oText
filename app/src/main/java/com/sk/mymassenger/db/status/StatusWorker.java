package com.sk.mymassenger.db.status;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.auth.UserDataFetcher;
import com.sk.mymassenger.data.ServerUserData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StatusWorker extends Worker {
    private static final String TAG = "StatusWorker";
    private final List<User> users;
    private final OTextDb db;

    public StatusWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db=OTextDb.getInstance(context);
                users=db.userDao()
                .getAllUser(Database.User.TYPE_FRIEND +"  OF "+
                        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

    }

    @NonNull
    @Override
    public Result doWork() {
        StatusDao statusDao=db.statusDao();
        for(User user : users){
            String userId=user.getUserId();
            FirebaseFirestore.getInstance().collection("users/")
                    .document(user.getUserId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()){
                            new Thread(()->{
                           ServerUserData userStatus=documentSnapshot.toObject(ServerUserData.class);
                           Status localStatusData=statusDao.getLastStatus(userId);
                           if(localStatusData==null){
                               localStatusData=new Status();
                               localStatusData.setStatusTime(0);
                           }


                           if(localStatusData.getStatusTime()<userStatus.getChanged().getLastUpdated()){
                               if(user.getLastUpdated()<userStatus.getChanged().getProfile()){
                                   new UserDataFetcher(getApplicationContext(),false).getUserData(documentSnapshot);
                               }else{
                                   getNormal(user,userStatus);
                               }
                               if(localStatusData.getStatusTime()<userStatus.getChanged().getStatus()){

                                   FirebaseFirestore.getInstance().collection("statuses/").document(user.getUserId())
                                           .get().addOnSuccessListener(snapshot -> {
                                               if(snapshot.exists()){
                                                   new Thread(()-> {
                                                       Map<String, Object> data = snapshot.getData();

                                                       if (data != null) {
                                                           for (String key : data.keySet()) {
                                                               if (statusDao.getStatusById(key) == null) {
                                                                   Log.d(TAG, "onSuccess: documentSnapshot " + snapshot);
                                                                   Status status = Status.toStatus((Map<String, Object>) Objects.requireNonNull(data.get(key)));
                                                                   statusDao.setStatus(status);
                                                               }

                                                           }
                                                           statusDao.deleteNotPresent(user.getUserId(), new LinkedList<>(data.keySet()));

                                                       } else {
                                                           statusDao.deleteAll(user.getUserId());
                                                       }
                                                   }).start();
                                               }
                                           }).addOnFailureListener(Throwable::printStackTrace);
                               }
                           }
                        }).start();
                        }
                    });
        }
        return Result.success();
    }
    public void getNormal(User user,ServerUserData userStatus){
        user.setUserName(userStatus.getUserName());
        ServerUserData.Mood mood=userStatus.getMood();
        user.setMood(mood.getValue());
        user.setMoodSince(mood.getSince());
        user.setFreeTime(userStatus.getFreeTime());
        db.userDao().update(user);

    }
}
