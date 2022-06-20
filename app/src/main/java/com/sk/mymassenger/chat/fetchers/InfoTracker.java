package com.sk.mymassenger.chat.fetchers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.MessageDao;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

//TODO(last changed info ) status update changed
public class InfoTracker extends Worker {
    private final String userid;
    private final boolean get;
  private  boolean success=true;
    public InfoTracker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        userid=getInputData().getString( "UserId" );
        get=getInputData().getBoolean( "get",false );
    }

    @NonNull
    @Override
    public Result doWork() {

        if(get) {
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "info"+userid ).get().addOnSuccessListener( (task) -> {
                if (task != null)
                 new Thread(()-> getInfo( task.getData() )).start();
            } ).addOnFailureListener( (f)-> success=false );
        }else {
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + userid ).addSnapshotListener( (value, error) -> {
                if (value != null) {
                    new Thread(()-> getInfo( value.getData() )).start();
                }
            } );
        }
        if(success)
        return Result.success();
        return Result.retry();
    }

    private void getInfo(Map<String, Object> data) {
        if (data != null){
            //SQLiteDatabase db = new oTextDb( getApplicationContext(), Database.O_TEXT_DB, null, Database.DB_VERSION ).getWritableDatabase();
        for (String id : data.keySet()) {
            LinkedList<String> infolist = new LinkedList<>( (Collection<? extends String>) data.get( id ) );
            String time = String.valueOf(infolist.get( 0 ));
            String status = infolist.get( 1 );

            System.out.println("time : "+time +" status : "+status);
            MessageDao messageDao=OTextDb.getInstance( getApplicationContext() ).messageDao();
            if(status==null)
                return;
            switch (status) {
                case Database.Msg.STATUS_NOT_SEEN:
                   // db.update( Database.Msg.TBL_MSG, values, Database.Msg.MUSER_ID + " = ? AND " +
                         //           Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.TIME + "< ? AND " + Database.Msg.STATUS + " = ? "
                         //   , new String[]{userid, id, time, Database.Msg.STATUS_SENT} );
                    messageDao.updateStatus( userid,id,time,Database.Msg.STATUS_SENT,status );
                    break;
                case Database.Msg.STATUS_SEEN:
                    /*db.update( Database.Msg.TBL_MSG, values, Database.Msg.MUSER_ID + " = ? AND " +
                                    Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.TIME + "< ? AND "+ Database.Msg.STATUS+" = ? "
                            , new String[]{userid, id, time, Database.Msg.STATUS_NOT_SEEN} );

                     */
                    messageDao.updateStatus( userid,id,time,Database.Msg.STATUS_NOT_SEEN, status );
                    break;
                case Database.Msg.STATUS_SENT:
                    /*db.update( Database.Msg.TBL_MSG, values, Database.Msg.MUSER_ID + " = ? AND " +
                                    Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.TIME + "< ? AND " + Database.Msg.STATUS + " = ? "
                            , new String[]{userid, id, time, Database.Msg.STATUS_NOT_SENT} );

                     */
                    messageDao.updateStatus( userid,id,time, Database.Msg.STATUS_NOT_SENT,status);
                    break;
            }


        }

    }
    }

}
