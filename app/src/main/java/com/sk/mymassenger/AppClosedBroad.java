package com.sk.mymassenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.chat.fetchers.MessagesFatcher;

public class AppClosedBroad extends BroadcastReceiver {

    private ConnectivityManager manager;
    private FirebaseUser user;
    private MessagesFatcher.FetchMessage fetchMessage;

    @Override
    public void onReceive(Context context, Intent intent) {
        user=FirebaseAuth.getInstance().getCurrentUser();
        NotificationManager managerb= (NotificationManager) context.getSystemService( context.NOTIFICATION_SERVICE );
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel=new NotificationChannel( "MessageService","getmsg",NotificationManager.IMPORTANCE_MIN );
            managerb.createNotificationChannel( notificationChannel);
            Notification notificationCompat=new NotificationCompat.Builder(context,"MessageService")
                    .setSmallIcon( R.drawable.noti_icon )
                    .setPriority( NotificationCompat.PRIORITY_MIN)
                    .setAutoCancel( true )
                    .setContentTitle( user.getUid() ).setContentText( "Looking for messages..." ).build();
            managerb.notify( 1,notificationCompat);
        }else{
            Notification notificationCompat=new NotificationCompat.Builder(context,"MessageService")
                    .setSmallIcon( R.drawable.noti_icon )
                    .setContentTitle( "oText" )
                    .setAutoCancel( true )
                    .setPriority( NotificationCompat.PRIORITY_MIN ).setContentText( "Looking for messages..." ).build();
            managerb.notify( 1,notificationCompat);
        }


         manager= (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network= manager.getActiveNetwork();
            NetworkCapabilities capabilities=manager.getNetworkCapabilities( network );
            if(capabilities==null){
                process(false,context );
            }else {
                process( capabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_INTERNET ), context );
            }
        }else{
            NetworkInfo info=manager.getActiveNetworkInfo();
            boolean isconn= info != null && info.isConnectedOrConnecting();
            process( isconn ,context);
        }


    }
    public  void process(boolean is,Context context){


        if(is){
             WorkManager.getInstance( context).cancelAllWork();
            FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + user.getUid()).addSnapshotListener( (value, error) -> {

                if(fetchMessage==null){
                    fetchMessage=new MessagesFatcher.FetchMessage(
                            context,
                            user.getUid(),
                            0
                    );
                }
                fetchMessage.work(value);
            } );
            SystemClock.sleep( 6000 );
        }

    }
}
