package com.sk.mymassenger;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Date;
import java.util.HashMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MessageService extends Service {

   private String userid;
    private String Username;
    private Handler handler;
    private Runnable runnable;
    private AlarmManager alarmManager;

    public MessageService() {
        super( );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        userid=user.getUid();
        Username=user.getDisplayName();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
         super.onStartCommand( intent, flags, startId );
        NotificationManager manager= (NotificationManager) getSystemService( NOTIFICATION_SERVICE );
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel=new NotificationChannel( "MessageService","getmsg",NotificationManager.IMPORTANCE_MIN );
            manager.createNotificationChannel( notificationChannel);
            Notification notificationCompat=new NotificationCompat.Builder(getApplicationContext(),"MessageService")
                    .setSmallIcon( R.drawable.noti_icon )
                     .setPriority( NotificationCompat.PRIORITY_MIN)
                    .setAutoCancel( true )
                    .setContentTitle( "oText" ).setContentText( "Looking for messages..." ).build();
            startForeground( 1,notificationCompat);
        }else{
            Notification notificationCompat=new NotificationCompat.Builder(getApplicationContext(),"MessageService")
                    .setSmallIcon( R.drawable.noti_icon )
                    .setContentTitle( "oText" )
                    .setAutoCancel( true )
                    .setPriority( NotificationCompat.PRIORITY_MIN ).setContentText( "Looking for messages..." ).build();
            startForeground( 1,notificationCompat);
        }



         return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        DatabaseReference ref= FirebaseDatabase.getInstance("https://otext-2f0cc-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users/"+userid);
        HashMap<String,Object> hashMap=new HashMap<>( );
        hashMap.put( "status","offline" );
        hashMap.put( "last seen", ServerValue.TIMESTAMP );
        ref.setValue( hashMap );

        alarmManager= (AlarmManager) getApplicationContext().getSystemService( ALARM_SERVICE );
        Intent intent=new Intent(getApplicationContext(),AppClosedBroad.class);
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent=PendingIntent.getBroadcast(getApplicationContext(),
                1,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d( "service   ", "onStop: "+pendingIntent );
        alarmManager.setRepeating( AlarmManager.RTC, System.currentTimeMillis(),60000,pendingIntent );
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }





}
