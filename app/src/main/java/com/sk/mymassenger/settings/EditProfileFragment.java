package com.sk.mymassenger.settings;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.ImageActivity;
import com.sk.mymassenger.PrivateActivity;
import com.sk.mymassenger.R;
import com.sk.mymassenger.data.ServerUserData;
import com.sk.mymassenger.databinding.ProfileFragmentBinding;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.dialog.SetUpProfileDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public class EditProfileFragment extends Fragment {
    private static final String TAG = "Setting";
    private static final int GET_PROFILE = 0;
    private String cutUid;
    private ProfileFragmentBinding binding;
    private ServerUserData serverUserData;
    User userdata;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView( inflater, container, savedInstanceState );
        binding=ProfileFragmentBinding.inflate(inflater);
        FirebaseUser user=null;


        user= FirebaseAuth.getInstance().getCurrentUser();

        assert user != null;
        OTextDb.getInstance( getContext() ).userDao().getLiveUser( user.getUid() ).observe(getViewLifecycleOwner(), user1 -> {

            if(user1!=null){
                userdata=user1;
                binding.editUsername.setText(user1.getUserName() );
                String mood=user1.getMood();
                if(mood!=null&&mood.length()>0){
                    binding.mood.setText(mood);
                }
                String freeTime=user1.getFreeTime();
                if(freeTime!=null&&freeTime.length()>0){
                    binding.freeTime.setText(freeTime);
                }
                String profilepath=user1.getProfilePicture();
                if(profilepath!=null) {

                    File profilefile = new File( requireContext().getFilesDir(),
                            profilepath.substring( profilepath.lastIndexOf( "/" ) ) );
                    Uri uri= FileProvider.getUriForFile( requireContext(),
                            "co" +
                                    "m.sk.mymassenger.fileProvider", profilefile );
                    Glide.with( getContext() ).load( uri ).centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into( binding.userProfilePic );
                    binding.userProfilePic.setOnClickListener(view -> {
                        Intent intent=new Intent( requireContext(), ImageActivity.class );
                        intent.putExtra("userId",user1.getUserId());
                        intent.setData( uri );
                        requireActivity().startActivity( intent );
                    });
                }
            }
        });




        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get().addOnSuccessListener(snapshot -> serverUserData=snapshot.toObject(ServerUserData.class));
        binding.settingAppLogo.findViewById( R.id.setting_app_logo ).setOnLongClickListener((v)-> {
            startActivity(new Intent( requireContext(), PrivateActivity.class ));
            return true;
        } );


        binding.editProfile.setOnClickListener( (v)->{
            Intent intent=new Intent( Intent.ACTION_GET_CONTENT);
            intent.setType( "image/*" );
            startActivityForResult( intent ,GET_PROFILE);
        } );
        binding.editUsername.setOnEditorActionListener((textView, i, keyEvent) -> {
            serverUserData.setUserName(textView.getText().toString());
            FirebaseFirestore.getInstance().collection("users")
                    .document(userdata.getUserId()).set(serverUserData);
            userdata.setUserName(textView.getText().toString());
            new Thread(()-> OTextDb.getInstance(requireActivity().getApplicationContext()).userDao().insertAll(userdata)).start();

            return true;
        });
        binding.mood.setOnEditorActionListener((textView, i, keyEvent) -> {
            long time=new Date().getTime();
            serverUserData.getMood().setSince(time);
            serverUserData.getMood().setValue(textView.getText().toString());
            FirebaseFirestore.getInstance().collection("users")
                    .document(userdata.getUserId()).set(serverUserData)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(),"Success",
                                    Toast.LENGTH_SHORT).show());
            userdata.setMoodSince(time);
            userdata.setMood(textView.getText().toString());
            new Thread(()-> OTextDb.getInstance(requireActivity().getApplicationContext()).userDao().insertAll(userdata)).start();
            return true;
        });
        binding.freeTime.setOnEditorActionListener((textView, i, keyEvent) -> {
            serverUserData.setFreeTime(textView.getText().toString());
            FirebaseFirestore.getInstance().collection("users")
                    .document(userdata.getUserId()).set(serverUserData)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(),"Success",
                                    Toast.LENGTH_SHORT).show());
            userdata.setFreeTime(textView.getText().toString());
            new Thread(()-> OTextDb.getInstance(requireActivity().getApplicationContext()).userDao().insertAll(userdata)).start();
            return true;
        });

        return binding.getRoot();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==GET_PROFILE){
            if(resultCode==-1){
                assert data != null;
                Handler handler=new Handler(getActivity().getMainLooper());

                SetUpProfileDialog dialog=new SetUpProfileDialog(requireContext());
                new Thread(()->{

                    dialog.create();
                    handler.post(dialog::show);

                    dialog.setCancelable(false);


                    System.out.println("Image URI :- "+ Objects.requireNonNull(data.getData()).toString() );
                    File userFile=new File(userdata.getProfilePicture());
                    byte[] image = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            Bitmap profile_bitmap= ImageDecoder.decodeBitmap( ImageDecoder.createSource(
                                    requireContext().getContentResolver(),data.getData() ) );
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
                            Bitmap profile_bitmap= MediaStore.Images.Media.getBitmap(
                                    requireContext().getContentResolver(),data.getData() );
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
                        FileOutputStream out=new FileOutputStream( userFile );
                        out.write( image );
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FirebaseStorage.getInstance().getReference("profiles/"+
                            userdata.getUserId()+".png" ).putFile(data.getData())
                            .addOnSuccessListener(taskSnapshot -> {
                                Toast.makeText(getContext(),"Success",Toast.LENGTH_SHORT).show();

                                handler.post(dialog::cancel);
                            }).addOnFailureListener((task)-> handler.post(dialog::cancel));
                }).start();
                Glide.with(requireContext()).load( data.getData() )
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .centerCrop().into( binding.userProfilePic );
            }
        }
    }
}

