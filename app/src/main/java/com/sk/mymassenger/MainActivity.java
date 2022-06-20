package com.sk.mymassenger;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.ViewModel;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN=1;
    private static final int RC_VERIFY_EMAIL =2 ;
    private static final int RC_SETUP =3 ;
    private static final String TAG = "MainActivity deb";
    SQLiteDatabase readable;
   SQLiteDatabase writable;
   String userid= FirebaseAuth.getInstance().getCurrentUser().getUid();
   String Username;
   String localid;
   RecyclerView recyclerView;
    CollectionReference Messages;

  private  AlarmManager alarmManager;
   // private BroadCastMain broadcast;
    private ListenerRegistration listenmsg;
    private ListenerRegistration listeninfo;
    private View option;
    private ImageView profile;
    private boolean shouldStart;
    private LinearLayout linearLayout;
    private Button action_chat;
    private Button action_btn;
    private ValueEventListener valueevent;
    private DatabaseReference ref;
    private ViewModel viewModel;
    private ConversationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        linearLayout=findViewById( R.id.action );
        action_chat=findViewById( R.id.action_btn_new_chat );

        recyclerView=findViewById( R.id.conversation_list );
        option=findViewById( R.id.select_opt );
      //  oTextDb db=new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,1 );
     //   readable=db.getReadableDatabase();
      //  writable=db.getWritableDatabase();
        IntentFilter intentFilter=new IntentFilter(Database.DB_DATA_CHANGED);
        intentFilter.addAction( Database.DB_DATA_CHANGED );
        //broadcast=new BroadCastMain(recyclerView,readable,userid);
        //registerReceiver( broadcast,intentFilter );
        registerReceiver(new  AppClosedBroad(),new IntentFilter( ) );
        alarmManager= (AlarmManager) getSystemService( ALARM_SERVICE );
      /*  Cursor localuserdata=db.getReadableDatabase().query(
                Database.User.TBL_USER,
                null,Database.User.USER_ID+" = ?",
                new String[]{userid},null,null,null);


       */
        Messages=FirebaseFirestore.getInstance().collection( "Messages" );
         User localuserdata= OTextDb.getInstance( getApplicationContext() ).userDao().getUser( userid );
        System.out.println( userid);
        localid= String.valueOf( localuserdata.getLocalId() );
        Username=localuserdata.getUserName();
        profile=findViewById( R.id.main_profile );

        action_btn=findViewById( R.id.action_btn );
        String profilepath=localuserdata.getProfilePicture();
        Log.d( TAG, "onCreate: user profile "+profilepath );
        if(profilepath!=null) {

            final int density=getResources().getDisplayMetrics().densityDpi;
            int size=40*(density/160);
            File profilefile = new File( getFilesDir(), profilepath.substring( profilepath.lastIndexOf( "/" ) ) );
            new Handler( getMainLooper() ).post( () -> Glide.with( getApplicationContext() ).load( FileProvider.getUriForFile( getApplicationContext(),
                    "com.sk.mymassenger.fileProvider", profilefile ) ).centerCrop().into( profile ) );
        }
         listenmsg=FirebaseFirestore.getInstance().collection( "Messages" ).document( "to" + userid ).addSnapshotListener( (value, error) -> {

             Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
             OneTimeWorkRequest work= new OneTimeWorkRequest.Builder(MessagesFatcher.class).setConstraints( constraints )
                     .addTag("MessageServiceO")
                     .setInputData(
                             new Data.Builder().putString("UserId",userid).putString( "Username",Username)
                                     .putBoolean( "get",true ).build()
                     ).build();
             WorkManager.getInstance( getApplicationContext()).enqueue( work);
         } );
        listenmsg=FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + userid ).addSnapshotListener( (value, error) -> {

            Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
            OneTimeWorkRequest work= new OneTimeWorkRequest.Builder(BlockInfo.class).setConstraints( constraints )
                    .addTag("BlockInfoO")
                    .setInputData(
                            new Data.Builder().putString("UserId",userid).build()
                    ).build();
            WorkManager.getInstance( getApplicationContext()).enqueue( work);
        } );
        listeninfo=FirebaseFirestore.getInstance().collection( "Messages" ).document( "info" + userid ).addSnapshotListener( (value, error) -> {
            if (value != null) {
                Constraints constraints=new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                OneTimeWorkRequest workinfo= new OneTimeWorkRequest.Builder(InfoTracker.class).setConstraints( constraints )
                        .addTag("MessageInfoTrackerO")
                        .setInputData(
                                new Data.Builder().putString("UserId",userid)
                                        .putBoolean( "get",true ).build()
                        ).build();
                WorkManager.getInstance(getApplicationContext() ).enqueue( workinfo);

            }
        } );

        ref=FirebaseDatabase.getInstance("https://otext-2f0cc-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users/"+userid);
        HashMap<String,Object> hashMap=new HashMap<>( );
        hashMap.put( "status","online" );
        hashMap.put( "last seen",ServerValue.TIMESTAMP );
        ref.setValue( hashMap ).addOnCompleteListener( new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText( getApplicationContext(),"Done",Toast.LENGTH_SHORT ).show();
            }
        } );
        recyclerView.setLayoutManager( new LinearLayoutManager( null ) );

        System.out.println( "Adapter added" );
        action_btn.setOnLongClickListener( new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //startActivity( new Intent( MainActivity.this,MainActivity2.class ) );
                if(AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_NO)
                AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_YES );
                else
                    AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_NO );
                return true;
            }
        } );
        viewModel=new ViewModelProvider( MainActivity.this ).get( ViewModel.class );
        LiveData<List<Recent>> recent=viewModel.getRecentDao().getRecent( userid, Database.Recent.MODE_PUBLIC );
        new Handler( getMainLooper() ).post( ()->{
            List<Recent> data=recent.getValue();
            setAdapter( data );
        } );
        recent.observe( MainActivity.this, new Observer<List<Recent>>() {
            @Override
            public void onChanged(List<Recent> recents) {
              setAdapter( recents );
            }
        } );
        action_btn.setOnClickListener( view -> new Handler( getMainLooper() ).post( ()-> animate(action_chat,linearLayout,action_btn) ) );
    }
    public void openProfile(View v){
        shouldStart=false;
        Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
        intent.putExtra( "SettingMode" ,"NoExtraSetting");
        startActivity( intent );
    }
    public void openSetting(View v){
        if(action_chat.getVisibility()==View.VISIBLE)

        shouldStart=false;
        Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
        intent.putExtra( "SettingMode" ,"ExtraSetting");
        startActivity( intent );
        animate(action_chat,linearLayout,action_btn);
    }
   public void startNewChat(View v){
        if(action_chat.getVisibility()==View.VISIBLE)
            animate(action_chat,linearLayout,action_btn);
        shouldStart=false;
        Intent intent=new Intent(MainActivity.this,ContactActivity.class);
        startActivity( intent );
       animate(action_chat,linearLayout,action_btn);
    }
    public void signOut(View v){


        new AlertDialog.Builder(new ContextThemeWrapper( MainActivity.this,R.style.AppTheme ) ).setTitle( "Sign out" )
                .setMessage( "Are you sure ? " )
                .setCancelable( true )
                .setNegativeButton( "cancel" ,null)
                .setPositiveButton( "Sign out",(d,i)->{
             if(ref!=null) {
                 if(valueevent!=null)
                 ref.removeEventListener( valueevent );
                 ref.setValue( markOfline() );
             }
             AuthUI.getInstance().signOut( getApplicationContext() )
                        .addOnCompleteListener((task)-> {

                            shouldStart = false;
                            List<AuthUI.IdpConfig> providers = Arrays.asList(
                                    new AuthUI.IdpConfig.EmailBuilder().build(),
                                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                                    new AuthUI.IdpConfig.GoogleBuilder().build() );
                            WorkManager.getInstance( getApplicationContext() ).cancelAllWork();
                            if (listeninfo != null)
                                listeninfo.remove();
                            if (listenmsg != null)
                                listenmsg.remove();
                            startActivityForResult(
                                    AuthUI.getInstance()
                                            .createSignInIntentBuilder()
                                            .setAvailableProviders( providers )
                                            .setTheme( R.style.AppTheme )

                                            .build(),
                                    RC_SIGN_IN );
                        } ); } ).create().show();
        animate(action_chat,linearLayout,action_btn);
    }
    public void animate(Button action_chat, LinearLayout linearLayout, Button action_btn){
        final int density=getResources().getDisplayMetrics().densityDpi;
        if(action_chat.getVisibility()==View.GONE) {
            ValueAnimator valueAnimator = ValueAnimator.ofInt( 50*(density/160), 50*(density/160)*4 );
            valueAnimator.addUpdateListener( valueAnimator1 -> {
                int val = (int) valueAnimator1.getAnimatedValue();
                linearLayout.getLayoutParams().width = val;
                if (val >=100*(density/160)) {
                    linearLayout.findViewById( R.id.action_btn_new_chat ).setVisibility( View.VISIBLE );
                }
                if (val >=140*(density/160)) {
                    linearLayout.findViewById( R.id.action_btn_open_setting ).setVisibility( View.VISIBLE );
                }
                if (val >=190*(density/160)) {
                    linearLayout.findViewById( R.id.action_btn_sign_out).setVisibility( View.VISIBLE );
                }
                action_btn.setRotation( 45);

                linearLayout.requestLayout();
            } );
            valueAnimator.setTarget( linearLayout );
            valueAnimator.setDuration( 500 );
            valueAnimator.start();
        }else{
            ValueAnimator valueAnimator = ValueAnimator.ofInt( 50*(density/160)*4, 50*(density/160) );
            valueAnimator.addUpdateListener( valueAnimator12 -> {
                int val = (int) valueAnimator12.getAnimatedValue();
                linearLayout.getLayoutParams().width = val;
                if (val <=100*(density/160)) {
                    linearLayout.findViewById( R.id.action_btn_new_chat ).setVisibility( View.GONE);
                }
                if (val <=140*(density/160)) {
                    linearLayout.findViewById( R.id.action_btn_open_setting ).setVisibility( View.GONE);
                }
                if (val <=190*(density/160)) {
                    linearLayout.findViewById( R.id.action_btn_sign_out).setVisibility( View.GONE );
                }
                linearLayout.requestLayout();
                action_btn.setRotation( 0);

            } );
            valueAnimator.setTarget( linearLayout );
            valueAnimator.setDuration( 500 );
            valueAnimator.start();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        shouldStart=true;
        Intent intent=new Intent(getApplicationContext(),AppClosedBroad.class);
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent=PendingIntent.getBroadcast( getApplicationContext(),
                1,intent,PendingIntent.FLAG_UPDATE_CURRENT);


        alarmManager.cancel( pendingIntent  );
        stopService( new Intent( getApplicationContext(),MessageService.class ));
       /* Cursor data=readable.query(
                Database.Recent.TBL_RECENT,null,Database.Recent.MUSER_ID+" = ? AND "+ Database.Recent.RECENT_MODE+" = ? ",
                new String[]{userid, Database.Recent.MODE_PUBLIC},null,null, Database.Recent.TIME+" DESC" );

        */






       ref.onDisconnect().setValue( markOfline()).addOnCompleteListener( new OnCompleteListener<Void>() {
           @Override
           public void onComplete(@NonNull Task<Void> task) {

           }
       } );
    }
    private HashMap<String,Object> markOfline(){
        HashMap<String,Object> hashMap=new HashMap<>( );
        hashMap.put( "status","offline" );
        hashMap.put( "last seen",ServerValue.TIMESTAMP );
        return hashMap;
    }
    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();

        if(shouldStart)
          startService( new Intent( getApplicationContext(),MessageService.class ));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // unregisterReceiver( broadcast );
       if(valueevent!=null)
        ref.removeEventListener(valueevent  );



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==RC_SIGN_IN){

            if(resultCode==RESULT_OK){
                //check if sign in successful
                Toast.makeText( getApplicationContext(),"Signed In",Toast.LENGTH_SHORT ).show();
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(user.getEmail()!=null) {
                    if (user.isEmailVerified()) {
                        userid = user.getUid();
                        new Handler( getMainLooper() ).post( () -> getUserData( user ) );
                    } else {
                        startActivityForResult( new Intent( MainActivity.this, EmailVerification.class ), RC_VERIFY_EMAIL );
                    }
                }else{
                    userid = user.getUid();
                    new Handler( getMainLooper() ).post( () -> getUserData( user ) );
                }

            }else{
                finish();
            }
        }else if(requestCode==RC_VERIFY_EMAIL){
            if(resultCode==RESULT_OK){
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(user.getEmail()!=null) {
                    if (user.isEmailVerified()) {
                        userid = user.getUid();
                        new Handler( getMainLooper() ).post( () -> getUserData( user ) );
                    } else {
                        Toast.makeText( getApplicationContext(), "Some error occurred ", Toast.LENGTH_SHORT ).show();
                        finish();
                    }
                }else{
                    userid = user.getUid();
                    new Handler( getMainLooper() ).post( () -> getUserData( user ) );
                }
            }else{
                finish();
            }
        }else if(requestCode==RC_SETUP){
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
           /* Cursor c = readable.query( Database.User.TBL_USER, null,
                    Database.User.USER_ID + " = ? ",
                    new String[]{user.getUid()}, null, null, null );

            */
            User user1=viewModel.getUserDao().getUser( user.getUid() );
            if (user1!=null){
                Toast.makeText( getApplicationContext(),"Some error occurred ",Toast.LENGTH_SHORT ).show();
                FirebaseAuth.getInstance().signOut();
                finish();
            }
        }

    }
    public void getUserData(FirebaseUser user){
        FirebaseFirestore.getInstance().collection( "users/" ).document(user.getUid()).get().addOnSuccessListener( documentSnapshot -> {
            if(documentSnapshot.exists()){
               // SQLiteDatabase readable=new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION ).getReadableDatabase();
              /*  Cursor c = readable.query( Database.User.TBL_USER, null,
                        Database.User.USER_ID + " = ? ",
                        new String[]{user.getUid()}, null, null, null );

               */
                User user1=viewModel.getUserDao().getUser( user.getUid() );
                if (user1==null) {

                    FirebaseFirestore.getInstance().collection( "users" ).whereEqualTo( Database.Server.USER_ID, user.getUid() ).get().addOnCompleteListener( task -> {
                        new UserDataFetcher( getApplicationContext(),false ).getUserData( task );
                      /*  Cursor cur = readable.query( Database.User.TBL_USER, null,
                                Database.User.USER_ID + " = ? ",
                                new String[]{user.getUid()}, null, null, null );
                        cur.moveToFirst();

                       */
                        User userr=viewModel.getUserDao().getUser( user.getUid() );
                        String path=userr.getProfilePicture();
                        if(path!=null) {
                            File profilefile = new File( getApplication().getFilesDir(), path.substring( path.lastIndexOf( "/" ) ) );
                            Glide.with( getApplicationContext()).load( FileProvider.getUriForFile( getApplicationContext(),
                                    "com.sk.mymassenger.fileProvider", profilefile ) ).centerCrop().into( profile );
                        }
                        Constraints constraints = new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                        OneTimeWorkRequest workinfo = new OneTimeWorkRequest.Builder( InfoTracker.class ).setConstraints( constraints )
                                .addTag( "MessageInfoTrackerO" )
                                .setInputData(
                                        new Data.Builder().putString( "UserId", user.getUid() )
                                                .putBoolean( "get", true ).build()
                                ).build();
                        WorkManager.getInstance( getApplicationContext() ).enqueueUniqueWork( "MessageInfoTrackerO", ExistingWorkPolicy.KEEP, workinfo );
                        LiveData<List<Recent>> recent=viewModel.getRecentDao().getRecent( userid, Database.Recent.MODE_PUBLIC );
                        new Handler( getMainLooper() ).post( ()->{
                            List<Recent> data=recent.getValue();
                            setAdapter(data);
                        } );
                    } );
                }else {
                    Constraints constraints = new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
                    OneTimeWorkRequest workinfo = new OneTimeWorkRequest.Builder( InfoTracker.class ).setConstraints( constraints )
                            .addTag( "MessageInfoTrackerO" )
                            .setInputData(
                                    new Data.Builder().putString( "UserId", user.getUid() )
                                            .putBoolean( "get", true ).build()
                            ).build();
                    WorkManager.getInstance( getApplicationContext() ).enqueueUniqueWork( "MessageInfoTrackerO", ExistingWorkPolicy.KEEP, workinfo );

                    userid=user.getUid();
                    LiveData<List<Recent>> recent=viewModel.getRecentDao().getRecent( userid, Database.Recent.MODE_PUBLIC );
                    new Handler( getMainLooper() ).post( ()->{
                        List<Recent> data=recent.getValue();
                        setAdapter( data );
                    } );
                    String path=user1.getProfilePicture();
                    if(path!=null) {
                        File profilefile = new File( getApplication().getFilesDir(), path.substring( path.lastIndexOf( "/" ) ) );
                        Glide.with( getApplicationContext()).load( FileProvider.getUriForFile( getApplicationContext(),
                                "com.sk.mymassenger.fileProvider", profilefile ) ).into( profile );
                    }

                }
            }else{
                Intent intent=new Intent(MainActivity.this,SetUpProfile.class);
                startActivityForResult( intent,RC_SETUP );
            }
        } );
    }

    private void setAdapter(List<Recent> data) {
        if(data!=null&&data.size()>0){
            if(adapter==null)
                adapter=new ConversationAdapter(false,data.size(),data,userid,option);
            else{
                adapter.setData( false,data.size(),data );
            }
            recyclerView.setAdapter(adapter);
        }

        else{
            if(adapter==null)
                adapter=new ConversationAdapter(true,1,null,userid,option);
            else{
                adapter.setData( true,1,null );
            }
            recyclerView.setAdapter(adapter);
        }
    }



   /* public class BroadCastMain extends BroadcastReceiver {
        RecyclerView recyclerView ;
        SQLiteDatabase readable;
        String userid;



        public BroadCastMain(RecyclerView recyclerView, SQLiteDatabase readable, String userid) {
            this.readable=readable;
            this.recyclerView=recyclerView;
            this.userid=userid;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            Cursor data=readable.query(
                    Database.Recent.TBL_RECENT,null,Database.Recent.MUSER_ID+" = ? AND "+ Database.Recent.RECENT_MODE+" = ? ",
                    new String[]{userid, Database.Recent.MODE_PUBLIC},null,null, Database.Recent.TIME+" DESC" );
            new Handler( getMainLooper() ).post( ()->{
            int count= data.getCount();

            if(count==0){
                recyclerView.setAdapter(new ConversationAdapter(true,1,null,readable,userid,option));
            }else{

                recyclerView.setAdapter(new ConversationAdapter(false,count,data,readable,userid,option));
            }
            System.out.println( "Recieved" );
           } );
        }

    }*/



    class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.MViewHolder>{
        private  int size;
        private boolean setEmpty;

        private boolean selectionMode;
        private ArrayList<String> selectedList;

        private long count=1L;
        private List<Recent> data;
        private  String userid;
        private View option;
        String muser="";
        String ouser="";
        public void setData(boolean b,long c,List<Recent> data){
            this.data=data;
            count=c;
            setEmpty=b;
        }
        public ConversationAdapter(boolean set, long c, List<Recent> data, String uuserid, View option) {
            setEmpty=set;
            this.userid=uuserid;
            count=c;
            this.option=option;


            if(data!=null&&data.size()>0) {
                int des=getResources().getDisplayMetrics().densityDpi;
                size=50*(des/160);
                muser=data.get( 0 ).getMUserId();
                ouser=data.get(0).getOUserId();
            }
            this.data=data;
            option.findViewById( R.id.opt_delete ).setOnClickListener( (vi)->{
                Intent intent=new Intent(MainActivity.this,DeleteMessage.class);
                intent.putStringArrayListExtra( "ids",selectedList);
                intent.putExtra( "mode", Database.Msg.TBL_MSG);
                intent.putExtra( Database.Msg.MUSER_ID,userid);
                intent.putExtra( Database.Msg.OUSER_ID,ouser);
                MainActivity.this.startService(intent);
                selectedList.clear();
                option.setVisibility( View.GONE );
            } );
        }

        @NonNull
        @Override
        public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if(setEmpty){
                View view= LayoutInflater.from(parent.getContext()).inflate( R.layout.empty_conversation,parent,false );
                return new MViewHolder( view );
            }
            View view= LayoutInflater.from(parent.getContext()).inflate( R.layout.conversation_layout,parent,false );

            return new MViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MViewHolder holder, int position) {
            if(!setEmpty) {
                Recent recent=data.get(position);
                holder.getName().setText( recent.getName() );

               /* Cursor cur=db.query( Database.User.TBL_USER, new String[]{Database.User.PROFILE_PICTURE},
                        Database.User.USER_ID+" = ?",new String[]{ids.get( position )},
                        null,null ,null);
                cur.moveToFirst();

                */
                User user=viewModel.getUserDao().getUser( recent.getOUserId() );
                String path=user.getProfilePicture();
                Log.d( TAG, "onBindViewHolder: " +user+path);
                if(path!=null&&path.length()>5) {
                    File imageFile = new File( getFilesDir(), path.substring( path.lastIndexOf( "/" ) + 1 ) );

                    Uri imguri = FileProvider.getUriForFile( getApplicationContext(), "com.sk.mymassenger.fileProvider", imageFile );
                    Glide.with( getApplicationContext() ).load( imguri ).centerCrop().into( holder.getProfile() );
                    holder.getProfile().setOnClickListener( (v)->{
                        shouldStart=false;
                        Log.d( "MainActivity ", "onbind : imageuri : "+imguri.toString() );
                        Intent intent=new Intent( getApplicationContext(),ImageActivity.class );
                        intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.setData( imguri );
                        startActivity( intent );
                    } );
                }
                Log.d( "MViewHolder", "onBindViewHolder: "+recent.getMUserId());
                holder.getDate().setText( new OnlineUsers.ResolveDate().resolve( new Date(recent.getTime()),"recent" ) );
                if(recent.getMediaType().equals( Database.Recent.MEDIA_IMG )){
                    holder.getImage_msg().setVisibility( View.VISIBLE );
                    holder.getImage_msg().setImageResource( R.drawable.ic_baseline_image_24 );
                }else if(recent.getMediaType().equals( Database.Recent.MEDIA_FILE)){
                    holder.getImage_msg().setVisibility( View.VISIBLE );
                    holder.getImage_msg().setImageResource( R.drawable.ic_baseline_insert_drive_file_24 );
                }else{
                    holder.getImage_msg().setVisibility( View.GONE );

                }
                holder.getLastMSG().setText( recent.getMsg());
                if(recent.getStatus().equals( Database.Recent.STATUS_NOT_SEEN ))
                    holder.getCount().setVisibility( View.VISIBLE );
                else
                    holder.getCount().setVisibility( View.GONE);
                holder.getRoot().setOnLongClickListener( view -> {
                    holder.getRoot().setBackgroundColor( Color.parseColor( "#34ae5aa4" )  );
                    if(selectedList==null){
                        selectedList=new ArrayList<>(  );
                    }
                    selectedList.add( data.get( position ).getOUserId() );
                    selectionMode=true;
                    option.setVisibility( View.VISIBLE );
                    return true;
                } );
                holder.getRoot().setOnClickListener( view -> {
                    if(selectionMode) {
                        String temp=data.get( position ).getOUserId() ;
                        if(selectedList.contains(temp )) {

                            holder.getRoot().setBackgroundColor( Color.parseColor( "#00000000" ) );
                            selectedList.remove( temp );

                        }else{
                            holder.getRoot().setBackgroundColor( Color.parseColor( "#34ae5aa4" ) );
                            selectedList.add( temp );
                        }
                        if(selectedList.size()<1){
                            option.setVisibility( View.GONE );
                            selectionMode=false;
                        }

                    }else {
                        Intent intent = new Intent( getApplicationContext(), ConversationActivity.class );
                        intent.putExtra( "User",data.get( position ).getOUserId() );
                        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                        intent.putExtra( "MUser", userid );
                        shouldStart=false;
                        holder.getProfile().setTransitionName( "recPic" );
                        ActivityOptions options=ActivityOptions.makeSceneTransitionAnimation( MainActivity.this,
                                 holder.getName(), ViewCompat.getTransitionName( holder.getName() ) );
                        getApplicationContext().startActivity( intent/* ,options.toBundle()*/);

                    }
                } );

            }else{
                holder.getStart_new().setOnClickListener( view -> {
                    Intent intent=new Intent( getApplicationContext(),ContactActivity.class );
                    intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                    getApplicationContext().startActivity(intent);
                } );
            }
        }

        @Override
        public int getItemCount() {
            return (int) count;
        }
        class MViewHolder extends RecyclerView.ViewHolder{
            private  TextView date;

            public ImageView getImage_msg() {
                return image_msg;
            }

            private  ImageView image_msg;
            private TextView name;
            private TextView lastMSG;
            private ImageView profile;
            private View root;

            public Button getStart_new() {
                return start_new;
            }

            private Button start_new;
            public TextView getName() {
                return name;
            }

            public TextView getLastMSG() {
                return lastMSG;
            }

            public ImageView getProfile() {
                return profile;
            }

            public TextView getCount() {
                return count;
            }
            public View getRoot() {
                return root;
            }

            private TextView count;
            public MViewHolder(@NonNull View itemView) {
                super( itemView );
                if(!setEmpty) {
                    root=itemView;
                    name = itemView.findViewById( R.id.conversation_name );
                    lastMSG = itemView.findViewById( R.id.last_msg );
                    count = itemView.findViewById( R.id.last_msg_count );
                    profile = itemView.findViewById( R.id.conversation_pic );
                    image_msg=itemView.findViewById( R.id.last_msg_image );
                    date=itemView.findViewById( R.id.last_msg_date );
                }else {
                    start_new=itemView.findViewById( R.id.start_new );

                }
            }

            public TextView getDate() {
                return date;
            }
        }
    }
}