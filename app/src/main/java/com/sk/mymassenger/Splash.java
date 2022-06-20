package com.sk.mymassenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.auth.EmailVerification;
import com.sk.mymassenger.auth.SetUpProfile;
import com.sk.mymassenger.auth.UserDataFetcher;
import com.sk.mymassenger.databinding.ActivitySplashBinding;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.status.StatusDeleteWorker;
import com.sk.mymassenger.db.status.StatusWorker;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.chat.senders.FailedMessageSender;
import com.sk.mymassenger.chat.fetchers.InfoTracker;

import java.util.Arrays;
import java.util.List;


public class Splash extends AppCompatActivity {
    private static final int RC_SIGN_IN =1;
    private static final int RC_LOCATION =2;
    private static final int RC_EMAIL_VERIFY = 3;
    private static final int RC_SETUP =4 ;
    private static final String TAG = "Splash Deb";
    ActivitySplashBinding binding;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        binding=ActivitySplashBinding.inflate(getLayoutInflater());
           setContentView( binding.getRoot());
           handler=new Handler(getMainLooper());
           try{
        AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) binding.imageView.getDrawable();
        drawable.start();
           }catch (Exception e){
               e.printStackTrace();
           }
        stopService( new Intent( getApplicationContext(),MessageService.class ));
               //check if user logged in or not
               FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

               if (user == null) {
                   //check for permission
                   if (ActivityCompat.checkSelfPermission( getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
                       startSignInActivity();
                   } else {

                       ActivityCompat.requestPermissions( Splash.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_CONTACTS}, RC_LOCATION );
                   }

               } else {
                   if(user.getEmail()!=null&&user.getEmail().length()>3){
                   if (user.isEmailVerified()) {
                    new Thread(()-> checkLocalUserOrfetch( user )).start();
                   } else {
                       startActivityForResult( new Intent( getApplicationContext(), EmailVerification.class ), RC_EMAIL_VERIFY );
                   }

                   } else {
                       new Thread(()-> checkLocalUserOrfetch( user )).start();
                   }
               }


    }

    private void startSignInActivity() {
        //build login method
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build() );

        int theme;
        try {
            theme=getPackageManager().getActivityInfo(getComponentName(),0)
                    .getThemeResource();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            theme=R.style.oText_Theme;
        }

        //start sign in activity
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders( providers )
                        .setTheme(theme)
                        .setAuthMethodPickerLayout(
                                new AuthMethodPickerLayout.Builder(R.layout.login_method)
                                        .setEmailButtonId(R.id.sign_email)
                                        .setGoogleButtonId(R.id.sign_google)
                                        .setPhoneButtonId(R.id.sign_phone)
                                        .build())
                        .build(),
                RC_SIGN_IN );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(FirebaseAuth.getInstance().getCurrentUser()!=null) {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(StatusWorker.class)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()).build();
            WorkManager.getInstance(getApplicationContext()).enqueue(request);
            request = new OneTimeWorkRequest.Builder(StatusDeleteWorker.class).build();
            WorkManager.getInstance(getApplicationContext()).enqueue(request);
        }
    }

    private void checkLocalUserOrfetch(FirebaseUser user){

        User userData=OTextDb.getInstance( getApplicationContext() ).userDao().getUser( user.getUid() );
        if (userData==null) {
            startActivityForResult( new Intent( Splash.this, SetUpProfile.class ), RC_SETUP );
        } else {
            Constraints constraints = new Constraints.Builder().setRequiredNetworkType( NetworkType.CONNECTED ).build();
            WorkRequest workRequest = new OneTimeWorkRequest.Builder( FailedMessageSender.class ).setInputData(
                    new Data.Builder().putString( "User", user.getUid() ).build() ).setConstraints( constraints ).build();
            WorkManager.getInstance( getApplicationContext() ).enqueue( workRequest );
            new Handler( getMainLooper() ).postDelayed( () -> {
                Intent intent= new Intent( Splash.this, HomeActivity.class );
                intent.putExtra("userMode", Database.Recent.MODE_PUBLIC);
                startActivity(intent );
                finish();
            }, 800 );
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        if(requestCode==RC_LOCATION){
            //check if permission granted
            if(grantResults.length>0){
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build() );

                //start login activity
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders( providers )
                                .setTheme( R.style.oText_Theme)
                                .setAuthMethodPickerLayout(
                                new AuthMethodPickerLayout.Builder(R.layout.login_method)
                                        .setEmailButtonId(R.id.sign_email)
                                        .setGoogleButtonId(R.id.sign_google)
                                        .setPhoneButtonId(R.id.sign_phone)
                                        .build())
                                .build(),
                        RC_SIGN_IN );
            }else{
                finish();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        //check if result for sign in
        if(requestCode==RC_SIGN_IN){

            if(resultCode==RESULT_OK){
                //check if sign in successful
                Toast.makeText( getApplicationContext(),"Signed In",Toast.LENGTH_SHORT ).show();
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(user.getEmail()!=null) {
                    if (user.isEmailVerified()) {
                       getUserData( user );

                    } else {
                        startActivityForResult( new Intent( getApplicationContext(), EmailVerification.class ), RC_EMAIL_VERIFY );
                    }
                }else{
                    getUserData( user );
                }

              }else{
                finish();
            }
        }else if(requestCode==RC_SETUP){
            if(resultCode==RESULT_OK) {
                new Thread(()-> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    User userData = OTextDb.getInstance(getApplicationContext()).userDao().getUser(user.getUid());
                    if (userData == null) {
                        handler.post(()->
                        Toast.makeText(getApplicationContext(), "Some error occurred ", Toast.LENGTH_SHORT).show());
                        AuthUI.getInstance().signOut(getApplicationContext()).addOnSuccessListener(aVoid -> finish());

                    } else {
                        handler.postDelayed(() -> {

                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                            intent.putExtra("userMode", Database.Recent.MODE_PUBLIC);
                            startActivity(intent);
                            finish();
                        }, 800);
                    }
                }).start();
            }else{
                signOutAndStartSignInActivity();
            }

        }else if(requestCode==RC_EMAIL_VERIFY){
            if(resultCode==RESULT_OK){
               FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
               if(user.getEmail()!=null) {
                   if (user.isEmailVerified()) {
                       getUserData( user );
                   } else {
                       Toast.makeText( getApplicationContext(), "Some error occurred ", Toast.LENGTH_SHORT ).show();
                       AuthUI.getInstance().signOut( getApplicationContext() ).addOnSuccessListener( aVoid -> finish() );
                       finish();
                   }
               }else{
                   getUserData( user );
               }
            }else{
                signOutAndStartSignInActivity();
            }
        }
    }

    private void signOutAndStartSignInActivity() {
        AuthUI.getInstance().signOut(getApplicationContext()).addOnSuccessListener(unused -> startSignInActivity());
    }

    private void getUserData(FirebaseUser user) {
        FirebaseFirestore.getInstance().collection( "users/" ).document(user.getUid()).get().addOnSuccessListener( documentSnapshot -> {
            if(documentSnapshot.exists()){
                new Thread(()-> {
                    User userData = OTextDb.getInstance(getApplicationContext()).userDao().getUser(user.getUid());
                    Log.d(TAG, "getUserData: " + userData);
                    if (userData == null) {
                        FirebaseFirestore.getInstance().collection("users").whereEqualTo(Database.Server.USER_ID, user.getUid()).get().addOnCompleteListener(task -> new Thread(()-> {
                            new UserDataFetcher(getApplicationContext(), false).getUserData(task);
                            Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                            OneTimeWorkRequest workinfo = new OneTimeWorkRequest.Builder(InfoTracker.class).setConstraints(constraints)
                                    .addTag("MessageInfoTrackerO")
                                    .setInputData(
                                            new Data.Builder().putString("UserId", user.getUid())
                                                    .putBoolean("get", true).build()
                                    ).build();
                            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("MessageInfoTrackerO", ExistingWorkPolicy.KEEP, workinfo);

                            new Handler(Splash.this.getMainLooper()).postDelayed(() -> {

                                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);

                                intent.putExtra("userMode", Database.Recent.MODE_PUBLIC);
                                startActivity(intent);
                                finish();
                            }, 800);
                        }).start());
                    } else {
                        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                        OneTimeWorkRequest workinfo = new OneTimeWorkRequest.Builder(InfoTracker.class).setConstraints(constraints)
                                .addTag("MessageInfoTrackerO")
                                .setInputData(
                                        new Data.Builder().putString("UserId", user.getUid())
                                                .putBoolean("get", true).build()
                                ).build();
                        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("MessageInfoTrackerO", ExistingWorkPolicy.KEEP, workinfo);

                        new Handler(getMainLooper()).postDelayed(() -> {

                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);

                            intent.putExtra("userMode", Database.Recent.MODE_PUBLIC);
                            startActivity(intent);
                            finish();
                        }, 800);
                    }
                }).start();
            }else{
                Intent intent=new Intent(Splash.this,SetUpProfile.class);
                startActivityForResult( intent , RC_SETUP);
            }
        } );

    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}