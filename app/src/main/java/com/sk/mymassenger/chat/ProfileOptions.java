package com.sk.mymassenger.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.bumptech.glide.Glide;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sk.mymassenger.DeleteMessage;
import com.sk.mymassenger.HomeActivity;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.ImageActivity;
import com.sk.mymassenger.PrivateActivity;
import com.sk.mymassenger.R;
import com.sk.mymassenger.databinding.ActivityChatSettingBinding;
import com.sk.mymassenger.db.OTextDb;
import com.sk.mymassenger.db.block.Block;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.viewmodels.HomeViewModel;
import com.sk.mymassenger.workers.MessageDeleter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class ProfileOptions extends Fragment {

    private static final int RC_SET_PASS = 2;
    private static final int RC_REC_PROFILE_CHANGE = 3;
    private String receiverid;


    String selectedReport="";
    private final String muser= FirebaseAuth.getInstance().getCurrentUser().getUid();
    private Uri imageuri;



    private ArrayList<String> results;
    private String userMode;

    private String password;

    Handler handler;

    ActivityChatSettingBinding binding;
    private User user;


    HomeViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Transition transition= TransitionInflater.from(requireContext()).inflateTransition(R.transition.move);

        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView( inflater,container,savedInstanceState );
        results=new ArrayList<>();
        binding=ActivityChatSettingBinding.inflate(getLayoutInflater());
        receiverid=getArguments().getString( "Reciever" );
       handler=new Handler(requireActivity().getMainLooper());
       viewModel=new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
       OTextDb.getInstance( requireContext() ).userDao().getLiveUser( receiverid )
               .observe(getViewLifecycleOwner(),(u)->{
           user=u;
                   userMode=user.getUserMode();
                   String profilepath=user.getProfilePicture();
                   if(profilepath!=null) {
                       File profilefile = new File( requireContext().getFilesDir(), profilepath.substring( profilepath.lastIndexOf( "/" ) ) );
                       imageuri=FileProvider.getUriForFile( requireContext(),
                               "com.sk.mymassenger.fileProvider", profilefile );
                      Glide.with( requireContext() ).load( imageuri ).into( binding.receiverImage ) ;
                   }

                if(userMode.equals( Database.User.MODE_PRIVATE ))
                    binding.makePrivateBtn.setText( "Make Conversation Public" );
                binding.recName.setText( user.getUserName());
                binding.deleteBtn.setOnClickListener( (v)->{
                    Intent intent=new Intent(requireContext(), DeleteMessage.class);
                    intent.putExtra("mode", Database.Recent.TBL_RECENT);
                    intent.putExtra("umode",userMode);
                    intent.putExtra(Database.Msg.OUSER_ID,receiverid);
                    intent.putExtra(Database.Msg.MUSER_ID,FirebaseAuth.getInstance().getCurrentUser().getUid());
                    intent.putExtra("ids",new ArrayList<>(Arrays.asList(receiverid)));
                    requireActivity().onBackPressed();
                    requireActivity().onBackPressed();
                    requireContext().startService(intent);

              });
                   binding.moodValue.setText(user.getMood());

                   binding.freeTimeValue.setText(user.getFreeTime());
                   String contact=user.getPhoneNo();
                   if(contact!=null&&contact.length()>3){
                       binding.inContactBy.setText( "Phone" );
                       binding.infoContactBy.setText( contact );
                   }else{
                       binding.inContactBy.setText( "Email" );
                       binding.infoContactBy.setText(user.getEmail());
                   }


       });
       binding.reportBtn.setOnClickListener(this::sendReport);
       binding.makePrivateBtn.setOnClickListener(this::changeMode);
       binding.blockBtn.setOnClickListener(this::blockUser);
       binding.receiverImage.setOnClickListener(this::viewImage);
        OTextDb.getInstance( requireContext() ).blockDao().getBlockLiveData( muser,receiverid )
                .observe(getViewLifecycleOwner(),(b)->{
                    if(b!=null){

                        String type=b.getType();
                        if(type.equals( Database.Block.BY_ME )){

                            viewModel.byme=true;
                            viewModel.can=false;
                            binding.blockBtn.setText( "Unblock User" );

                        }
                        else{
                           viewModel.byme=false;

                        }

                    }


                });


    return  binding.getRoot();
    }

    public void viewImage(View view) {
        Intent intent=new Intent( requireContext(), ImageActivity.class );
        intent.putExtra("userId",receiverid);
        intent.setData( imageuri );
        startActivity( intent );
    }

    public void blockUser(View view) {
        results.add( "block" );
        if (!viewModel.byme) {
          performBlock(true );
        }else{

            new Thread(()->{
                OTextDb.getInstance( requireContext() ).blockDao().delete( muser,receiverid, Database.Block.BY_ME );
                viewModel.byme=false;
                handler.post(()->
                binding.blockBtn.setText( "Block User" )
                );
                performUnBlock(false );
            }).start();

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
            blockValue.put( receiverid,details);
            FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + receiverid ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid1 -> new Thread(()-> {
                OTextDb.getInstance(requireContext()).blockDao().delete(muser, receiverid, Database.Block.BY_ME);
                viewModel.byme= false;
                handler.post(()->
                binding.blockBtn.setText("Block User")
                );
            }).start()).addOnFailureListener(e -> {

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
            blockValue.put( receiverid,details);
            FirebaseFirestore.getInstance().collection( "blockedUsers" ).document( "blockList" + receiverid ).set( blockValue, SetOptions.merge() ).addOnSuccessListener( aVoid1 -> {
               if(should) {

                   new Thread(()->{
                       Block block=new Block();
                       block.setMUserId(muser );
                       block.setOUserId(receiverid);
                       block.setType( Database.Block.BY_ME );
                       OTextDb.getInstance( requireContext() ).blockDao().insert( block );
                       viewModel.byme= true;
                       viewModel.can=false;
                       handler.post(()->
                       binding.blockBtn.setText( "Unblock User" )
                       );

                   }).start();

               }
            } ).addOnFailureListener( e -> {

            } );
        } ).addOnFailureListener( e -> {

        } );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode==RC_SET_PASS){
            if(resultCode==AppCompatActivity.RESULT_OK){
               new AlertDialog.Builder( new ContextThemeWrapper( getContext(), R.style.AppTheme ) )
                        .setCancelable( true)
                        .setMessage( "To see private conversation go to > Profile > long press on oText icon" )
                       .setView( R.layout.path_private ).create().show();


               new Thread(()-> OTextDb.getInstance(requireContext()).recentDao().updateMode(receiverid,
                       Database.User.TYPE_FRIEND + "  OF " + muser,
                       Database.Recent.MODE_PRIVATE)).start();
            }
        }else if(requestCode==RC_REC_PROFILE_CHANGE){
            if(resultCode==AppCompatActivity.RESULT_OK){
                 results.add("pf_change");
                 review();
            }
        }
    }

    private void review() {
       new Thread(()-> {
           User user = OTextDb.getInstance(requireContext()).userDao().getUser(receiverid);

           String profilepath = user.getProfilePicture();
           if (profilepath != null) {
               File profilefile = new File(requireContext().getFilesDir(), profilepath.substring(profilepath.lastIndexOf("/")));
               imageuri = FileProvider.getUriForFile(requireContext(),
                       "com.sk.mymassenger.fileProvider", profilefile);
               handler.post(() -> Glide.with(requireContext()).load(imageuri).into(binding.receiverImage));
           }

           handler.post(() ->
           binding.recName.setText(user.getUserName())
           );
           String contact = user.getPhoneNo();
           if (contact != null && contact.length() > 3) {
               handler.post(()->{
                   binding.inContactBy.setText("Phone");
                   binding.infoContactBy.setText(contact);
               });

           } else {
               handler.post(()->{
                   binding.inContactBy.setText("Email");
                   binding.infoContactBy.setText(user.getEmail());
               });
           }
       }).start();
    }



    public void changeMode(View view) {
        new Thread(()->{
            String val;
            if(userMode.equals( Database.User.MODE_PUBLIC )) {
                val = Database.User.MODE_PRIVATE;
                results.remove(  Database.User.MODE_PUBLIC );
                results.add( Database.User.MODE_PRIVATE);
                userMode=Database.User.MODE_PRIVATE;
                handler.post(()-> binding.makePrivateBtn.setText("Make Conversation Public"));
                SharedPreferences preferences=requireContext().getSharedPreferences( "Password", Context.MODE_PRIVATE );
                if(preferences!=null){
                    password=preferences.getString( "private",null );
                    if(password==null){
                        handler.post(()-> {
                            Intent intent = new Intent(requireContext(), PrivateActivity.class);
                            intent.putExtra("Work", "SetPass");
                            startActivityForResult(intent, RC_SET_PASS);
                        });
                    }
                }
            }else{
                val = Database.User.MODE_PUBLIC;
                results.remove(  Database.User.MODE_PRIVATE );
                results.add( Database.User.MODE_PUBLIC);
                userMode= Database.User.MODE_PUBLIC;
                handler.post(()-> binding.makePrivateBtn.setText("Make Conversation Private"));
            }

            Recent recent=OTextDb.getInstance(requireContext()).recentDao().getSingleRecent(FirebaseAuth.getInstance().getCurrentUser().getUid(),user.getUserId());
            recent.setRecentMode(val);
            OTextDb.getInstance(requireContext()).recentDao().insert(recent);
            user.setUserMode(val);
            OTextDb.getInstance(requireContext()).userDao().insertAll(user);

        }).start();


    }


    public void sendReport(View view) {

        CharSequence[] cs= new CharSequence[]{"Spam", "Scam and fraud", "absusive"};
        AlertDialog alertDialog=new AlertDialog.Builder( new ContextThemeWrapper( getContext(),R.style.AppTheme ) )
                .setTitle( "Report User" )
                .setSingleChoiceItems(cs, 1, (dialogInterface, i) -> selectedReport= (String) cs[i] ).setPositiveButton( "send report", (dialogInterface, i) -> {
                    AlertDialog alertDialog1=new AlertDialog.Builder( new ContextThemeWrapper( requireContext(),R.style.AppTheme ) )
                            .setView( R.layout.spinner ).create();
                    alertDialog1.show();
                    HashMap<String,String> data=new HashMap<>(  );
                    data.put( receiverid,selectedReport );
                    FirebaseFirestore.getInstance().document( "reportUsers/"+receiverid ).set(data,SetOptions.merge()).addOnSuccessListener( aVoid -> alertDialog1.cancel() ).addOnFailureListener( e -> alertDialog1.cancel() );
                } ).create();
        alertDialog.show();

    }


}






