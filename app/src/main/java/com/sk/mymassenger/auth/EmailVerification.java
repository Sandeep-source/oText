  package com.sk.mymassenger.auth;

  import android.content.Intent;
  import android.os.Bundle;
  import android.os.CountDownTimer;
  import android.widget.TextView;

  import androidx.annotation.Nullable;
  import androidx.appcompat.app.AppCompatActivity;

  import com.firebase.ui.auth.AuthMethodPickerLayout;
  import com.firebase.ui.auth.AuthUI;
  import com.google.firebase.auth.FirebaseAuth;
  import com.google.firebase.auth.FirebaseUser;
  import com.sk.mymassenger.R;

  import java.util.Arrays;
  import java.util.List;

  public class EmailVerification extends AppCompatActivity {

      private static final int RC_SIGN_IN = 5;
      private FirebaseUser user;

      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate  ( savedInstanceState );
        setContentView( R.layout.activity_email_verification );
        user= FirebaseAuth.getInstance().getCurrentUser();
        user.sendEmailVerification().addOnCompleteListener( task -> {
            if(task.isSuccessful()) {
                new CountDownTimer(30000,100) {
                    @Override
                    public void onTick(long l) {
                        //((TextView) findViewById( R.id.timer )).setText(  );
                        //TODO implement count down timer to send verification mail again
                    }

                    @Override
                    public void onFinish() {

                    }
                };
                ((TextView) findViewById( R.id.verification_info )).setText( "Email has sent to " + user.getEmail() + ". Check email and Verify Account. After that click continue  countinue" );
            }
        } );
        findViewById( R.id.change_mail ).setOnClickListener((cmv)-> AuthUI.getInstance().signOut( getApplicationContext() ).addOnSuccessListener( aVoid -> {
            //build login method
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build() );

            //start signin activity
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders( providers )
                            .setTheme( R.style.AppTheme )
                            .setAuthMethodPickerLayout(
                                    new AuthMethodPickerLayout.Builder(R.layout.login_method)
                                            .setEmailButtonId(R.id.sign_email)
                                            .setGoogleButtonId(R.id.sign_google)
                                            .setPhoneButtonId(R.id.sign_phone)
                                            .build())
                            .build(),
                    RC_SIGN_IN );
        } ) );
        findViewById( R.id.done_verification ).setOnClickListener( (v)-> AuthUI.getInstance().signOut(getApplicationContext()).addOnSuccessListener( aVoid -> {
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build() );

            //start signin activity
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders( providers )
                            .setTheme( R.style.AppTheme )
                            .setAuthMethodPickerLayout(
                                    new AuthMethodPickerLayout.Builder(R.layout.login_method)
                                            .setEmailButtonId(R.id.sign_email)
                                            .setGoogleButtonId(R.id.sign_google)
                                            .setPhoneButtonId(R.id.sign_phone)
                                            .build())
                            .build(),
                    RC_SIGN_IN );
        } ) );

   }

      @Override
      protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
          super.onActivityResult( requestCode, resultCode, data );
          if(requestCode==RC_SIGN_IN){
              user=FirebaseAuth.getInstance().getCurrentUser();
              if(user.isEmailVerified()){
                  setResult( RESULT_OK );
                  finish();
              }else {
                  ((TextView) findViewById( R.id.verification_info )).setText( "Email has sent to " + user.getEmail() + ". Check email and Verify Account. After that click countinue" );

              }
          }
      }
  }