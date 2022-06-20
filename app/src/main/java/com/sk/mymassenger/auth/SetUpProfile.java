package com.sk.mymassenger.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.sk.mymassenger.R;
import com.sk.mymassenger.chat.fetchers.InfoTracker;
import com.sk.mymassenger.chat.fetchers.MessagesFatcher;
import com.sk.mymassenger.data.ServerUserData;
import com.sk.mymassenger.databinding.ActivitySetupBinding;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.dialog.SetUpProfileDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class SetUpProfile  extends AppCompatActivity {
    private static final String TAG = "SetUpProfile";
    private static final int RC_GET_PROFILE = 4;

    private final FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
    byte[] image=null;
    private String Email;
    private String Phone;
    private String UserId;
    private File file;
    private ActivitySetupBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        binding=ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView( binding.getRoot() );


        binding.uploadProfileBtn.setOnClickListener((v)->{
            Intent intent=new Intent( Intent.ACTION_GET_CONTENT );
            intent.setType( "image/*" );
            startActivityForResult( intent,RC_GET_PROFILE );
        });
        binding.buttonContinue.setOnClickListener( (cv)->{
            SetUpProfileDialog alertDialog=new SetUpProfileDialog(SetUpProfile.this);
            alertDialog.create();
            alertDialog.show();
            binding.buttonContinue.setEnabled( false);
            upLoadProfile(alertDialog );
        } );
    }

    private void upLoadProfile(SetUpProfileDialog alertDialog) {
        if(image==null){
            alertDialog.cancel();
            buildDialog( "Choose a profile picture to continue" ).show();
            binding.buttonContinue.setEnabled( true );
            return;
        }
        if(binding.editName.getText().toString().length()<2){
            alertDialog.cancel();
            buildDialog( "Enter a valid name" ).show();
            binding.buttonContinue.setEnabled( true );
            return;
        }
        FirebaseStorage.getInstance().getReference( "profiles/"+user.getUid()+".png" ).putBytes( image ).addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                saveUserData("profiles/"+user.getUid()+".png",alertDialog);
            }
        } );
    }
    private AlertDialog buildDialog(String msg){
        return new AlertDialog.Builder( new ContextThemeWrapper(SetUpProfile.this,R.style.AppTheme  ) )
                .setNegativeButton( "Cancle" ,null)
                .setCancelable( true ).setMessage( msg)
                .create();
    }
    private void saveUserData(String imageurl, SetUpProfileDialog alertDialog) {

        ServerUserData details=new ServerUserData();
        assert user != null;
        String Name=binding.editName.getText().toString();
        if(Name.length()<2||imageurl==null||image==null){
            binding.buttonContinue.setEnabled( true );
            binding.buttonContinue.setText( "Continue" );
            Toast.makeText( getApplicationContext(),"Check details and try again",Toast.LENGTH_SHORT ).show();
            return;
        }
        Email=user.getEmail()!=null?user.getEmail():"";
        Phone=user.getPhoneNumber()!=null?user.getPhoneNumber():"";
        UserId=user.getUid();
        details.setUserName(Name);

        //details.put( Database.Server.USER_NAME,Name);
        details.setEmail(Email);
        //details.put( Database.Server.EMAIL,Email);
        details.setPhone(Phone);
        //details.put( Database.Server.PHONE,Phone);
        details.setStatusCount(0);
        //details.put("statusCount",0);
        details.setUserId(UserId);
        details.setProfile(UserId+".png");
        //details.put( Database.Server.USER_ID,UserId);
        ServerUserData.Mood moodMap=details.getMood();
        moodMap.setValue("😀");
        //moodMap.put("value","😀");
        long time=new Date().getTime();
        moodMap.setSince(time);
        //moodMap.put("since",new Date().getTime());
        details.setFreeTime("no time");
        //details.put("freeTime","no time");
        ServerUserData.Changed changed=details.getChanged();
        changed.setLastUpdated(time);
        changed.setProfile(time);
        changed.setStatus(time);

        UserProfileChangeRequest profileChangeRequest=new UserProfileChangeRequest.Builder().setDisplayName( Name ).setPhotoUri( Uri.parse( imageurl ) ).build();
        user.updateProfile( profileChangeRequest ).addOnSuccessListener( new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                saveUserLocali(Name,details,alertDialog);
            }
        } );

    }

    private void saveUserLocali(String Name, ServerUserData details, SetUpProfileDialog alertDialog) {

       FirebaseFirestore.getInstance().collection( "users/" ).document(user.getUid()).set(details).addOnSuccessListener( new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                new Thread(()-> {
                    User userdata = new User();
                    userdata.setUserId(UserId);
                    userdata.setUserName(Name);
                    userdata.setPhoneNo(Phone);
                    userdata.setEmail(Email);
                    userdata.setLastUpdated(details.getChanged().getLastUpdated());
                    userdata.setMoodSince(details.getChanged().getLastUpdated());

                    userdata.setProfilePicture(file.getAbsolutePath());
                    userdata.setType(Database.User.TYPE_SELF);
                    userdata.setUserMode(Database.User.MODE_PUBLIC);
                    if (OTextDb.getInstance(getApplicationContext()).userDao().getUser(UserId) == null) {
                        //db.insert( Database.User.TBL_USER,contentValues );

                        OTextDb.getInstance(getApplicationContext()).userDao().insertAll(userdata);
                        Log.d(TAG, "onSuccess: Added to local db");
                    } else {

                        Log.d(TAG, "onSuccess: User updated");
                        OTextDb.getInstance(getApplicationContext()).userDao().update(userdata);
                    }
                    System.out.println("Splash userid : " + UserId);

                    Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MessagesFatcher.class).setConstraints(constraints)
                            .addTag("MessageServiceO")
                            .setInputData(
                                    new Data.Builder().putString("UserId", UserId).putString("Username", Name)
                                            .putBoolean("get", true).build()
                            ).build();
                    WorkManager.getInstance(getApplicationContext()).enqueue(work);

                    OneTimeWorkRequest workinfo = new OneTimeWorkRequest.Builder(InfoTracker.class).setConstraints(constraints)
                            .addTag("MessageInfoTrackerO")
                            .setInputData(
                                    new Data.Builder().putString("UserId", UserId)
                                            .putBoolean("get", true).build()
                            ).build();
                    WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("MessageInfoTrackerO", ExistingWorkPolicy.KEEP, workinfo);
                    alertDialog.cancel();
                    setResult(RESULT_OK);
                    finish();
                }).start();
            }
        } ).addOnFailureListener( e -> {
            e.printStackTrace();
            Toast.makeText( getApplicationContext(),"Some error occured try again later",Toast.LENGTH_SHORT ).show();
            alertDialog.cancel();
            finish();
        } );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==RC_GET_PROFILE){
            if(resultCode==RESULT_OK){
               Uri profile_uri=data.getData();


               file=new File( getApplicationContext().getFilesDir(),"PFL"+user.getUid()+".png" );
                Glide.with( getApplicationContext() ).load( profile_uri ).centerCrop().into( binding.uploadProfile );
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        Bitmap profile_bitmap=ImageDecoder.decodeBitmap( ImageDecoder.createSource( getContentResolver(),profile_uri ) );
                        ByteArrayOutputStream out=new ByteArrayOutputStream(  );
                       // int h= (int) (profile_bitmap.getHeight()*(512.0/profile_bitmap.getWidth()));
                       // profile_bitmap= Bitmap.createScaledBitmap( profile_bitmap,512,h,true );
                        profile_bitmap.compress( Bitmap.CompressFormat.PNG, 100, out );
                        image=out.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        Bitmap profile_bitmap=MediaStore.Images.Media.getBitmap( getContentResolver(),profile_uri );
                        ByteArrayOutputStream out=new ByteArrayOutputStream(  );
                      //  int h= (int) (profile_bitmap.getHeight()*(512.0/profile_bitmap.getWidth()));
                     //   profile_bitmap= Bitmap.createScaledBitmap( profile_bitmap,512,h,true );
                        profile_bitmap.compress( Bitmap.CompressFormat.PNG, 100, out );
                        image=out.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();


                    }
                }
                try {
                    FileOutputStream out=new FileOutputStream(  file );
                    out.write( image );
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }else{
               //to be implemented when result is not success
            }
        }
    }
}
