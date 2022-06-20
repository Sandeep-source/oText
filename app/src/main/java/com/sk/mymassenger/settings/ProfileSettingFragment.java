package com.sk.mymassenger.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.ImageActivity;
import com.sk.mymassenger.R;
import com.sk.mymassenger.Splash;
import com.sk.mymassenger.databinding.ProfileSettingBinding;
import com.sk.mymassenger.db.status.Status;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.viewmodels.ProfileSettingViewModel;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ProfileSettingFragment extends Fragment {
    private static final String TAG = "ProfileSettingFragment";
    private static final int RC_SIGN_IN = 0;
    private LiveData<User> liveUser;
    ProfileSettingBinding binding;
    private User user;
    ProfileSettingViewModel viewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
         viewModel=new  ViewModelProvider(this).get(ProfileSettingViewModel.class);

        binding=ProfileSettingBinding.inflate(getLayoutInflater());
        View view=binding.getRoot();


       viewModel.getUser().observe(getViewLifecycleOwner(),user1 -> {
           String path=user1.getProfilePicture();
           viewModel.setUser(user1);
           user=user1;
           binding.profileName.setText(user1.getUserName());
           if(path!=null&&path.length()>5) {


               File imageFile = new File( getContext().getFilesDir(), path.substring( path.lastIndexOf( "/" ) + 1 ) );
               Uri imguri = FileProvider.getUriForFile( getContext(), "com.sk.mymassenger.fileProvider", imageFile );
               Glide.with( getContext() ).load( imguri )
                       .diskCacheStrategy(DiskCacheStrategy.NONE)
                       .skipMemoryCache(true)
                       .centerCrop().into( binding.profile );

           }
       });


        binding.profileSetting.setOnClickListener((v)->{
            Bundle bundle=new Bundle(  );
            bundle.putString( "mode","extra" );
            bundle.putString( "userId","id");
            NavHostFragment.findNavController(this).navigate(R.id.action_profileSettingFragment_to_editProfile,bundle);
        });
        binding.extraSetting.setOnClickListener((view1 -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_profileSettingFragment_to_ExtraSettingsFragment);

        }));
        binding.theme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(ProfileSettingFragment.this)
                        .navigate(R.id.action_profileSettingFragment_to_themeFragment);
            }
        });
       binding.signOut.setOnClickListener((view1 -> {
           new AlertDialog.Builder(new ContextThemeWrapper( requireActivity(),R.style.AppTheme ) ).setTitle( "Sign out" )
                   .setMessage( "Are you sure ? " )
                   .setCancelable( true )
                   .setNegativeButton( "cancel" ,null)
                   .setPositiveButton( "Sign out",(d,i)->{

                       AuthUI.getInstance().signOut( requireContext() )
                               .addOnCompleteListener((task)-> {

                                   WorkManager.getInstance( requireContext()).cancelAllWork();
                                   requireContext().startActivity(new Intent(requireContext(), Splash.class));
                                   requireActivity().finish();

                               } ); } ).create().show();
       }));
        binding.status.setOnClickListener((view1 -> {
            Intent intent=new Intent(requireContext(), ImageActivity.class);
            intent.putExtra("userId",user.getUserId());
            requireContext().startActivity(intent);
        }));
        binding.deleteAccount.setOnClickListener(view12 -> new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("To delete account you need to sign in" +
                        " again to confirm your identity.\n\nAre you sure ?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    FirebaseFirestore.getInstance().collection("users").document(user.getUserId()).delete();
                    FirebaseFirestore.getInstance().collection("users").document("info"+user.getUserId()).delete();
                    FirebaseFirestore.getInstance().collection("users").document("to"+user.getUserId()).delete();

                    AuthUI.getInstance().signOut(requireContext()).addOnSuccessListener(aVoid -> {
                        List<AuthUI.IdpConfig> providers = Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.PhoneBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build() );

                        //start login activity
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setAvailableProviders( providers )
                                        .setAuthMethodPickerLayout(
                                                new AuthMethodPickerLayout.Builder(R.layout.login_method)
                                                        .setEmailButtonId(R.id.sign_email)
                                                        .setGoogleButtonId(R.id.sign_google)
                                                        .setPhoneButtonId(R.id.sign_phone)
                                                        .build())
                                        .build(),
                                RC_SIGN_IN );
                    });

                })
                .setNegativeButton("No",null)
                .create()
                .show());
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RC_SIGN_IN){
            if(resultCode== Activity.RESULT_OK){
                if(user.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                    FirebaseAuth.getInstance().getCurrentUser().delete().addOnSuccessListener(aVoid -> {
                        WorkManager.getInstance(requireContext()).cancelAllWork();
                        requireActivity().startActivity(new Intent(requireContext(), Splash.class));
                        requireActivity().finish();
                    });
                }else{
                    new AlertDialog.Builder(requireContext())
                            .setCancelable(false)
                            .setMessage("Can't proceed your request of account deletion.\n\n" +
                                    "it seams that you have logged in with a different account.\n" +
                                    "We are signing out from your account to due to security reason.\n" +
                                    "Sign in again to continue further.")
                            .setPositiveButton("Sign Out", (dialogInterface, i) -> AuthUI.getInstance().signOut(requireContext()).addOnSuccessListener(aVoid -> {
                                WorkManager.getInstance(requireContext()).cancelAllWork();
                                requireActivity().startActivity(new Intent(requireContext(), Splash.class));
                                requireActivity().finish();

                            }))
                            .create()
                           .show();
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.save();
    }

    @Override
    public void onResume() {

        super.onResume();


    }


}
