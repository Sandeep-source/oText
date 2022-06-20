package com.sk.mymassenger.chat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.database.ServerValue;
import com.sk.mymassenger.AppClosedBroad;
import com.sk.mymassenger.DeleteMessage;
import com.sk.mymassenger.ImageActivity;
import com.sk.mymassenger.MainActivity2;
import com.sk.mymassenger.OnlineUsers;
import com.sk.mymassenger.R;
import com.sk.mymassenger.Splash;
import com.sk.mymassenger.databinding.FragmentChatListBinding;
import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.db.recent.Recent;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.viewmodels.HomeViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ChatListFragment extends androidx.fragment.app.Fragment {
    private static final String TAG = "ChatListFragment";
       private FragmentChatListBinding binding;
       private HomeViewModel viewModel;
    private Handler handler;
    private String userMode;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel=new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X,true));


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

       userMode=requireActivity().getIntent().getStringExtra("userMode");

        binding=FragmentChatListBinding.inflate(inflater);

        viewModel.getUserDao().getLiveUser(viewModel.userId).observe(getViewLifecycleOwner(),(u)-> setProfile(u,handler));
        handler=new Handler(requireActivity().getMainLooper());
        new Thread(()-> setProfile(viewModel.getUser(),handler)).start();

        binding.conversationList.setLayoutManager( new LinearLayoutManager( null ) );

        System.out.println( "Adapter added" );
        binding.actionBtn.setOnLongClickListener(view -> {
            requireContext().startActivity( new Intent( requireContext(), MainActivity2.class ) );

            return true;
        });
        LiveData<List<Recent>> recent=viewModel.getRecentDao().getRecent(viewModel.userId,userMode );

            List<Recent> data=recent.getValue();

            Log.d(TAG, "onCreate: data size "+ (data==null?null:data.size()));
            setAdapter( data);
        binding.actionBtnNewChat.setOnClickListener((v)->{
            if(binding.actionBtn.getVisibility()==View.VISIBLE)
                animate();
            NavHostFragment.findNavController(ChatListFragment.this)
                    .navigate(R.id.action_chatListFragment_to_contactFragment);

        });
        recent.observe( getViewLifecycleOwner(), recents -> {

            Log.d(TAG, "onCreate: recent size "+(recents==null?null: recents.size()));
            setAdapter( recents);
        });
        binding.actionBtn.setOnClickListener( view ->  animate()  );
        binding.actionBtnSignOut.setOnClickListener(this::signOut);
        startPostponedEnterTransition();
        return binding.getRoot();
    }

    private void setAdapter(List<Recent> data) {
        if(data!=null&&data.size()>0){
            if(viewModel.adapter==null)
                viewModel.adapter=new ConversationAdapter(false,data.size(),data,viewModel.userId,binding.selectOpt);
            else{
                viewModel.adapter.setData( false,data.size(),data);
            }
        }

        else{
            if(viewModel.adapter==null)
                viewModel.adapter=new ConversationAdapter(true,1,null,viewModel.userId,binding.selectOpt);
            else{
                viewModel.adapter.setData( true,1,null );
            }
        }
        binding.conversationList.setAdapter(viewModel.adapter);
    }



    private HashMap<String,Object> markOfline(){
        HashMap<String,Object> hashMap=new HashMap<>( );
        hashMap.put( "status","offline" );
        hashMap.put( "last seen",ServerValue.TIMESTAMP );
        return hashMap;
    }

    public void signOut(View v){


        new AlertDialog.Builder(new ContextThemeWrapper( requireActivity(),R.style.AppTheme ) ).setTitle( "Sign out" )
                .setMessage( "Are you sure ? " )
                .setCancelable( true )
                .setNegativeButton( "cancel" ,null)
                .setPositiveButton( "Sign out",(d,i)->{
                    if(viewModel.ref!=null) {
                        viewModel.ref.setValue( markOfline() );
                    }
                    AuthUI.getInstance().signOut( requireContext() )
                            .addOnCompleteListener((task)-> {

                                WorkManager.getInstance( requireContext() ).cancelAllWork();
                                if (viewModel.listenInfo != null)
                                    viewModel.listenInfo.remove();
                                if (viewModel.listenMsg != null)
                                    viewModel.listenMsg.remove();
                                requireContext().startActivity(new Intent(requireContext(), Splash.class));
                                requireActivity().finish();
                            } ); } ).create().show();
        animate();
    }

    public void animate(){
        if(binding.actionBtnSignOut.getVisibility()==View.VISIBLE){
            binding.actionBtn.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.rotate));
            binding.actionBtnSignOut.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.close));
            binding.actionBtnNewChat.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.close));
            binding.actionBtnNewChat.setVisibility(View.GONE);

            binding.actionBtnSignOut.setVisibility(View.GONE);
            binding.actionBtn.setRotation(0);
        }else{
            binding.actionBtn.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.rotate));
            binding.actionBtnSignOut.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.open));
            binding.actionBtnNewChat.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.open));
            binding.actionBtnNewChat.setVisibility(View.VISIBLE);

            binding.actionBtnSignOut.setVisibility(View.VISIBLE);
            binding.actionBtn.setRotation(45);
        }
    }

    private void setProfile(User localUserData,Handler handler){
        String profilePath=localUserData.getProfilePicture();
        Log.d( TAG, "onCreate: user profile "+profilePath );
        if(profilePath!=null) {

            File profileFile = new File( requireContext().getFilesDir(), profilePath.substring( profilePath.lastIndexOf( "/" ) ) );
         handler.post(()-> Glide.with(requireContext()).load(FileProvider.getUriForFile(requireContext(),
                 "com.sk.mymassenger.fileProvider", profileFile))
                 .diskCacheStrategy(DiskCacheStrategy.NONE)
                 .skipMemoryCache(true)
                 .centerCrop().into(binding.mainProfile));
        }
        handler.post(()-> binding.mainProfile.setOnClickListener(view -> NavHostFragment.findNavController(ChatListFragment.this)
                .navigate(R.id.action_chatListFragment_to_profileSettingFragment)));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postponeEnterTransition();
        view.getViewTreeObserver().addOnPreDrawListener(() -> {
            startPostponedEnterTransition();
            return true;
        });
        String userMode=requireActivity().getIntent().getStringExtra("userMode");

        if(userMode.equals(Database.Recent.MODE_PRIVATE)){
            binding.actionBtnSignOut.setVisibility(View.GONE);
            binding.actionBtnNewChat.setVisibility(View.GONE);
            binding.actionBtn.setVisibility(View.GONE);
            binding.toolbar.setTitle("Private Mode");
            binding.toolbar.setBackgroundColor(Color.parseColor("#121212"));
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if(!userMode.equals(viewModel.mode)){

        }
        Intent intent=new Intent(requireContext(), AppClosedBroad.class);
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent=PendingIntent.getBroadcast( requireContext(),
                1,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager= (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel( pendingIntent  );

        viewModel.ref.onDisconnect().setValue( markOfline());
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.ref.setValue(markOfline());
        AlarmManager alarmManager= (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);

        Intent intent=new Intent(requireContext(),AppClosedBroad.class);
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent=PendingIntent.getBroadcast(requireContext(),
                1,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d( "service   ", "onStop: "+pendingIntent );
        alarmManager.setRepeating( AlarmManager.RTC, System.currentTimeMillis(),60000,pendingIntent );

    }

    public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.MViewHolder>{

        private boolean setEmpty;

        private boolean selectionMode;
        private ArrayList<String> selectedList;

        private long count;
        private List<Recent> data;
        private final String userid;
        private final View option;
        String muser="";
        String ouser="";
        public void setData(boolean b,long c,List<Recent> data){
            this.data=data;

            if(!b)
                ouser=data.get(0).getOUserId();
            count=c;
            setEmpty=b;
        }
        public ConversationAdapter(boolean set, long c, List<Recent> data, String uuserid, View option) {
            setEmpty=set;
            this.userid=uuserid;
            count=c;
            this.option=option;


            if(data!=null&&data.size()>0) {

                muser=data.get( 0 ).getMUserId();
                ouser=data.get(0).getOUserId();
            }
            this.data=data;
            binding.optDelete.setOnClickListener( (vi)->{
                Intent intent=new Intent(requireContext(), DeleteMessage.class);
                intent.putStringArrayListExtra( "ids",selectedList);
                intent.putExtra( "mode", Database.Recent.TBL_RECENT);
                intent.putExtra( "umode", Database.Recent.MODE_PUBLIC);
                System.out.println(userid+"  "+ouser);
                System.out.println("size  "+selectedList.size());
                intent.putExtra( Database.Msg.MUSER_ID,userid);
                intent.putExtra( Database.Msg.OUSER_ID,ouser);
                requireActivity().startService(intent);

                option.setVisibility( View.GONE );
            } );
        }

        @NonNull
        @Override
        public ConversationAdapter.MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if(setEmpty){
                View view= LayoutInflater.from(parent.getContext()).inflate( R.layout.empty_conversation,parent,false );
                return new ConversationAdapter.MViewHolder( view );
            }
            View view= LayoutInflater.from(parent.getContext()).inflate( R.layout.conversation_layout,parent,false );

            return new ConversationAdapter.MViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ConversationAdapter.MViewHolder holder, int position) {
            if(!setEmpty) {
                Recent recent=data.get(position);
                holder.getName().setText( recent.getName() );


                LiveData<User> user=viewModel.getUserDao().getLiveUser( recent.getOUserId() );
                Log.d(TAG, "onBindViewHolder: receiverId in recent "+recent.getOUserId());
                user.observe(getViewLifecycleOwner(), user1 -> {
                    String path=user1.getProfilePicture();
                    Log.d( TAG, "onBindViewHolder: " +user1+path);
                    if(path!=null&&path.length()>5) {
                        File imageFile = new File( requireContext().getFilesDir(), path.substring( path.lastIndexOf( "/" ) + 1 ) );

                        Uri imguri = FileProvider.getUriForFile( requireContext(), "com.sk.mymassenger.fileProvider", imageFile );
                        Glide.with( requireContext() ).load( imguri ).centerCrop().into( holder.getProfile() );
                        holder.getProfile().setOnClickListener( (v)->{

                            Log.d( "MainActivity ", "onbind : imageuri : "+imguri.toString() );
                            Intent intent=new Intent( requireContext(), ImageActivity.class );
                            intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION |Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            intent.putExtra("userId",data.get(position).getOUserId());
                            intent.setData( imguri );
                            requireContext().startActivity( intent );
                        } );
                    }
                });

                Log.d( "MViewHolder", "onBindViewHolder: "+recent.getMUserId());
                new OnlineUsers.ResolveDate().resolve( new Date(recent.getTime()),"recent" ,
                        holder.getDate(),new Handler(requireActivity().getMainLooper())) ;
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
                        String userId=recent.getOUserId();
                        Bundle intent=new Bundle();
                        intent.putString( "User", userId);
                        intent.putString( "MUser", userid );
                        NavHostFragment.findNavController(ChatListFragment.this)
                        .navigate(R.id.action_chatListFragment_to_chatFragment,intent);

                    }
                } );

            }else{

                holder.getStart_new().setOnClickListener( view -> {
                    NavHostFragment.findNavController(ChatListFragment.this)
                            .navigate(R.id.action_chatListFragment_to_contactFragment);
                    /*Intent intent=new Intent( requireContext(),ContactActivity.class );
                    intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                    requireContext().startActivity(intent);

                     */
                } );
            }
        }

        @Override
        public int getItemCount() {
            return (int) count;
        }
        class MViewHolder extends RecyclerView.ViewHolder{
            private TextView date;

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
