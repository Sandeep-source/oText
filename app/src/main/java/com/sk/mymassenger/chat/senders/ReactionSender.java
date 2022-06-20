package com.sk.mymassenger.chat.senders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class ReactionSender extends Worker {
    private final String msg;
    private Message message;
    private String react;
    public ReactionSender(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Data data=workerParams.getInputData();
        String userId=data.getString(Database.Msg.MUSER_ID);
        String recId=data.getString(Database.Msg.OUSER_ID);
        String msgId=data.getString(Database.Msg.MSG_ID);
        String react=workerParams.getInputData().getString("react");
        message= OTextDb.getInstance(context).messageDao().get(userId,recId,msgId);
        message.setReact(react);
        OTextDb.getInstance(context).messageDao().insertAll(message);
        msg=message.getMessage();
        if(!message.getMediaType().equals(Database.Msg.MEDIA_TEXT))
        message.setMessage(new File(msg).getName());
    }

    @NonNull
    @Override
    public Result doWork() {
            HashMap<String, Object> valueforserver = new HashMap<>();
            HashMap<String, Object> val = new HashMap<>();


            valueforserver.put(message.getMsgId(),new MessageData(message,false) );

            FirebaseFirestore.getInstance().collection( "Messages" ).document( message.getMUserId()+ "to" +message.getOUserId() ).set( valueforserver, SetOptions.merge() ).addOnSuccessListener(aVoid -> {
                HashMap<String, Object> status_info = new HashMap<>();
                status_info.put( message.getOUserId(), Arrays.asList( message.getTime(),message.getStatus() ) );
                FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + message.getMUserId() ).set( status_info, SetOptions.mergeFields( message.getOUserId() ) ).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        valueforserver.clear();
                        val.clear();
                        val.put( message.getMsgId(), message.getTime() );
                        valueforserver.put( message.getMUserId(), val );

                        FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + message.getOUserId()).set( valueforserver, SetOptions.mergeFields( message.getMUserId() ) ).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                message.setStatus( Database.Msg.STATUS_SENT );
                                message.setMessage(msg);
                                OTextDb.getInstance(getApplicationContext() ).messageDao().update( message );
                            }
                        });

                    }
                });

            } );

        return Result.success();
    }
}
