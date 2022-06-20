package com.sk.mymassenger;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.user.User;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Chat_Setting extends AppCompatActivity {

    private static final int RC_SET_PASS = 2;
    private static final int RC_REC_PROFILE_CHANGE = 3;
    private String receiverid;
    private ImageView profileImage;
    private TextView name;
    private TextView contactType;
    private TextView contactInfo;
    String selectedReport="";
    private  String muser= FirebaseAuth.getInstance().getCurrentUser().getUid();
    private Uri imageuri;
    private boolean blockbyme;
    private TextView textBlock;
    private ArrayList<String> results;
    private String userMode;
    private Button btnMakePrivate;
    private SQLiteDatabase writable;
    private  String userid=FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String password;
    private oTextDb db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        results=new ArrayList<>();
        setContentView( R.layout.activity_chat__setting );
        receiverid=getIntent().getStringExtra( "Reciever" );
      db=new oTextDb( getApplicationContext(),Database.O_TEXT_DB,null,Database.DB_VERSION );
        writable=db.getWritableDatabase();
       /* Cursor cursor=db.getReadableDatabase()
                .query( Database.User.TBL_USER,null, Database.User.USER_ID+" = ? ",new String[]{receiverid},null,null,null );

        */
        User user= OTextDb.getInstance( getApplicationContext() ).userDao().getUser( receiverid );
        profileImage=findViewById( R.id.receiver_image );

        userMode=user.getUserMode();
        String profilepath=user.getProfilePicture();
        if(profilepath!=null) {
            File profilefile = new File( getFilesDir(), profilepath.substring( profilepath.lastIndexOf( "/" ) ) );
            imageuri=FileProvider.getUriForFile( getApplicationContext(),
                    "com.sk.mymassenger.fileProvider", profilefile );
            new Handler( getMainLooper() ).post( () -> Glide.with( getApplicationContext() ).load( imageuri ).into( profileImage ) );
        }
        textBlock=findViewById( R.id.block_btn );

        name=findViewById( R.id.rec_name );
        btnMakePrivate=findViewById( R.id.make_private_btn );
        if(userMode.equals( Database.User.MODE_PRIVATE ))
            btnMakePrivate.setText( "Make Conversation Public" );
        name.setText( user.getUserName());
        findViewById( R.id.delete_btn ).setOnClickListener( (v)->{
            WorkRequest work=new OneTimeWorkRequest.Builder(MessageDeleter.class).setInputData(
                    new Data.Builder().putString( Database.Msg.MUSER_ID,muser ).putString( Database.Msg.OUSER_ID,receiverid )
                            .putStringArray( "ids",  new String[]{} ).putString( "mode",Database.Recent.TBL_RECENT ).build() ).build() ;
            WorkManager.getInstance(getApplicationContext()).enqueue( work );
            Intent intent=new Intent( );
            results.add( "Delete" );
            intent.putStringArrayListExtra( "Results",results);
            setResult( RESULT_OK,intent );
            finish();

        } );
        findViewById( R.id.customize_rec_btn ).setOnClickListener( (v)->{
            Intent intent=new Intent( Chat_Setting.this,SettingsActivity.class );
            intent.putExtra( "SettingMode", "customize");
            intent.putExtra( "userId",receiverid );
            startActivityForResult( intent,RC_REC_PROFILE_CHANGE );
        } );
     //   Cursor cur=db.getReadableDatabase().query( Database.Block.TBL_BLOCK,null,
           //     Database.Block.MUSER_ID+" = ? AND "+ Database.Block.OUSER_ID+" = ? ",
            //    new String[]{muser,receiverid},null,null,null );
        Block cur=OTextDb.getInstance( getApplicationContext() ).blockDao().getBlock( muser,receiverid );
        if(cur!=null){

            String type=cur.getType();
            if(type.equals( Database.Block.BY_ME )){
                blockbyme=true;
                textBlock.setText( "Unblock User" );

            }
            else{
                blockbyme=false;

            }

        }

        contactType=findViewById( R.id.in_contact_by );
        contactInfo=findViewById( R.id.info_contact_by );
        String contact=user.getPhoneNo();
        if(contact!=null&&contact.length()>3){
            contactType.setText( "Phone" );
            contactInfo.setText( contact );
        }else{
            contactType.setText( "Email" );
            contactInfo.setText(user.getEmail());
        }
    }

    public void viewImage(View view) {
        Intent intent=new Intent( getApplicationContext(),ImageActivity.class );
        intent.setData( imageuri );
        startActivity( intent );
    }

    public void blockUser(View view) {
        results.add( "block" );
        if (!blockbyme) {
          performBlock(true );
        }else{
           /* oTextDb db = new oTextDb( getApplicationContext(), Database.O_TEXT_DB, null, Database.DB_VERSION );
             db.getWritableDatabase().delete( Database.Block.TBL_BLOCK,
                    Database.Block.MUSER_ID + " = ? AND " + Database.Block.OUSER_ID + " = ? ",
                    new String[]{muser, receiverid} );*/
            OTextDb.getInstance( getApplicationContext() ).blockDao().delete( muser,userid, Database.Block.BY_ME );
             blockbyme=false;
            textBlock.setText( "Block User" );
            performUnBlock(false );
        }
    }

    private void performUnBlock(boolean b) {
        HashMap<String, Object> blockValue = new HashMap<>();
        HashMap<String,Object> details=new HashMap<>(  );
        details.put( Database.Block.BY_ME,FieldValue.delete() );
        blockValue.put( receiverid,details);
        FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + muser ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
            blockValue.clear();
            details.clear();
            details.put( Database.Block.BY_OTHER,FieldValue.delete() );
            blockValue.put( userid,details);
            FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + receiverid ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid1 -> {

                    oTextDb db = new oTextDb( getApplicationContext(), Database.O_TEXT_DB, null, Database.DB_VERSION );
                    Cursor cursor = db.getReadableDatabase().query( Database.Block.TBL_BLOCK, null,
                            Database.Block.MUSER_ID + " = ? AND " + Database.Block.OUSER_ID + " = ? ",
                            new String[]{muser, receiverid}, null, null, null );
                    if (cursor.getCount() >= 1) {
                        db.getWritableDatabase().delete( Database.Block.TBL_BLOCK, Database.Block.OUSER_ID+" = ? AND "+
                                Database.Block.MUSER_ID+" = ? AND "+ Database.Block.TYPE+" = ? ",new String[]{receiverid,muser, Database.Block.BY_ME}  );
                    }
                    cursor.close();
                    blockbyme =false;
                    textBlock.setText( "Unblock User" );

            } ).addOnFailureListener( e -> {

            } );
        } ).addOnFailureListener( e -> {

        } );
    }

    void  performBlock(boolean should){
        HashMap<String, Object> blockValue = new HashMap<>();
        HashMap<String,Object> details=new HashMap<>(  );
        details.put( Database.Block.BY_ME,true );
        blockValue.put( receiverid,details);
        FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + muser ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid -> {
            blockValue.clear();
            details.clear();
            details.put( Database.Block.BY_OTHER,true );
            blockValue.put( userid,details);
            FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + receiverid ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid1 -> {
               if(should) {
                  /* oTextDb db = new oTextDb( getApplicationContext(), Database.O_TEXT_DB, null, Database.DB_VERSION );
                   Cursor cursor = db.getReadableDatabase().query( Database.Block.TBL_BLOCK, null,
                           Database.Block.MUSER_ID + " = ? AND " + Database.Block.OUSER_ID + " = ? ",
                           new String[]{muser, receiverid}, null, null, null );*/
                   Block user=OTextDb.getInstance( getApplicationContext() ).blockDao().getBlock( muser,receiverid );
                   if (user==null) {
                       ContentValues values = new ContentValues();
                       values.put( Database.Block.MUSER_ID, muser );
                       values.put( Database.Block.OUSER_ID, receiverid );
                       values.put( Database.Block.TYPE, Database.Block.BY_ME );
                       db.getWritableDatabase().insert( Database.Block.TBL_BLOCK, null, values );
                   }
                   blockbyme = true;
                   textBlock.setText( "Unblock User" );
               }
            } ).addOnFailureListener( e -> {

            } );
        } ).addOnFailureListener( e -> {

        } );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==RC_SET_PASS){
            if(resultCode==RESULT_OK){
               new AlertDialog.Builder( new ContextThemeWrapper( Chat_Setting.this,R.style.AppTheme ) )
                        .setCancelable( true)
                        .setMessage( "To see private conversation go to > Profile > long press on oText icon" )
                       .setView( R.layout.path_private ).create().show();

                addMode( Database.User.MODE_PRIVATE );
            }
        }else if(requestCode==RC_REC_PROFILE_CHANGE){
            if(resultCode==RESULT_OK){
                 results.add("pf_change");
                 review();
            }
        }
    }

    private void review() {
       /* Cursor cursor=db.getReadableDatabase()
                .query( Database.User.TBL_USER,null, Database.User.USER_ID+" = ? ",new String[]{receiverid},null,null,null );

        */
        User user=OTextDb.getInstance( getApplicationContext() ).userDao().getUser( receiverid );
        profileImage=findViewById( R.id.receiver_image );
       String profilepath=user.getProfilePicture();
        if(profilepath!=null) {
            File profilefile = new File( getFilesDir(), profilepath.substring( profilepath.lastIndexOf( "/" ) ) );
            imageuri=FileProvider.getUriForFile( getApplicationContext(),
                    "com.sk.mymassenger.fileProvider", profilefile );
            new Handler( getMainLooper() ).post( () -> Glide.with( getApplicationContext() ).load( imageuri ).into( profileImage ) );
        }
        textBlock=findViewById( R.id.block_btn );
        name.setText(user.getUserName());
        String contact=user.getPhoneNo();
        if(contact!=null&&contact.length()>3){
            contactType.setText( "Phone" );
            contactInfo.setText( contact );
        }else{
            contactType.setText( "Email" );
            contactInfo.setText(user.getEmail());
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(  );
        intent.putStringArrayListExtra( "Results",results);
        setResult( RESULT_OK,intent );
        super.onBackPressed();
    }

    public void changeMode(View view) {

        String val="";
        if(userMode.equals( Database.User.MODE_PUBLIC )) {
            val = Database.User.MODE_PRIVATE;
            if(results.contains( Database.User.MODE_PUBLIC ))
                results.remove(  Database.User.MODE_PUBLIC );
            results.add( Database.User.MODE_PRIVATE);
            userMode=Database.User.MODE_PRIVATE;
            btnMakePrivate.setText( "Make Conversation Public" );
            SharedPreferences preferences=getSharedPreferences( "Password",MODE_PRIVATE );
            if(preferences!=null){
                password=preferences.getString( "private",null );
                if(password==null){
                    Intent intent=new Intent(Chat_Setting.this,PrivateActivity.class );
                    intent.putExtra( "Work","SetPass" );
                    startActivityForResult(intent,RC_SET_PASS  );
                }
            }
        }else{
            val = Database.User.MODE_PUBLIC;
            if(results.contains( Database.User.MODE_PRIVATE ))
                results.remove(  Database.User.MODE_PRIVATE );
            results.add( Database.User.MODE_PUBLIC);
            userMode= Database.User.MODE_PUBLIC;
            btnMakePrivate.setText( "Make Conversation Private" );
        }
        addMode( val );

    }
    public  void addMode(String val){
        ContentValues contentValues=new ContentValues(  );
        contentValues.put( Database.User.USER_MODE,val );
        try {
            writable.update( Database.User.TBL_USER, contentValues, Database.User.USER_ID + " = ? ", new String[]{receiverid} );
            contentValues.clear();
            System.out.println( "TBL_USER CHANGED TO : "+val );
            contentValues.put( Database.Recent.RECENT_MODE,val );
            writable.update( Database.Recent.TBL_RECENT, contentValues, Database.Recent.OUSER_ID + " = ? AND " + Database.Recent.MUSER_ID + " = ? ",
                    new String[]{receiverid, userid} );
            System.out.println( "TBL_RECENT CHANGED TO : "+val );
        }catch (SQLException ex){
            ex.printStackTrace();
        }
    }

    public void sendReport(View view) {

        CharSequence[] cs= new CharSequence[]{"Spam", "Scam and fraud", "absusive"};
        AlertDialog alertDialog=new AlertDialog.Builder( new ContextThemeWrapper( Chat_Setting.this,R.style.AppTheme ) )
                .setTitle( "Report User" )
                .setSingleChoiceItems(cs, 1, (dialogInterface, i) -> selectedReport= (String) cs[i] ).setPositiveButton( "send report", (dialogInterface, i) -> {
                    AlertDialog alertDialog1=new AlertDialog.Builder( new ContextThemeWrapper( Chat_Setting.this,R.style.AppTheme ) )
                            .setView( R.layout.spinner ).create();
                    alertDialog1.show();
                    HashMap<String,String> data=new HashMap<>(  );
                    data.put( userid,selectedReport );
                    FirebaseFirestore.getInstance().document( "reportUsers/"+receiverid ).set(data,SetOptions.merge()).addOnSuccessListener( aVoid -> alertDialog1.cancel() ).addOnFailureListener( e -> alertDialog1.cancel() );
                } ).create();
        alertDialog.show();

    }

    public void back(View view) {
        Toast.makeText( getApplicationContext(),"backed",Toast.LENGTH_SHORT ).show();
        super.onBackPressed();
    }
}






