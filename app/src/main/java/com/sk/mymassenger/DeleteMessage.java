package com.sk.mymassenger;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.db.recent.RecentDao;

import java.io.File;
import java.util.ArrayList;

public class DeleteMessage extends Service {
    private static final String TAG = "Delete Service";
    private ArrayList<String> ids;
   // private SQLiteDatabase db;
    private String mode;
    private String muser;
    private String ouser;
    private String rcMode;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d( TAG, "onCreate: done" );
     }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand( intent, flags, startId );
        NotificationManager manager= (NotificationManager) getSystemService( NOTIFICATION_SERVICE );
        NotificationChannel notificationChannel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel=new NotificationChannel( "Delete","Delete Service",NotificationManager.IMPORTANCE_HIGH );
            manager.createNotificationChannel( notificationChannel );
        }

        NotificationCompat.Builder builder=new NotificationCompat.Builder(getApplicationContext(),"Delete")
                .setPriority( NotificationCompat.PRIORITY_HIGH )
                .setContentTitle( "Deleting Messages" )
                .setSmallIcon( R.drawable.noti_icon )
                .setContentText( "Deleting...." );
         ids=intent.getStringArrayListExtra("ids");
        Toast.makeText(getApplicationContext(),"Deleted "+ids.size()+" messages",Toast.LENGTH_SHORT).show();
         mode=intent.getStringExtra("mode");
        muser=intent.getStringExtra( Database.Msg.MUSER_ID);
        ouser=intent.getStringExtra( Database.Msg.OUSER_ID);
        rcMode=intent.getStringExtra( "umode");
        new DeleteThread().start();
        startForeground( 1000,builder.build() );
        return START_STICKY;
    }
    class DeleteThread extends Thread{
        @Override
        public void run() {

            MessageDao msgDao=OTextDb.getInstance( getApplicationContext() ).messageDao();
            System.out.println(muser+"  ouser :"+ouser+" mode "+mode);
            if(mode.equals( Database.Msg.TBL_MSG )) {
                try {
                    System.out.println( "Messages to delete : " + ids.size() );
                    for(String id : ids){
                        System.out.println(id);
                    }

                    for (String id : ids) {
                        /*Cursor cursor = db.query( Database.Msg.TBL_MSG, null, Database.Msg.MSG_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? AND " + Database.Msg.OUSER_ID + " = ?",
                                new String[]{id, muser, ouser}, null, null, null );
                        cursor.moveToFirst();*/

                        Message msg=msgDao.get( muser,ouser,id );
                        String type=msg.getMediaType();
                        if (type.equals( Database.Msg.MEDIA_IMG )||type.equals( Database.Msg.MEDIA_FILE )) {
                            File file = new File(msg.getMessage());
                            if (file.exists()) {
                                if (file.delete()) {
                                    System.out.println( "File Deleted " );
                                }
                            }
                        }
                        int del=msgDao.deleteMessage(msg.getMUserId(),msg.getOUserId(),msg.getMsgId());
                        Log.d(TAG, "run: delete msg "+del);
                      /*  db.delete( Database.Msg.TBL_MSG, Database.Msg.MSG_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? AND " + Database.Msg.OUSER_ID + " = ?",
                                new String[]{id, muser, ouser} );


                       */
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }else{
                RecentDao dao=OTextDb.getInstance( getApplicationContext() ).recentDao();
                for(String ouse : ids){
                    int del=dao.deleteAll( muser,rcMode,ouse );
                    Log.d(TAG, "run: delete msg in recent "+del);
                    //db.delete( Database.Recent.TBL_RECENT,  Database.Recent.MUSER_ID + " = ? AND " + Database.Recent.OUSER_ID + " = ?",
                            //new String[]{ muser, ouse} );
                   del= msgDao.deleteAll( muser,rcMode,ouse );

                    Log.d(TAG, "run: deleted recent "+del);
                    //db.delete( Database.Msg.TBL_MSG,  Database.Msg.MUSER_ID + " = ? AND " + Database.Msg.OUSER_ID + " = ?",
                            //new String[]{ muser, ouse} );
                }

            }
            stopSelf();
        }
    }
}
