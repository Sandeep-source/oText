package com.sk.mymassenger;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.data.MessageData;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.ViewModel;
import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.message.Message;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class ConversationActivity extends AppCompatActivity {
    private static final int GET_IMAGE = 1;
    private static final int RC_CHAT_SETTING =2 ;
    private static final String TAG = "Debbug con activity";
    private MEditText msg_editer;
   // private oTextDb db;
    private String recieverid;
    private String userid;
    private String Recievername;
    private String userMode;
    private String Recieverlocalid;
    boolean can=true;
    boolean byme=false;
    private Handler handler;
    private long msg_ids_count;
    private RecyclerView recyclerView;
    private DocumentReference doc;
    private View option;
    private int GET_FILE=3;
    private Uri imguri;
    private BlockBroad blockbroadcast;
    private PrepareMessage prepareMessage;
    private ArrayList<MessageData> list;
    private ConversationActivityAdapter adapter;
    private ViewModel viewModel;
    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_conversation );
        list=new ArrayList<>(  );
        recyclerView=findViewById( R.id.conversation_activity_list );
        String reciever_id=getIntent().getStringExtra( "User" );

        option=findViewById( R.id.select_opt );
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
       // db=new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION );
        viewModel=new ViewModelProvider( this ).get( ViewModel.class );
        User receiver=viewModel.getUserDao().getUser( reciever_id );
       /* Cursor cur=db.getReadableDatabase().query(
                Database.User.TBL_USER,
                null,Database.User.USER_ID+" = ?",
                new String[]{reciever_id},null,null,null);

        cur.moveToFirst();

        */
        handler=new Handler( getMainLooper() );

        userid=user.getUid();

        String mail=receiver.getEmail();
        String phone=receiver.getPhoneNo();
        String info_id = mail != null && mail.length() > 0 ? mail : phone;
        ((TextView)findViewById( R.id.conversation_activity_id_info )).setText( info_id );
    //    db=new oTextDb( getApplicationContext(),Database.O_TEXT_DB ,null,Database.DB_VERSION);
        findViewById( R.id.back_conversation_activity ).setOnClickListener( (v)-> super.onBackPressed() )
        ;
        Recieverlocalid= String.valueOf( receiver.getLocalId() );
        recieverid=reciever_id;
        userMode=receiver.getUserMode();
        ((ImageView) findViewById( R.id.conversation_profile_img )).setTransitionName( "recPic" );
        Log.d( "debug", "onCreate: mode "+userMode );
        String path=receiver.getProfilePicture();
        if(path!=null&&path.length()>5) {
          handler.post( ()-> {
              File imageFile = new File( getFilesDir(), path.substring( path.lastIndexOf( "/" ) + 1 ) );
              imguri = FileProvider.getUriForFile( getApplicationContext(), "com.sk.mymassenger.fileProvider", imageFile );
              Glide.with( getApplicationContext() ).load( imguri ).centerCrop().into( (ImageView) findViewById( R.id.conversation_profile_img ) );
          });
        }
        Recievername=receiver.getUserName();
        /*
        db.getWritableDatabase().update( Database.Recent.TBL_RECENT,con,
                Database.Recent.MUSER_ID+" = ? AND "+ Database.Recent.OUSER_ID+" = ? ",
                new String[]{userid,reciever_id});*/
        viewModel.getRecentDao().updateStatus( user.getUid(),recieverid, Database.Recent.STATUS_SEEN );
        msg_editer=findViewById( R.id.conversation_enter_msg );

        LinearLayoutManager layoutManager=new LinearLayoutManager( getApplicationContext() );
        layoutManager.setStackFromEnd( true );
        recyclerView.setLayoutManager( layoutManager );
       new loadMessageData(handler,null).start();
        CollectionReference collection = FirebaseFirestore.getInstance().collection( "Messages" );
        doc= collection.document( userid+"to"+reciever_id+"messageidscount" );
        doc.get().addOnCompleteListener( task -> {
            DocumentSnapshot documentSnapshot=task.getResult();
            if(documentSnapshot!=null&&documentSnapshot.exists()) {
                msg_ids_count = documentSnapshot.getLong( "count" );
            }else{
                msg_ids_count=0;
            }
        } ).addOnFailureListener( e -> {

        } );

        findViewById( R.id.profile_info ).setOnClickListener( (v)->{
            Intent intent=new Intent( getApplicationContext(),Chat_Setting.class );
            intent.putExtra( "Reciever",reciever_id );
            startActivityForResult( intent,RC_CHAT_SETTING );
        } );


    }

    private void animate(ImageButton b1,ImageButton b2){
        if(b1.getVisibility()==View.GONE){
            ValueAnimator valueAnimator=ValueAnimator.ofInt( 1, 60);
            valueAnimator.addUpdateListener( valueAnimator1 -> {
                int value= (int) valueAnimator.getAnimatedValue();
                b1.getLayoutParams().height=value;
                b1.requestLayout();
                b2.getLayoutParams().height=value;
                b2.requestLayout();
            } );
            b1.setVisibility( View.VISIBLE );
            b2.setVisibility( View.VISIBLE );
            valueAnimator.setDuration( 300 );
            valueAnimator.start();
        }else{
            ValueAnimator valueAnimator=ValueAnimator.ofInt( 60, 0);
            valueAnimator.addUpdateListener( valueAnimator1 -> {
                int value= (int) valueAnimator.getAnimatedValue();
                b1.getLayoutParams().height=value;
                b1.requestLayout();
                b2.getLayoutParams().height=value;
                b2.requestLayout();
                if(value<3){
                    b1.setVisibility( View.GONE );
                    b2.setVisibility( View.GONE );
                }
            } );
            b1.setVisibility( View.VISIBLE );
            b2.setVisibility( View.VISIBLE );
            valueAnimator.setDuration( 300 );
            valueAnimator.start();
        }

    }
    protected void onResume() {
        super.onResume();
        msg_editer.setOnGifAttach((uri)->{
             sendImage( uri,"gif","sent/gifs/" );
        });
        CoordinatorLayout layout=findViewById( R.id.con_root );
        layout.getViewTreeObserver().addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r=new Rect(  );
                layout.getWindowVisibleDisplayFrame( r );
                int height=layout.getRootView().getHeight();
                HashMap<String,Object> map=new HashMap<>(  );
                if((height-r.bottom)>(height*0.15)){
                    map.put( "status","typing..." );
                    map.put( "for",recieverid);
                    map.put( "last seen",new Date( ).getTime() );
                    FirebaseDatabase.getInstance("https://otext-2f0cc-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference( "Users/"+userid ).setValue(map);


                }else{
                    map.put( "status","online" );
                    map.put( "last seen",new Date( ).getTime() );
                    FirebaseDatabase.getInstance("https://otext-2f0cc-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference( "Users/"+userid ).setValue(map);
                }
            }
        } );
        HashMap<String,Object> status_info1=new HashMap<>(  );
      /*  Cursor block=db.getReadableDatabase().query( Database.Block.TBL_BLOCK,null, Database.Block.MUSER_ID+" = ? AND "+ Database.Block
                .OUSER_ID+" = ? " ,new String[]{userid,recieverid},null,null,null);


       */
        Block block=viewModel.getBlockDao().getBlock( userid,recieverid );
        if(block!=null){
            can=false;
            byme=block.getType().equals(Database.Block.BY_ME);
        }else{
            can=true;
        }
        System.out.println( "user : "+userid );
        status_info1.put(userid, Arrays.asList( new Timestamp( new Date(  ).getTime() ).toString(),Database.Msg.STATUS_SEEN ) );
        FirebaseFirestore.getInstance().collection( "Messages" ).document( "info"+recieverid)
                .set(status_info1, SetOptions.mergeFields(userid));

        ImageButton buttonSendImg=findViewById( R.id.conversation_send_btn_img );

        ImageButton buttonSendfile=findViewById( R.id.conversation_send_btn_file);
        buttonSendfile.setOnClickListener( (v)->{
            handler.post( ()-> animate( buttonSendfile,buttonSendImg ) );
            Intent getImage=new Intent(Intent.ACTION_GET_CONTENT);
            getImage.setType( "*/*" );
           startActivityForResult( getImage,GET_FILE );
        } );
        buttonSendImg.setOnClickListener( (v)->{
            handler.post( ()-> animate( buttonSendfile,buttonSendImg ) );
            Intent getImage=new Intent(Intent.ACTION_GET_CONTENT);
            getImage.setType( "image/*" );
            startActivityForResult( getImage,GET_IMAGE );
        } );
        findViewById( R.id.extra_option ).setOnClickListener( (v)-> handler.post( ()-> animate( buttonSendfile,buttonSendImg ) ) );
        doc.addSnapshotListener( (value, error) -> {
            if(value!=null&&value.exists())
                msg_ids_count=value.getLong("count");
        } );
        ((TextView)findViewById( R.id.conversation_activity_name )).setText(Recievername);


        findViewById(R.id.conversation_send_btn).setOnClickListener( (view)->{
            if(can) {
                String msg = msg_editer.getText().toString();
                msg_editer.setText( "" );
                HashMap<String, Long> newcount = new HashMap<>();
                newcount.put( "count", msg_ids_count + 1 );
                doc.set( newcount, SetOptions.merge() );
                String newmsgid = userid + "to" + recieverid + msg_ids_count;
                msg = msg.trim();
                if (msg.length() == 0)
                    return;
                Message message=new Message();
                message.setMessage(msg );
                message.setType( Database.Msg.TYPE_SENT );
                message.setOUserId( recieverid );
                message.setMUserId( userid );
                message.setMsgId( newmsgid );
                message.setStatus( Database.Msg.STATUS_NOT_SENT );
                message.setMsgMode( userMode );
                message.setMediaType( Database.Msg.MEDIA_TEXT );

                if(prepareMessage==null) {
                    prepareMessage = new PrepareMessage(message,false,getApplicationContext());
                    prepareMessage.start();
                }else{
                    prepareMessage.getMessage().setMsgMode( userMode  );
                    prepareMessage.getMessage().setMessage( msg );
                    prepareMessage.getMessage().setMsgId( newmsgid );
                    prepareMessage.start();
                }
            }else if(byme){
                new AlertDialog.Builder( new ContextThemeWrapper(ConversationActivity.this,R.style.AppTheme  ) )
                        .setNegativeButton( "Cancle" ,null)
                        .setCancelable( true ).setMessage( "Unblock User first to send messages." )
                        .setPositiveButton( "Unblock", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                HashMap<String, Object> blockValue = new HashMap<>();
                                blockValue.put( recieverid, FieldValue.delete() );
                                FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + userid ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
                                    blockValue.clear();
                                    blockValue.put( userid,FieldValue.delete());
                                    FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + recieverid ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid1 -> {
                                       // oTextDb db = new oTextDb( getApplicationContext(), Database.O_TEXT_DB, null, Database.DB_VERSION );
                                      //  db.getWritableDatabase().delete( Database.Block.TBL_BLOCK,
                                        //        Database.Block.MUSER_ID + " = ? AND " + Database.Block.OUSER_ID + " = ? ",
                                          //      new String[]{userid,recieverid} );
                                        OTextDb.getInstance( getApplicationContext() ).blockDao().delete( userid,recieverid, Database.Block.BY_ME );
                                        byme=false;
                                        can=true;
                                    } ).addOnFailureListener( e -> {

                                    } );
                                } ).addOnFailureListener( e -> {

                                } );

                            }
                        } )
                        .create().show();
            }else{
                String msg = msg_editer.getText().toString();
                msg_editer.setText( "" );
                HashMap<String, Long> newcount = new HashMap<>();
                newcount.put( "count", msg_ids_count + 1 );
                doc.set( newcount, SetOptions.merge() );
                String newmsgid = userid + "to" + recieverid + msg_ids_count;
                msg = msg.trim();
                if (msg.length() == 0)
                    return;
                Message message=new Message();
                message.setMessage(msg );
                message.setType( Database.Msg.TYPE_SENT );
                message.setOUserId( recieverid );
                message.setMUserId( userid );
                message.setMsgId( newmsgid );
                message.setStatus( Database.Msg.STATUS_NOT_SENT );
                message.setMsgMode( userMode );
                message.setMediaType( Database.Msg.MEDIA_TEXT );
                if(prepareMessage==null) {
                    prepareMessage = new PrepareMessage(message,false,getApplicationContext());
                    prepareMessage.start();

                }else{
                    prepareMessage.setBlocked( true );
                    prepareMessage.getMessage().setMessage( msg );
                    prepareMessage.getMessage().setMsgId( newmsgid );
                    prepareMessage.start();
                }
            }
        } );
        IntentFilter intentFilter=new IntentFilter( Database.DB_DATA_CHANGED );
       /* broadcast=new BroadCastConversation(recieverid,userid,recyclerView,db.getReadableDatabase());
        registerReceiver( broadcast,intentFilter );

        */
        IntentFilter intentFilter2=new IntentFilter( Database.Block.BLOCK_STATUS_CHANGED );
        blockbroadcast=new BlockBroad();
        registerReceiver( blockbroadcast,intentFilter2 );
        Log.d( TAG, "onResume: pre get()" );
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        DatabaseReference ref=FirebaseDatabase.getInstance("https://otext-2f0cc-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference( "Users/"+recieverid );
        ref.keepSynced( true );
        ref.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot!=null&&snapshot.exists())
                new OnlineUsers( handler,findViewById(R.id.conversation_activity_id_info ),snapshot,userid).start();

             }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                  error.toException().printStackTrace();
            }
        } );

        Log.d( TAG, "onResume: post get()" );
    }

    @Override
    protected void onStop() {
        super.onStop();
        //unregisterReceiver( broadcast );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==GET_IMAGE){
            if(resultCode==RESULT_OK){
                handler.post( ()-> sendImage(data.getData(), "png","sent/images/" ) );

            }
        }else if(requestCode==GET_FILE){
            if(resultCode==RESULT_OK){
                handler.post( ()-> sendFile(data.getData()) );

            }
        }else if(requestCode==RC_CHAT_SETTING){
            if(resultCode==RESULT_OK){
                System.out.println("data : "+ data );
                System.out.println("data Array : "+data.getStringArrayExtra( "Results" ) );
                ArrayList<String> action=  data.getStringArrayListExtra(  "Results") ;
                if(action.contains( "Delete" )){
                    finish();
                }
                if(action.contains( "block" )){
                    Block block=viewModel.getBlockDao().getBlock( userid,recieverid );

                    if(block!=null){

                        can=false;
                        byme=block.getType().equals(Database.Block.BY_ME);
                        if(prepareMessage!=null){
                            prepareMessage.setBlocked( true );
                        }
                    }else{
                        can=true;
                        if(prepareMessage!=null){
                            prepareMessage.setBlocked( false);
                        }
                    }
                }
                if(action.contains( Database.User.MODE_PRIVATE )){

                   userMode= Database.User.MODE_PRIVATE;


                }else{
                    userMode= Database.User.MODE_PUBLIC;
                }
            }
        }
    }

    private void sendFile(Uri data) {
        HashMap<String,Long> newcount=new HashMap<>(  );
        newcount.put("count",msg_ids_count+1);
        doc.set(newcount,SetOptions.merge());
       Cursor cur=getContentResolver().query( data,null,null,null,null,null );
       cur.moveToFirst();
       String name=cur.getString( cur.getColumnIndex( OpenableColumns.DISPLAY_NAME ) );
       String newmsgid=userid+"to"+recieverid+msg_ids_count;
       File directory=new File( getApplicationContext().getFilesDir(),"sent/file/" );
       if(!directory.exists()){
           directory.mkdirs();
       }
        File file=new File( getApplicationContext().getFilesDir(),"sent/file/"+name );
        System.out.println("dir path :"+ getFilesDir().getAbsolutePath() );

        addMsgData( file,newmsgid , Database.Msg.MEDIA_FILE,name);
        new PrepareFileToSend( userid, recieverid, data, new Timestamp( new Date().getTime() ).toString(),
                newmsgid,name, getApplicationContext() ).start();




    }

    private void addMsgData(File file, String newmsgid,String mediatype,String name){
         Timestamp time = new Timestamp( new Date().getTime() ) ;
        Message message=new Message();
        message.setMessage(file.getAbsolutePath()  );
        message.setType( Database.Msg.TYPE_SENT );
        message.setOUserId( recieverid );
        message.setMUserId( userid );
        message.setMsgId( newmsgid );
        message.setTime( time.getTime() );
        message.setStatus( Database.Msg.STATUS_NOT_SENT );
        message.setMsgMode( userMode );
        message.setMediaType( mediatype );
        OTextDb.getInstance( getApplicationContext() ).messageDao().insertAll( message );
      /* ContentValues contentValuesMsg = new ContentValues();
       contentValuesMsg.put( Database.Msg.MSG, file.getAbsolutePath() );
       contentValuesMsg.put( Database.Msg.TYPE, Database.Msg.TYPE_SENT );
       contentValuesMsg.put( Database.Msg.OUSER_ID, recieverid );
       contentValuesMsg.put( Database.Msg.MUSER_ID, userid );
       contentValuesMsg.put( Database.Msg.MSG_ID, newmsgid );
       contentValuesMsg.put( Database.Msg.TIME, time );
       contentValuesMsg.put( Database.Msg.STATUS, Database.Msg.STATUS_NOT_SENT );
       contentValuesMsg.put( Database.Msg.MSG_MODE,userMode );
       contentValuesMsg.put( Database.Msg.MEDIA_TYPE, mediatype );
       db.getWritableDatabase().insert( Database.Msg.TBL_MSG, null, contentValuesMsg );
       Intent intent = new Intent( Database.DB_DATA_CHANGED );
       intent.putExtra( "User", recieverid );
       sendBroadcast( intent );

       */

    /*   Cursor ct=db.getReadableDatabase().query( Database.Recent.TBL_RECENT, null, Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? AND ", new String[]{recieverid, userid}
               , null, null, null );
       if (ct.getCount() < 1) {
           ct.close();
           Recent recent=new Recent();
           recent.setMsg( name);
           recent.setType( Database.Recent.TYPE_GROUP );
           recent.setLocalId( Recieverlocalid );
           recent.setMUserId( userid );
           recent.setOUserId( recieverid );
           recent.setName( Recievername );
           recent.setMsgId( newmsgid );
           recent.setRecentMode( userMode );
           recent.setMediaType(mediatype );
           recent.setTime( time );
           recent.setStatus( Database.Recent.STATUS_SEEN );
           OTextDb.getInstance( getApplicationContext() ).recentDao().insert( recent );

           ContentValues contentValues = new ContentValues();
           contentValues.put( Database.Recent.MSG,name );
           contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_GROUP );
           contentValues.put( Database.Recent.LOCAL_ID, Recieverlocalid );
           contentValues.put( Database.Recent.MUSER_ID, userid );
           contentValues.put( Database.Recent.OUSER_ID, recieverid );
           contentValues.put( Database.Recent.NAME, Recievername );
           contentValues.put( Database.Recent.MSG_ID, newmsgid );
           contentValues.put( Database.Recent.MEDIA_TYPE,mediatype );
           contentValues.put( Database.Recent.RECENT_MODE,userMode );
           contentValues.put( Database.Recent.TIME, time );
           contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_SEEN );
           db.getWritableDatabase().insert( Database.Recent.TBL_RECENT, null, contentValues );
           System.out.println("Recent Inserted in conversation");
       } else {

     */
           Recent recent=new Recent();
           recent.setMsg( name);
           recent.setType( Database.Recent.TYPE_USER );
           recent.setLocalId( Recieverlocalid );
           recent.setMUserId( userid );
           recent.setOUserId( recieverid );
           recent.setName( Recievername );
           recent.setMsgId( newmsgid );
           recent.setRecentMode( userMode );
           recent.setMediaType(mediatype );
           recent.setTime(  time.getTime()  );
           recent.setStatus( Database.Recent.STATUS_SEEN );
           OTextDb.getInstance( getApplicationContext() ).recentDao().insert( recent );


           /*ContentValues contentValues = new ContentValues();
           contentValues.put( Database.Recent.MSG, name );
           contentValues.put( Database.Recent.TYPE, Database.Recent.TYPE_USER );
           contentValues.put( Database.Recent.LOCAL_ID, Recieverlocalid );
           contentValues.put( Database.Recent.MUSER_ID, userid );
           contentValues.put( Database.Recent.OUSER_ID, recieverid );
           contentValues.put( Database.Recent.NAME, Recievername );
           contentValues.put( Database.Recent.MSG_ID, newmsgid );
           contentValues.put( Database.Recent.TIME, time );
           contentValues.put( Database.Recent.RECENT_MODE,userMode );
           contentValues.put( Database.Recent.MEDIA_TYPE, mediatype );
           contentValues.put( Database.Recent.STATUS, Database.Recent.STATUS_SEEN );
           int done = db.getWritableDatabase().update( Database.Recent.TBL_RECENT, contentValues,
                   Database.Recent.OUSER_ID + " = ? AND " + Database.Recent.MUSER_ID + " = ?"
                   , new String[]{recieverid, userid} );
           System.out.println("Recent Updated in conversation");

            */

   }
    private void sendImage(Uri data, String type,String path) {
        HashMap<String,Long> newcount=new HashMap<>(  );
        newcount.put("count",msg_ids_count+1);
        doc.set(newcount,SetOptions.merge());
        File directory=new File( getApplicationContext().getFilesDir(),path );
        if(!directory.exists()){
            directory.mkdirs();
        }
        String newmsgid=userid+"to"+recieverid+msg_ids_count;
        String prefix=type.equals( "gif" )?"GIF":"IMG";
        File file=new File( getFilesDir(),path+prefix+newmsgid+"."+type );
        System.out.println("dir path :"+ getFilesDir().getAbsolutePath() );
        addMsgData( file,newmsgid, Database.Msg.MEDIA_IMG,"You sent a picture ");
        new ImageSender( userid, recieverid, data.toString(),new Timestamp( new Date().getTime() ).toString(),
                newmsgid, getApplicationContext(),type ).start();




    }

    public void viewImage(View view) {
        Intent intent=new Intent( getApplicationContext(),ImageActivity.class );
        intent.setData( imguri );
        startActivity( intent );
    }

   /* public class BroadCastConversation extends BroadcastReceiver {

        private String recieverid;
        private String userid;
        private RecyclerView recyclerView;
        private SQLiteDatabase db;

        public BroadCastConversation(String recieverid, String userid, RecyclerView recyclerView, SQLiteDatabase db) {
            this.recieverid = recieverid;
            this.userid = userid;
            this.recyclerView = recyclerView;
            this.db = db;
        }



        public void onReceive(Context context, Intent intent) {
            System.out.println("BroadCast Received");
            String name=intent.getStringExtra( "User" );
            if(name.equals(recieverid)){
                int index=intent.getIntExtra( "index",-1 );


                if(index!=-1){
                    String msgid=intent.getStringExtra( "msgid" );
                    Cursor cur = db.query( Database.Msg.TBL_MSG,
                            null,
                            Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? AND " +Database.Msg.MSG_ID + " = ? ",
                            new String[]{recieverid, userid,msgid},
                            null,
                            null, Database.Msg.TIME
                    );
                    cur.moveToFirst();
                    if(cur.getCount()>0){
                        MessageData msgdata= new MessageData( cur.getString( 0 ), cur.getString( 1 ), cur.getString( 2 ), cur.getString( 3 ), cur.getString( 4 ),
                                cur.getString( 5 ), cur.getString( 6 ), cur.getString( 7 ), cur.getString( 8 ), cur.getString( 9 ), cur.getString( 10 ) ) ;

                    adapter.add( index,msgdata );
                    adapter.notifyItemChanged( index );
                    }
                }else {
                    Cursor cur = db.query( Database.Msg.TBL_MSG,
                            null,
                            Database.Msg.OUSER_ID + " = ? AND " + Database.Msg.MUSER_ID + " = ? ", new String[]{recieverid, userid},
                            null,
                            null, Database.Msg.TIME
                    );
                    try {
                        int pre = list.size();
                        int count = cur.getCount();
                        if (count > pre) {
                            cur.moveToPosition( pre );

                            while (!cur.isAfterLast()) {
                                list.add( new MessageData( cur.getString( 0 ), cur.getString( 1 ), cur.getString( 2 ), cur.getString( 3 ), cur.getString( 4 ),
                                        cur.getString( 5 ), cur.getString( 6 ), cur.getString( 7 ), cur.getString( 8 ), cur.getString( 9 ), cur.getString( 10 ) ) );
                                cur.moveToNext();
                            }
                            int prein=(adapter.getMsgs().size()-2);
                            adapter.setMsgs( list );
                            adapter.notifyDataSetChanged();
                            int pos=((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                            Log.d( TAG, "onReceive: prein="+prein+" pos="+pos );
                            if(pos==prein)
                                recyclerView.scrollToPosition( list.size()-1 );
                            MediaPlayer player=MediaPlayer.create( context,R.raw.mt );
                            player.start();
                        } else {
                            list.clear();
                            cur.moveToFirst();

                            while (!cur.isAfterLast()) {
                                list.add( new MessageData( cur.getString( 0 ), cur.getString( 1 ), cur.getString( 2 ), cur.getString( 3 ), cur.getString( 4 ),
                                        cur.getString( 5 ), cur.getString( 6 ), cur.getString( 7 ), cur.getString( 8 ), cur.getString( 9 ), cur.getString( 10 ) ) );
                                cur.moveToNext();
                            }
                            Log.d( TAG, "onReceive: less" );

                        }
                    } catch (Exception ex) {

                    }
                }
             }else{
                System.out.println("Not matched");
            }
        }
    }

    */

     private class BlockBroad extends BroadcastReceiver{

         @Override
         public void onReceive(Context context, Intent intent) {
               Block cur=viewModel.getBlockDao().getBlock( userid,recieverid );
               if(cur!=null){
                   can=false;
                   byme=cur.getType().equals( Database.Block.BY_ME );
                   if(prepareMessage!=null){
                       prepareMessage.setBlocked( true );
                   }
               }else{
                   can=true;
                   if(prepareMessage!=null){
                       prepareMessage.setBlocked( false );
                   }
               }
         }
     }
     class loadMessageData extends Thread{
        private  Handler handler;
        private List<Message> messageList;
        public loadMessageData(Handler handler,List<Message> list){
            this.handler=handler;
            this.messageList=list;

        }

         @Override
         public void run() {
            /* Cursor cu=db.getReadableDatabase().query( Database.Msg.TBL_MSG,
                     null,
                     Database.Msg.OUSER_ID+" = ? AND "+Database.Msg.MUSER_ID+" = ? ",new String[]{recieverid,userid},
                     null,
                     null, Database.Msg.TIME
             );
             list=new ArrayList<>(  );*/
             if(messageList==null) {
                 LiveData<List<Message>> listLiveData = viewModel.getMessageDao().getAll( userid, recieverid, userMode );
                 messageList=listLiveData.getValue();
                 handler.post( ()->{
                 listLiveData.observe( ConversationActivity.this, messages -> {
                     Log.d( TAG, "run: " +messages);
                     adapter.setMsgs( messages );
                     int prein=(adapter.getMsgs().size()-2);
                     int pos=((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition();

                     handler.post( ()->{
                         adapter.notifyDataSetChanged();
                         if(pos==prein)
                             recyclerView.scrollToPosition( list.size()-1 );
                     } );
                 } );
                 } );
             }
             handler.post( ()->{
                 adapter=new ConversationActivityAdapter(messageList,getApplicationContext(),userid,option );
                 recyclerView.setAdapter(adapter );
             } );
         }
     }

    @Override
    public void onBackPressed() {
        Toast.makeText( getApplicationContext(),"BackPressed",Toast.LENGTH_SHORT ).show();
        super.onBackPressed();
    }
}
