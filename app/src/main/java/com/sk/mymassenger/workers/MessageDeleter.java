package com.sk.mymassenger.workers;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.db.recent.RecentDao;

import java.io.File;

public class MessageDeleter extends Worker {
    private final String mode;
    private String muser;
    private String ouser;
    private String[] ids;
   // private SQLiteDatabase db;
    public MessageDeleter(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super( context, workerParams );
        Data data=getInputData();
        muser=data.getString( Database.Msg.MUSER_ID );
        ouser=data.getString( Database.Msg.OUSER_ID );
        ids=data.getStringArray( "ids" );
        //db=new oTextDb( context,Database.O_TEXT_DB,null,Database.DB_VERSION ).getWritableDatabase();
        mode=data.getString( "mode" );
    }

    @NonNull
    @Override
    public Result doWork() {

        MessageDao dao= OTextDb.getInstance( getApplicationContext() ).messageDao();
        if(mode.equals( Database.Msg.TBL_MSG )) {
            try {
                System.out.println( "Messages to delete : " + ids.length );
                for (String id : ids) {
                  /*  Cursor cursor = db.query( Database.Msg.TBL_MSG, null, Database.Msg.MSG_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? AND " + Database.Msg.OUSER_ID + " = ?",
                            new String[]{id, muser, ouser}, null, null, null );
                    cursor.moveToFirst();

                   */
                    Message message=dao.get( muser,ouser,id );
                    String type=message.getType();
                    if (type.equals( Database.Msg.MEDIA_IMG )||type.equals( Database.Msg.MEDIA_FILE )) {
                        File file = new File( message.getMessage());
                        if (file.exists()) {
                            if (file.delete()) {
                                System.out.println( "File Deleted " );
                            }
                        }
                    }


                    dao.deleteMessage(message.getMUserId(),message.getOUserId(),message.getMsgId());
                   /* db.delete( Database.Msg.TBL_MSG, Database.Msg.MSG_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? AND " + Database.Msg.OUSER_ID + " = ?",
                            new String[]{id, muser, ouser} );

                    */
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Intent intent = new Intent( Database.DB_DATA_CHANGED );
            intent.putExtra( "User", ouser );
            getApplicationContext().sendBroadcast( intent );
        }else{
            RecentDao recentDao=OTextDb.getInstance( getApplicationContext() ).recentDao();
            for(String ouse : ids){
               // db.delete( Database.Recent.TBL_RECENT,  Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?",
                 //       new String[]{ muser, ouse} );
                recentDao.deleteAll( muser,mode,ouse );
                //db.delete( Database.Msg.TBL_MSG,  Database.Msg.MUSER_ID + " = ? AND " + Database.Msg.OUSER_ID + " = ?",
                  //      new String[]{ muser, ouse} );
                dao.deleteAll( muser,mode,ouse );
            }
          /*  Intent intent = new Intent( Database.DB_DATA_CHANGED );
            intent.putExtra( "User", ouser );
            getApplicationContext().sendBroadcast( intent );

           */
        }
        return Result.success();
    }
}
