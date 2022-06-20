package com.sk.mymassenger.db;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.sk.mymassenger.db.block.BlockDao;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.db.recent.RecentDao;
import com.sk.mymassenger.db.status.StatusDao;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.db.user.UserDao;

public class ViewModel extends AndroidViewModel {
    private OTextDb db;

    public User getUser() {
        return user;
    }

    private User user;


    public UserDao getUserDao() {

        if(userDao==null){

           userDao= db.userDao();
        }
        return userDao;
    }

    public RecentDao getRecentDao() {

        if(recentDao==null){

            recentDao=db.recentDao();
        }
        return recentDao;
    }
    public MessageDao getMessageDao() {

        if(messageDao==null){

            messageDao=db.messageDao();
        }
        return messageDao;
    }

    public BlockDao getBlockDao() {

        if(blockDao==null){

            blockDao=db.blockDao();
        }
        return blockDao;
    }

    private UserDao userDao;
    private RecentDao recentDao;
    private BlockDao blockDao;
    private MessageDao messageDao;

    public StatusDao getStatusDao() {
        if(statusDao==null){

            statusDao=db.statusDao();
        }
        return statusDao;
    }

    private StatusDao statusDao;
    public ViewModel(@NonNull Application application) {

        super( application );
        db=OTextDb.getInstance( getApplication().getApplicationContext() );
        user=db.userDao().getUser(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

}
