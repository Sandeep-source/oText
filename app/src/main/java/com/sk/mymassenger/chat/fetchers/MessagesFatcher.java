    package com.sk.mymassenger.chat.fetchers;

    import android.app.Notification;
    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.app.PendingIntent;
    import android.content.Context;
    import android.content.Intent;
    import android.graphics.BitmapFactory;
    import android.media.RingtoneManager;
    import android.os.Build;
    import android.util.Log;

    import androidx.annotation.NonNull;
    import androidx.core.app.NotificationCompat;
    import androidx.work.Worker;
    import androidx.work.WorkerParameters;

    import com.google.android.gms.tasks.OnSuccessListener;
    import com.google.firebase.firestore.CollectionReference;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FieldValue;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.storage.FirebaseStorage;
    import com.sk.mymassenger.db.Database;
    import com.sk.mymassenger.HomeActivity;
    import com.sk.mymassenger.R;
    import com.sk.mymassenger.auth.UserDataFetcher;
    import com.sk.mymassenger.data.MessageData;
    import com.sk.mymassenger.db.OTextDb;
    import com.sk.mymassenger.db.message.Message;
    import com.sk.mymassenger.db.recent.Recent;
    import com.sk.mymassenger.db.user.User;

    import java.io.File;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.LinkedList;
    import java.util.List;
    import java.util.Map;
    import java.util.Objects;

    public class MessagesFatcher extends Worker {
        private static final String TAG = "MessageFatcher";
        private final CollectionReference Messages;
        private final String userid;
        private final String localid;
        final ArrayList<String> changed;
        private final Context context;
        int ids = 0;
        final Context activityCompat;
        private boolean failed = false;
        final boolean get;
        private HashMap<String,Object> dm;
        private FetchMessage fetchMessage;

        public MessagesFatcher(Context context, WorkerParameters params) {
            super( context, params );
            this.context = context;
            changed = new ArrayList<>();
            dm=new HashMap<>(  );
            this.activityCompat = context;
            Messages = FirebaseFirestore.getInstance().collection( "Messages" );
            this.userid = getInputData().getString( "UserId" );
            this.get = getInputData().getBoolean( "get", false );
            User localdata=OTextDb.getInstance( getApplicationContext() ).userDao().getUser( userid );
            this.localid = localdata.getUserId();
            fetchMessage=new FetchMessage(context,userid,0);
        }



        @NonNull
        @Override
        public Result doWork() {
            if (get) {
                FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + userid ).get().addOnSuccessListener(documentSnapshot -> fetchMessage.work(documentSnapshot)).addOnFailureListener(e -> failed = true );
            } else {
                FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + userid ).addSnapshotListener( (value, error) -> {
                    if (value != null) {
                        fetchMessage.work( value );
                    }
                } );
            }
            if (failed) {
                return Result.retry();
            }
            return Result.success();
        }

        public static class FetchMessage{
            private Context context;
            private String userid;
            private int ids;
            private Map<String, Object> dm;
            private CollectionReference Messages;

            public FetchMessage(Context context, String userid, int ids) {
                this.context = context;
                this.userid = userid;
                this.ids = ids;
                this.dm = new HashMap<>();
                Messages = FirebaseFirestore.getInstance().collection( "Messages" );
            }

            public void work(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    System.out.println( "Fatcher " + documentSnapshot );

                    Map<String, Object> valuemap = documentSnapshot.getData();

                    if (valuemap != null) {
                        new Thread(()-> {
                            for (String User : valuemap.keySet()) {
                                Map<String, String> map = (Map<String, String>) valuemap.get(User);
                                String key = null;
                                if (map != null) {
                                    LinkedList<String> list = new LinkedList<>(map.keySet());
                                    key = list.get(0);

                                }
                                System.out.println("user " + User);
                                String Senderid = User;


                                System.out.println("key : " + key);

                                User userD = OTextDb.getInstance(context).userDao().getUserByType(User, Database.User.TYPE_FRIEND + "  OF " + userid);
                                if (userD == null) {
                                    String finalSenderid = Senderid;
                                    String finalKey = key;
                                    FirebaseFirestore.getInstance().collection("users").whereEqualTo(Database.Server.USER_ID, User).get().addOnCompleteListener(task -> {
                                        new Thread(()-> {
                                            String userName = new UserDataFetcher(context, false).getUserData(task);
                                            if (userName == null)
                                                return;
                                            if (userName.length() < 1)
                                                return;
                                            User user = OTextDb.getInstance(context).userDao().getUser(User);
                                            String userMode = user.getUserMode();
                                            getMsg(finalSenderid, finalKey, userMode);
                                        }).start();
                                    });
                                } else {

                                    String userMode = userD.getUserMode();
                                    getMsg(Senderid, key, userMode);
                                }


                            }
                        }).start();
                    }
                }


            }

            private void getMsg(String Senderid, String key, String userMode) {

                System.out.println( "user : " + userid + " Sender id : " + Senderid );

                System.out.println( "Senderid : " + Senderid );
                System.out.println( key );
                System.out.println( Senderid + "to" + userid );

                Messages.document( Senderid + "to" + userid ).get().addOnSuccessListener( (OnSuccessListener<DocumentSnapshot>) documentSnapshot1 -> {

                    if (documentSnapshot1 != null) {
                        Map<String, Object> newdocvalue = documentSnapshot1.getData();
                        if (newdocvalue != null) {
                            new Thread(()-> {
                                for (String msgid : newdocvalue.keySet()) {
                                    System.out.println("msgidid : " + msgid + " " + documentSnapshot1);
                                    Message existing = OTextDb.getInstance(context).messageDao().get(userid, Senderid, key);
                                    Message message;
                                    Log.d(TAG, "getMsg: userMode " + userMode);
                                    if (existing == null) {
                                        message = MessageData.toMessage((Map<String, Object>) Objects.requireNonNull(newdocvalue.get(msgid)), userMode);

                                        message.setMsgMode(userMode);
                                        Log.d(TAG, "getMsg: " + message);
                                        if (message.getMediaType().equals(Database.Msg.MEDIA_IMG) && message.getMessage().endsWith(".png")) {
                                            File fileDir = new File(context.getFilesDir(), "imagesThumb");
                                            if (!fileDir.exists()) {
                                                fileDir.mkdirs();
                                            }
                                            message.setMediaStatus(Database.Msg.MEDIA_STATUS_NOT_DOWNLOADED);
                                            File fileThumb = new File(fileDir, message.getMsgId() + ".png");
                                            FirebaseStorage.getInstance().getReference("imagesThumb/" + message.getMsgId() + ".png")
                                                    .getFile(fileThumb).addOnSuccessListener(taskSnapshot -> {
                                                message.setMessage(fileThumb.getAbsolutePath());
                                                new Thread(()-> {
                                                    OTextDb.getInstance(context).messageDao().insertAll(message);
                                                    FirebaseStorage.getInstance().getReference("imagesThumb/" + message.getMsgId() + ".png").delete();
                                                }).start();
                                            });
                                        } else {
                                            Log.d(TAG, "getMsg: Sender in msg " + message.getOUserId() + " user in msg " + message.getMUserId());
                                            List<Long> val = OTextDb.getInstance(context).messageDao().insertAll(message);
                                            Log.d(TAG, "getMsg: msg insert " + (val == null ? null : val.size()));
                                            User sender = OTextDb.getInstance(context).userDao().getUser(Senderid);
                                            Recent recent = MessageData.getRecent();
                                            recent.setLocalId(String.valueOf(sender.getLocalId()));
                                            recent.setName(sender.getUserName());
                                            long rval = OTextDb.getInstance(context).recentDao().insert(recent);
                                            Log.d(TAG, "getMsg: rec insert " + rval);
                                            notify(Senderid, sender.getUserName(), recent.getMsg(), sender.getProfilePicture());
                                        }
                                    } else {
                                        Log.d(TAG, "getMsg: exists");
                                        Log.d(TAG, "getMsg: msgId" + existing.getMsgId());
                                        User sender = OTextDb.getInstance(context).userDao().getUser(Senderid);

                                        message = existing;
                                        message.setReact((String) ((Map<String, Object>) Objects.requireNonNull(newdocvalue.get(msgid))).get("react"));
                                        OTextDb.getInstance(context).messageDao().insertAll(message);
                                        notify(Senderid, sender.getUserName(), sender.getUserName() + " reacted to a message ", sender.getProfilePicture());

                                    }
                                    dm.put(msgid, FieldValue.delete());



                                }
                                Messages.document(Senderid + "to" + userid).update(dm);
                            }).start();
                        }

                    }
                } ).addOnFailureListener( e -> {
                    e.printStackTrace();
                    Notification noti = new NotificationCompat.Builder( context, "failed" )
                            .setContentText( "Failed to Fetch Msg due to no connection " ).setContentTitle( "Failed" )

                            .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                            .setSmallIcon( R.drawable.noti_icon )
                            .setPriority( 5 )
                            .build();
                    NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
                    NotificationChannel channel;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        channel = new NotificationChannel( "failed", "Noti", NotificationManager.IMPORTANCE_HIGH );
                        manager.createNotificationChannel( channel );
                    }
                    manager.notify( ids++, noti );

                } );
                //}
            }

            public void notify(String finalSenderid,String sendername,String value,String profilepath){
                Intent intent = new Intent( context, HomeActivity.class );
                intent.putExtra("start",true);
                intent.putExtra( "User", finalSenderid );
                intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        /*   MessagingStyle messagingStyle=null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
               messagingStyle= new MessagingStyle(new Person.Builder().setName("you").build());
               messagingStyle.setConversationTitle(sendername);

            List<Message> message=OTextDb.getInstance(getApplicationContext()).messageDao().getAll(userid,finalSenderid, Database.Msg.MODE_PUBLIC).getValue();
                if (message != null) {
                    for(Message m : message){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            messagingStyle.addMessage(m.getMessage(),m.getTime(),
                                    new Person.Builder().setName(m.getOUserId()).build());
                        }else{
                            messagingStyle.addMessage(m.getMessage(),m.getTime(),sendername);
                        }
                    }
                }
            }

         */
                Notification noti = new NotificationCompat.Builder( context, finalSenderid )
                        .setContentText( value ).setContentTitle( sendername )
                        .setContentIntent( pendingIntent )
                        .setAutoCancel( true )
                        .setContentTitle(sendername)
                        // .setStyle((NotificationCompat.Style) messagingStyle)
                        .setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
                        .setLargeIcon( BitmapFactory.decodeFile( profilepath ) )
                        .setSmallIcon( R.drawable.noti_icon )
                        .setPriority( NotificationCompat.PRIORITY_HIGH )
                        .build();

                NotificationManager manager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
                NotificationChannel channel;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channel = new NotificationChannel( finalSenderid, "Noti", NotificationManager.IMPORTANCE_HIGH );
                    manager.createNotificationChannel( channel );
                }
                Intent intentbroad = new Intent( Database.DB_DATA_CHANGED );
                intentbroad.putExtra( "User", finalSenderid );
                context.sendBroadcast( intentbroad );

                manager.notify( ids++, noti );
            }

        }
    }
