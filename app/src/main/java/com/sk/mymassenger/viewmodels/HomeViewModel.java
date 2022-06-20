package com.sk.mymassenger.viewmodels;

import android.app.Application;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.BlockInfo;
import com.sk.mymassenger.chat.ChatFragmentAdapter;
import com.sk.mymassenger.chat.ChatListFragment;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.HomeActivity;
import com.sk.mymassenger.chat.senders.ImageSender;
import com.sk.mymassenger.chat.senders.PrepareFileToSend;
import com.sk.mymassenger.chat.senders.PrepareMessage;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.block.BlockDao;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.message.MessageDao;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.recent.RecentDao;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.db.user.UserDao;
import com.sk.mymassenger.chat.senders.FilesSender;
import com.sk.mymassenger.chat.fetchers.InfoTracker;
import com.sk.mymassenger.chat.fetchers.MessagesFatcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public class HomeViewModel extends AndroidViewModel {
    private static final String TAG = "HomeViewModel";
    private static HomeViewModel viewModel;
    private final OTextDb db;
    public final ListenerRegistration listenMsg;
    public final ListenerRegistration listenInfo;
    public final DatabaseReference ref;
    public String userId;
    public ChatListFragment.ConversationAdapter adapter;
    public ChatFragmentAdapter chatAdapter;
    public boolean can=true;
    public boolean byme;
    public String mode;
    private LinkedList<MutableLiveData<Integer>> liveData;
    private MessagesFatcher.FetchMessage fetchMessage;

    public static HomeViewModel getInstance() {
        return viewModel;
    }

    public LiveData<User> getLiveReceiver(String receiverId) {
        if(liveReceiver==null){
            liveReceiver=getUserDao().getLiveUser(receiverId);
        }
        return liveReceiver;
    }

    private LiveData<User> liveReceiver;

    public User getReceiver(String receiverId) {
        if(receiver==null){
            liveData=new LinkedList<>();

            receiver=getUserDao().getUser(receiverId);
        }else if(receiverId!=null){

            liveData=new LinkedList<>();

            receiver=getUserDao().getUser(receiverId);
        }
        checkBlock(receiverId);
        return receiver;
    }

    public void checkBlock(String rcId){
        Block block=getBlockDao().getBlock(userId,rcId);
        if(block==null){
            can=true;
        }else{
            Log.d(TAG, "checkBlock: "+block.getType()+" "+block.getMUserId()+" "+block.getOUserId());
            byme=block.getType().equals(Database.Block.BY_ME);
            can=false;
        }
    }
    public User receiver;
    public PrepareMessage prepareMessage;
    private Message message;


    public HomeViewModel(@NonNull Application application) {
        super(application);
        db= OTextDb.getInstance(getApplication());
        userId=FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "HomeViewModel: userId : "+userId);
        FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + userId ).addSnapshotListener( (value, error) -> {

            if(fetchMessage==null){
                fetchMessage=new MessagesFatcher.FetchMessage(
                        getApplication().getApplicationContext(),
                        userId,
                        0
                );
            }
           fetchMessage.work(value);

        } );
        listenMsg= FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" +userId ).addSnapshotListener( (value, error) -> {

            Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
            OneTimeWorkRequest work= new OneTimeWorkRequest.Builder(BlockInfo.class).setConstraints( constraints )
                    .addTag("BlockInfoO")
                    .setInputData(
                            new Data.Builder().putString("UserId",userId).build()
                    ).build();
            WorkManager.getInstance( application.getApplicationContext()).enqueue( work);
        } );
        listenInfo=FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + userId ).addSnapshotListener( (value, error) -> {
            if (value != null) {
                Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                OneTimeWorkRequest workinfo= new OneTimeWorkRequest.Builder(InfoTracker.class).setConstraints( constraints )
                        .addTag("MessageInfoTrackerO")
                        .setInputData(
                                new Data.Builder().putString("UserId",userId)
                                        .putBoolean( "get",true ).build()
                        ).build();
                WorkManager.getInstance(application.getApplicationContext() ).enqueue( workinfo);

            }
        } );

        ref = FirebaseDatabase.getInstance("https://otext-2f0cc-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users/" + userId);
        HashMap<String,Object> hashMap=new HashMap<>( );
        hashMap.put( "status","online" );
        hashMap.put( "last seen", ServerValue.TIMESTAMP );
        ref.setValue( hashMap ).addOnCompleteListener( new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "onComplete: Done");
            }
        } );
        viewModel=this;
    }
   public static HomeViewModel getInstance(HomeActivity activity) {
        if(viewModel==null){
            viewModel=new ViewModelProvider(activity).get(HomeViewModel.class);
        }
        return viewModel;
    }


    public UserDao getUserDao() {
        return db.userDao();
    }

    public MessageDao getMessageDao(){
        return db.messageDao();
    }

    public User getUser() {
        return getUserDao().getUser(userId);
    }

    public RecentDao getRecentDao() {
        return db.recentDao();
    }


    public void setReplyTo(Message o) {
        this.message=o;
    }


    public BlockDao getBlockDao() {
        return db.blockDao();
    }


    public Message getReplyTo() {
        return message;
    }

    public StatusPair add(){
        MutableLiveData<Integer> live=new MutableLiveData<>();
        liveData.add(live);
        return new StatusPair(liveData.size()-1,live);
    }

    public MutableLiveData<Integer> getLiveData(int transferIndex) {
        if(liveData!=null&&liveData.size()>transferIndex)
        return liveData.get(transferIndex);
        else {
            markFailedMsg();
            return null;
        }
    }

    private void markFailedMsg() {

    }

    public void sendPixaBayImages(String largeImage) {


    }

    public static class StatusPair{
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



    public void sendImage(Uri data, String type, String path, long msg_ids_count,String pixBay) {
        String receiverId= getReceiver(null).copy().getUserId();

        Log.d(TAG, "sendFile: receiver Id : "+receiverId);
        HashMap<String,Long> newcount=new HashMap<>(  );
        newcount.put("count",msg_ids_count+1);
        FirebaseFirestore.getInstance().collection( "Messages" )
                .document( getUser().getUserId()+"to"+receiverId+"messageidscount" )
                .set(newcount, SetOptions.merge());
        File directory=new File( getApplication().getApplicationContext().getFilesDir(),path );
        if(!directory.exists()){
            directory.mkdirs();
        }
        String newmsgid=userId+"to"+receiverId+msg_ids_count;
        String prefix=type.equals( "gif" )?"GIF":"IMG";
        HomeViewModel.StatusPair pair=add();
        File file=new File( getApplication().getApplicationContext().getFilesDir(),path+prefix+newmsgid+"."+type );
        System.out.println("dir path :"+ getApplication().getApplicationContext().getFilesDir().getAbsolutePath() );
        addMsgData( file,newmsgid, Database.Msg.MEDIA_IMG,"You sent a picture ",pair.getIndex(),receiver);
        if(!pixBay.equals("pixa"))
        new ImageSender(userId, receiverId, data.toString(),new Timestamp( new Date().getTime() ).toString(),
                newmsgid, getApplication().getApplicationContext(),type ).start();
        else{
            new Thread(()->{
                try {
                    HttpURLConnection connection=(HttpURLConnection) new URL(data.toString()).openConnection();
                    Bitmap bitmap= BitmapFactory.decodeStream(connection.getInputStream());
                    ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
                    FileOutputStream stream=new FileOutputStream(file);
                    stream.write(outputStream.toByteArray());
                    stream.close();
                    outputStream.close();
                    String serverpath = "images/" + newmsgid + ".png";
                    String serverval=newmsgid+".png";
                    Log.d(TAG, "rec In Thread : "+receiverId);
                Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                OneTimeWorkRequest request=new OneTimeWorkRequest.Builder( FilesSender.class )
                        .setConstraints( constraints )
                        .setInputData( new Data.Builder().putString( "userid",userId)
                                .putString( "recieverid",receiverId )
                                .putString( "msgid",newmsgid )
                                .putString( "serverpath",serverpath )
                                .putString( "servervalue",serverval)
                                .putString( "file",file.getAbsolutePath() )
                                .putString( "fileBlur","/sh.png")
                                .putString( "type","img" )
                                .putString( "serverPathThumb","imagesThumb/"+newmsgid+".png" )
                                .putString( "time", String.valueOf(new Date().getTime())).build())
                        .build();
                WorkManager.getInstance(getApplication().getApplicationContext()).enqueue( request );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }




    }

    public void sendFile(Uri data, Long msg_ids_count) {
        new Thread(()-> {
            User receiverId = receiver;

            Log.d(TAG, "sendFile: receiver Id : " + receiverId.getUserId());
            HashMap<String, Long> newcount = new HashMap<>();
            newcount.put("count", msg_ids_count + 1);
            FirebaseFirestore.getInstance().collection("Messages")
                    .document(getUser().getUserId() + "to" + receiverId + "messageidscount")
                    .set(newcount, SetOptions.merge());
            Cursor cur = getApplication().getApplicationContext().getContentResolver().query(data, null, null, null, null, null);
            assert cur != null;
            cur.moveToFirst();
            String name = cur.getString(cur.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            String newmsgid = userId + "to" + receiverId + msg_ids_count;
            File directory = new File(getApplication().getApplicationContext().getFilesDir(), "sent/file/");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(getApplication().getApplicationContext().getFilesDir(), "sent/file/" + name);
            System.out.println("dir path :" + getApplication().getApplicationContext().getFilesDir().getAbsolutePath());
            HomeViewModel.StatusPair pair = add();
            addMsgData(file, newmsgid, Database.Msg.MEDIA_FILE, name, pair.getIndex(), receiverId);
            new PrepareFileToSend(userId, receiverId.getUserId(), data, new Timestamp(new Date().getTime()).toString(),
                    newmsgid, name, getApplication().getApplicationContext()).start();

            cur.close();
        }).start();


    }


    private void addMsgData(File file, String newmsgid, String mediatype, String name, int index,User user){
        Timestamp time = new Timestamp( new Date().getTime() ) ;
        Message message=new Message();
        message.setMessage(file.getAbsolutePath()  );
        message.setType( Database.Msg.TYPE_SENT );
        message.setOUserId( user.getUserId() );
        message.setMUserId( viewModel.userId );
        message.setMsgId( newmsgid );
        message.setTime( time.getTime() );
        message.setStatus( Database.Msg.STATUS_NOT_SENT );
        message.setMsgMode( user.getUserMode() );
        message.setTransferIndex(index);
        message.setMediaType( mediatype );
        message.setMediaStatus(Database.Msg.MEDIA_STATUS_SENDING);
        OTextDb.getInstance( getApplication().getApplicationContext() ).messageDao().insertAll( message );

        Recent recent=new Recent();
        recent.setMsg( name);
        recent.setType( Database.Recent.TYPE_USER );
        recent.setLocalId(String.valueOf(viewModel.getReceiver(null).getLocalId()));
        recent.setMUserId( userId );
        recent.setOUserId( user.getUserId() );
        recent.setName( user.getUserName() );
        recent.setMsgId( newmsgid );
        recent.setRecentMode( user.getUserMode() );
        recent.setMediaType(mediatype );
        recent.setTime(  time.getTime()  );
        recent.setStatus( Database.Recent.STATUS_SEEN );
        OTextDb.getInstance( getApplication().getApplicationContext() ).recentDao().insert( recent );




    }


}
