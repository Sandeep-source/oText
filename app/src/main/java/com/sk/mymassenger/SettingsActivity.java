package com.sk.mymassenger;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.db.user.UserDao;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.settings_activity );
        String settingMode=getIntent().getStringExtra( "SettingMode" );
        if(settingMode.equals( "ExtraSetting" )) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace( R.id.settings, new SettingsFragment() )
                    .commit();
        }else if(settingMode.equals( "customize" )) {
            Setting fragment=new  Setting();
            Bundle bundle=new Bundle(  );
            bundle.putString( "mode","customize" );
            bundle.putString( "userId",getIntent().getStringExtra( "userId" ));
            fragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace( R.id.settings,fragment )
                    .commit();
        }
        else{
            Setting fragment=new  Setting();
            Bundle bundle=new Bundle(  );
            bundle.putString( "mode","extra" );
            bundle.putString( "userId","id");
            fragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace( R.id.settings,fragment )
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled( true );
        }
    }

    public void profileBack(View view) {
        super.onBackPressed();
    }


    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener( this );

        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals( "DarkTheme" )){
                boolean enabled=sharedPreferences.getBoolean( key,false );
                if(enabled){
                    AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_YES );
                }else{
                    AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_NO );
                }

            }
        }
    }

    public static class Setting extends Fragment {
        private static final int GET_PROFILE = 0;
        private ImageView profile;
        private Bitmap profile_bitmap;
        private EditText editId_value;
        private String cutUid;
        private String type;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreateView( inflater, container, savedInstanceState );
            View view=inflater.inflate( R.layout.profile_fragment,container,false );
            FirebaseUser user=null;
            String mode=getArguments().getString( "mode" );
            User userdata= null;
            editId_value=view.findViewById( R.id.id_value );

            if(mode.equals( "customize" )){

               cutUid=getArguments().getString( "userId" );
               editId_value.setEnabled( true );
               /* userdata=new oTextDb( getContext(),Database.O_TEXT_DB,null,Database.DB_VERSION ).getReadableDatabase().
                        query( Database.User.TBL_USER,null, Database.User.USER_ID+" = ?",new String[]{cutUid},null,null,null );

                */
                userdata=OTextDb.getInstance( getContext() ).userDao().getUser( cutUid );
                view.findViewById( R.id.delete_account ).setVisibility( View.GONE );
            }else {
                user=FirebaseAuth.getInstance().getCurrentUser();
              /*  userdata=new oTextDb( getContext(),Database.O_TEXT_DB,null,Database.DB_VERSION ).getReadableDatabase().
                        query( Database.User.TBL_USER,null, Database.User.USER_ID+" = ?",new String[]{user.getUid()},null,null,null );


               */
                userdata=OTextDb.getInstance( getContext() ).userDao().getUser( user.getUid() );

                FirebaseUser finalUser1 = user;
                view.findViewById( R.id.delete_account ).setOnClickListener( (dv)-> finalUser1.delete().addOnSuccessListener(
                        new OnSuccessListener<Void>() {

                            @Override
                            public void onSuccess(Void aVoid) {
                                FirebaseFirestore.getInstance().document( "users/"+ finalUser1.getUid() ).delete();
                                startActivity( new Intent( getContext(),Splash.class ) );
                                getActivity().finish();
                            }
                        }
                ) );
            }
            profile=view.findViewById( R.id.user_profile_pic );
            EditText username=view.findViewById( R.id.edit_username );
            view.findViewById( R.id.setting_app_logo ).setOnLongClickListener((v)-> {
                startActivity(new Intent( getContext(),PrivateActivity.class ));
                return true;
            } );

            if(userdata!=null){
                username.setText(userdata.getUserName() );
                String val=userdata.getPhoneNo();
                type=val!=null&&val.length()>1?"Mobile":"Email";
                val=type.equals( "Mobile" )?val:userdata.getEmail();
                ((TextView)view.findViewById( R.id.id_type )).setText(type);

                        editId_value.setText(val);
                String profilepath=userdata.getProfilePicture();
                if(profilepath!=null) {
                    final int density=getResources().getDisplayMetrics().densityDpi;
                    int size=100*(density/160);
                    File profilefile = new File( getContext().getFilesDir(), profilepath.substring( profilepath.lastIndexOf( "/" ) ) );
                    Uri uri=FileProvider.getUriForFile( getContext(),
                            "co" +
                                    "m.sk.mymassenger.fileProvider", profilefile );
                    Glide.with( getContext() ).load( uri ).centerCrop().into( profile );
                    profile.setOnClickListener( new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent=new Intent( getActivity(),ImageActivity.class );
                            intent.setData( uri );
                            getActivity().startActivity( intent );
                        }
                    } );
                }
            }
            view.findViewById( R.id.edit_profile ).setOnClickListener( (v)->{
                Intent intent=new Intent( Intent.ACTION_GET_CONTENT);
                intent.setType( "image/*" );
                startActivityForResult( intent ,GET_PROFILE);
            } );
            FirebaseUser finalUser = user;
            view.findViewById( R.id.save_profile ).setOnClickListener( (v)->{
                if(mode.equals( "customize" )){
                     String id_column="";
                     if(type.equals( "Phone" )){
                         id_column= Database.User.PHONE_NO;
                     }else{
                         id_column= Database.User.EMAIL;
                     }
                     ContentValues values=new ContentValues(  );
                     String name=username.getText().toString();
                     if(name!=null&&name.length()>0)
                     values.put( Database.User.USER_NAME,name );
                     String fakeId=editId_value.getText().toString();
                     if(fakeId!=null&& fakeId.length()>0){
                         values.put( id_column,fakeId );
                     }
                     if(profile_bitmap!=null){
                         ByteArrayOutputStream out = new ByteArrayOutputStream();

                         int h = (int) (profile_bitmap.getHeight() * (512.0 / profile_bitmap.getWidth()));
                         profile_bitmap = Bitmap.createScaledBitmap( profile_bitmap, 512, h, true );
                         profile_bitmap.compress( Bitmap.CompressFormat.PNG, 100, out );
                         try {
                           FileOutputStream stream=  new FileOutputStream( new File( getContext().getFilesDir(), "PFL" + cutUid + ".png" ) );
                           stream.write( out.toByteArray() );
                           stream.close();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }
                     if(values.size()>0){
                        /* new oTextDb( getContext(), Database.O_TEXT_DB, null, Database.DB_VERSION ).getWritableDatabase()
                                 .update( Database.User.TBL_USER, values, Database.User.USER_ID + " = ? ", new String[]{cutUid} );


                         */
                         UserDao dao=OTextDb.getInstance( getContext() ).userDao();
                         User userD=dao.getUser( cutUid );
                         if(id_column.equals( "Phone" )){
                             userD.setPhoneNo( fakeId );
                         }else{
                             userD.setEmail( fakeId );
                         }
                         userD.setUserName( name );
                         dao.update( userD );
                         getActivity().setResult( RESULT_OK );
                     }
                }else {
                    AlertDialog alertDialog = new AlertDialog.Builder( new ContextThemeWrapper( getActivity(), R.style.AppTheme ) )

                            .setView( R.layout.spinner ).create();
                    alertDialog.show();
                    if (profile_bitmap != null) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        int h = (int) (profile_bitmap.getHeight() * (512.0 / profile_bitmap.getWidth()));
                        profile_bitmap = Bitmap.createScaledBitmap( profile_bitmap, 512, h, true );
                        profile_bitmap.compress( Bitmap.CompressFormat.PNG, 100, out );
                        FirebaseStorage.getInstance().getReference( "profiles/" + finalUser.getUid() + ".png" ).putBytes( out.toByteArray() ).addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                try {
                                    FileOutputStream stream=new FileOutputStream( new File( getContext().getFilesDir(), "PFL" + finalUser.getUid() + ".png" ) );
                                    stream.write( out.toByteArray() );
                                    stream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String name = username.getText().toString();
                                if (name.length() > 2) {
                                    UserProfileChangeRequest changeRequest = new UserProfileChangeRequest.Builder().setDisplayName( name ).build();
                                    finalUser.updateProfile( changeRequest ).addOnSuccessListener( new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            ContentValues values = new ContentValues();
                                            values.put( Database.User.USER_NAME, name );
                                            /*new oTextDb( getContext(), Database.O_TEXT_DB, null, Database.DB_VERSION ).getWritableDatabase()
                                                    .update( Database.User.TBL_USER, values, Database.User.USER_ID + " = ? ", new String[]{finalUser.getUid()} );



                                             */
                                            alertDialog.cancel();

                                        }
                                    } );
                                } else
                                    alertDialog.cancel();
                            }
                        } );
                    } else {
                        String name = username.getText().toString();
                        if (name.length() > 2) {
                            UserProfileChangeRequest changeRequest = new UserProfileChangeRequest.Builder().setDisplayName( name ).build();
                            finalUser.updateProfile( changeRequest ).addOnSuccessListener( new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    ContentValues values = new ContentValues();
                                    values.put( Database.User.USER_NAME, name );
                                    /*new oTextDb( getContext(), Database.O_TEXT_DB, null, Database.DB_VERSION ).getWritableDatabase()
                                            .update( Database.User.TBL_USER, values, Database.User.USER_ID + " = ? ", new String[]{finalUser.getUid()} );


                                     */
                                    alertDialog.cancel();
                                }
                            } );
                        }
                    }

                }
            } );


            return view;

        }
        private AlertDialog buildAlert(String msg){
            return new AlertDialog.Builder(new ContextThemeWrapper(getActivity(),R.style.AppTheme) )
                    .setTitle( "Update info" ).setMessage( msg ).setCancelable( true ).create();
        }
        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult( requestCode, resultCode, data );
            if(requestCode==GET_PROFILE){
                if(resultCode==-1){
                    System.out.println("Image URI :- "+ data.getData().toString() );
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            profile_bitmap= ImageDecoder.decodeBitmap( ImageDecoder.createSource( getActivity().getContentResolver(),data.getData() ) );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        try {
                            profile_bitmap= MediaStore.Images.Media.getBitmap( getActivity().getContentResolver(),data.getData() );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Glide.with( getContext() ).load( data.getData() ).centerCrop().into( profile );
                }
            }
        }
    }


}