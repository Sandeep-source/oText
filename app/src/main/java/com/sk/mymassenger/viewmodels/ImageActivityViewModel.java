package com.sk.mymassenger.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.ImageActivity;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.status.Status;
import com.sk.mymassenger.db.status.StatusDao;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.db.user.UserDao;

import java.util.List;


public class ImageActivityViewModel extends AndroidViewModel {
    public long statusId = 0;
    public Status currentStatus = null;
    private StatusDao statusDao;
    private UserDao userDao;
    private List<MutableLiveData<Integer>> liveDataList;
    private User user;
    private OTextDb db;
    private static ImageActivityViewModel viewModel;

    public LiveData<List<Status>> getStatuses() {
        return statuses;
    }

    private LiveData<List<Status>> statuses;


    public ImageActivityViewModel(@NonNull Application application) {
        super(application);
        db = OTextDb.getInstance(application.getApplicationContext());
        statusDao = db.statusDao();
        userDao = db.userDao();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users/").document(
                userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        statusId = (long) documentSnapshot.get("statusCount");
                    }
                });

    }
    private void prepare(String userId){
        new Thread(()-> {
            user = userDao.getUser(userId);
            statuses = statusDao.getStatus(userId);
        }).start();
    }

    public static ImageActivityViewModel getInstance(ImageActivity activity, String userId) {
        if (viewModel == null) {


            viewModel= new ViewModelProvider(activity).get(ImageActivityViewModel.class);
        }
        viewModel.prepare(userId);
        return viewModel;
    }

    public StatusDao getStatusDao() {
        return statusDao;
    }

    public void setStatusDao(StatusDao statusDao) {
        this.statusDao = statusDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MutableLiveData<Integer> getLiveData(int index) {
        if (index >= 0) {
            return liveDataList.get(index);
        }
        return null;
    }

    public StatusPair add() {
        MutableLiveData<Integer> live = new MutableLiveData<>();
        liveDataList.add(live);
        return new StatusPair(liveDataList.size() - 1, live);
    }


    public static class StatusPair {
        private int index;
        private LiveData<Integer> liveData;

        public StatusPair(int index, LiveData<Integer> liveData) {
            this.index = index;
            this.liveData = liveData;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public LiveData<Integer> getLiveData() {
            return liveData;
        }

        public void setLiveData(LiveData<Integer> liveData) {
            this.liveData = liveData;
        }
    }
}
