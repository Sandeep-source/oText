package com.sk.mymassenger.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.status.StatusDao;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.db.user.UserDao;

public class ProfileSettingViewModel extends AndroidViewModel {
    public StatusDao getStatusDao() {
        return statusDao;
    }

    private final StatusDao statusDao;
    private UserDao userDao;

    public ProfileSettingViewModel(@NonNull Application application) {
        super(application);

            OTextDb db=OTextDb.getInstance(application.getApplicationContext());
            userDao=db.userDao();
            statusDao=db.statusDao();


    }


    public LiveData<User> getUser() {


            String userId=FirebaseAuth.getInstance().getCurrentUser().getUid();
            return userDao.getLiveUser(userId);



    }

    private User user;


    @Override
    protected void onCleared() {
        super.onCleared();
        new Thread(()->{ userDao.update(user);}).start();

    }
    public void save(){
        new Thread(()->{ userDao.update(user);}).start();
    }

    public void setUser(User user) {
        this.user=user;
    }
}
