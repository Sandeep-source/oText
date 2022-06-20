package com.sk.mymassenger;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.sk.mymassenger.databinding.ActivityImageBinding;
import com.sk.mymassenger.databinding.AudioFragmentBinding;
import com.sk.mymassenger.databinding.ImageFragmentBinding;
import com.sk.mymassenger.databinding.VideoFragmentBinding;
import com.sk.mymassenger.db.status.Status;
import com.sk.mymassenger.db.status.StatusDao;
import com.sk.mymassenger.db.status.StatusUploadWorker;
import com.sk.mymassenger.db.user.User;
import com.sk.mymassenger.dialog.StatusUploadDialog;
import com.sk.mymassenger.viewmodels.ImageActivityViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

//TODO problem in updating list after download of statuses
public class ImageActivity extends FragmentActivity {
    private static final String TAG = "ImageActivity";
    private static final int RC_GET_STATUS_MEDIA = 1;
    private static final int RC_GET_STATUS_AUDIO = 2;
    private static final int RC_GET_STATUS_VIDEO = 3;
    private ImageActivityViewModel viewModel;
    private User user;
    private ViewPager2 statusListView;
    private PagerAdapter adapter;
    private String userId;
    private final int pagerIndex=0;

    public ImageActivityViewModel getViewModel() {
        return viewModel;
    }

    public ActivityImageBinding getBinding() {
        return binding;
    }

    private ActivityImageBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        binding=ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Uri data=getIntent().getData();
        userId=getIntent().getStringExtra("userId");

        View view=getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        viewModel=ImageActivityViewModel.getInstance(this, userId);
        viewModel.getUserDao().getLiveUser(userId).observe(ImageActivity.this, userUpdate -> {
            user=userUpdate;
            if(userUpdate.getMood()!=null&&userUpdate.getMood().length()>0)
             binding.mood.setText(userUpdate.getMood());
        });
        statusListView=findViewById(R.id.statusList);
        if(!userId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
            binding.fabOpenMenu.setVisibility(View.GONE);
        }else{
            binding.fabOpenMenu.setOnClickListener((v)-> animate());
        }

        binding.audioStatus.setOnClickListener(view13 -> {
            animate();
            Intent intentAud = new Intent(Intent.ACTION_GET_CONTENT);
            intentAud.setType("audio/*");
            // intent.putExtra(Intent.EXTRA_MIME_TYPES,{"imag/*","vedio"});
            startActivityForResult(intentAud, RC_GET_STATUS_AUDIO);
        });

        binding.imageStatus.setOnClickListener(view12 -> {
            animate();
            Intent intentImg = new Intent(Intent.ACTION_GET_CONTENT);
            intentImg.setType("image/*");
            // intent.putExtra(Intent.EXTRA_MIME_TYPES,{"imag/*","vedio"});
            startActivityForResult(intentImg, RC_GET_STATUS_MEDIA);
        });
        binding.videoStatus.setOnClickListener(view1 -> {
            animate();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            startActivityForResult(intent, RC_GET_STATUS_VIDEO);
        });
        binding.deleteStatus.setOnClickListener((v)->{
           if(viewModel.currentStatus==null)
               return;
           new Thread(()-> {
               Status status = viewModel.getStatusDao().getStatusById(viewModel.currentStatus.getStatusId());
               if (status == null)
                   return;
               viewModel.getStatusDao().delete(viewModel.currentStatus.getStatusId());
               viewModel.getStatusDao().delete(status);
               new Handler(getMainLooper()).post(()->
               adapter.notifyItemRemoved(Objects.requireNonNull(viewModel.getStatuses().getValue()).indexOf(viewModel.currentStatus)));
               HashMap<String, Object> map = new HashMap<>();
               map.put(status.getStatusId(), FieldValue.delete());
               File file = new File(status.getStatusUri());
               if (file.exists()) {
                   file.delete();
               }
               FirebaseFirestore.getInstance().collection("statuses/").document(userId)
                       .set(map, SetOptions.mergeFields(status.getStatusId()));
               FirebaseStorage.getInstance().getReference("statuses/" + file.getName()).delete();
           }).start();

        });

        Log.d( "ImageActivity ", "onCreate: data : "+data );
    }
    public void animate(){
        if(binding.imageStatus.getVisibility()==View.VISIBLE){
            binding.fabOpenMenu.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate));
            binding.imageStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close));
            binding.videoStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close));
            binding.audioStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close));
            binding.imageStatus.setVisibility(View.GONE);
            binding.videoStatus.setVisibility(View.GONE);
            binding.audioStatus.setVisibility(View.GONE);
            if(binding.deleteStatus.getVisibility()==View.VISIBLE){
                binding.deleteStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close));
                binding.deleteStatus.setVisibility(View.GONE);
            }
            binding.fabOpenMenu.setRotation(0);
        }else{
            binding.fabOpenMenu.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate));
            binding.imageStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.open));
            binding.videoStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.open));
            binding.audioStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.open));
            binding.imageStatus.setVisibility(View.VISIBLE);
            binding.videoStatus.setVisibility(View.VISIBLE);
            binding.audioStatus.setVisibility(View.VISIBLE);
            if(viewModel.currentStatus!=null){
                binding.deleteStatus.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.open));
                binding.deleteStatus.setVisibility(View.VISIBLE);
            }
            binding.fabOpenMenu.setRotation(45);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Handler handler=new Handler(getMainLooper());
        if(requestCode==RC_GET_STATUS_MEDIA&&resultCode==RESULT_OK){
            if(data==null)
                return;
            StatusUploadDialog dialog=new StatusUploadDialog(this);
            dialog.setImage(data.getData());
            Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null);
            if(cursor!=null&&cursor.moveToFirst())
            dialog.setName(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)));
            if (cursor != null) {
                cursor.close();
            }
            dialog.addUploadListener((v)->{
                HashMap<String,Object> value=new HashMap<>();
                value.put("statusCount", FieldValue.increment(1));
                FirebaseFirestore.getInstance().collection("users/").document(userId)
                        .set(value, SetOptions.merge());
                schedule(Status.STATUS_TYPE_IMAGE,data,".png",viewModel.statusId,dialog.getStatusText());
                viewModel.statusId++;
               handler.postDelayed(dialog::cancel,4000);
            });
            dialog.show();


        }
        else if(requestCode==RC_GET_STATUS_AUDIO){
            if(data==null)
                return;
            StatusUploadDialog dialog=new StatusUploadDialog(this);
            dialog.setImage(data.getData());
            Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null);
            if(cursor!=null&&cursor.moveToFirst())
                dialog.setName(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)));
            if (cursor != null) {
                cursor.close();
            }
            dialog.addUploadListener((v)->{
                HashMap<String,Object> value=new HashMap<>();
                value.put("statusCount", FieldValue.increment(1));
                FirebaseFirestore.getInstance().collection("users/").document(userId)
                        .set(value, SetOptions.merge());
                schedule(Status.STATUS_TYPE_AUDIO,data,".mp3",viewModel.statusId,dialog.getStatusText());
                viewModel.statusId++;
                handler.postDelayed(dialog::cancel,4000);
            });
            dialog.show();

        }
        else if(requestCode==RC_GET_STATUS_VIDEO){
            if(data==null)
                return;
            StatusUploadDialog dialog=new StatusUploadDialog(this);
            dialog.setImage(data.getData());
            Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null);
            if(cursor!=null&&cursor.moveToFirst())
                dialog.setName(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)));
            if (cursor != null) {
                cursor.close();
            }
            dialog.addUploadListener((v)->{
                HashMap<String,Object> value=new HashMap<>();
                value.put("statusCount", FieldValue.increment(1));
                FirebaseFirestore.getInstance().collection("users/").document(userId)
                        .set(value, SetOptions.merge());
                schedule(Status.STATUS_TYPE_VIDEO,data,".mp4",viewModel.statusId,dialog.getStatusText());
                viewModel.statusId++;
                handler.postDelayed(dialog::cancel,4000);
            });
            dialog.show();

        }
    }
    public void schedule(String type, Intent data, String ext, long statusId, String statusText){
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(StatusUploadWorker.class)
                .setInputData(new Data.Builder()
                        .putString("userId",userId)
                        .putString("mediaType",type)
                        .putLong("id",statusId)
                        .putString("statusText",statusText)
                        .putString("data",data.getData().toString())
                        .putString("extension",ext).build())
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiveData<List<Status>> statusLiveData=viewModel.getStatuses();
        if(statusLiveData!=null )
        statusLiveData.observe(this, this::setAdapter);


    }
    private void setAdapter(List<Status> statusList){
        Log.d(TAG, "setAdapter: "+(statusList==null?null:statusList.size()));

        if(adapter==null){
            Status status=new Status();
            status.setUserId(viewModel.getUser().getUserId());
            status.setStatusUri(viewModel.getUser().getProfilePicture());
            status.setMediaStatus(Status.MEDIA_STATUS_LOCAL);
            status.setStatusType(Status.STATUS_TYPE_IMAGE);
            if (statusList == null) {
                statusList = new ArrayList<>();
            }
            statusList.add(status);
            adapter= new PagerAdapter(this, statusList);
            statusListView.setAdapter(adapter);
        }else{
            adapter.setStatus(statusList);
            adapter.notifyDataSetChanged();


        }
    }
    public void back(View view) {
        super.onBackPressed();
    }



    public void showMood(View view) {

        String date=new SimpleDateFormat("dd MMM yy", Locale.ROOT).format(user.getMoodSince());
        Toast toast=Toast.makeText(getApplicationContext(),"Last Update: "+date,Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP|Gravity.END,10,30);

        toast.show();
    }
 private static class PagerAdapter extends FragmentStateAdapter {
     private List<Status> statuses;

     public Status getCurrent() {
         return current;
     }

     private Status current;


     public PagerAdapter(@NonNull FragmentActivity fragmentActivity, List<Status> statuses) {

         super(fragmentActivity);
         this.statuses = statuses;
     }

     @NonNull
     @Override
     public Fragment createFragment(int position) {



             Status status = statuses.get(position);
             current=status;
             if (status.getStatusType().equals(Status.STATUS_TYPE_IMAGE)) {
                 return new ImageFragment();
             } else if (status.getStatusType().equals(Status.STATUS_TYPE_VIDEO)) {
                 return new VideoFragment();
             } else {
                 return new AudioFragment();
             }


     }




     @Override
     public int getItemCount() {
         if(statuses==null)
             return 0;
         return statuses.size();
     }

     public synchronized void setStatus(List<Status> statusList) {
         statuses=statusList;

     }

     public synchronized void setStatus(Status status) {
         int pos=statuses.size()-1;
         statuses.add(pos,status);
         notifyItemInserted(pos);
     }
 }


    public static class ImageFragment extends Fragment {
        private Status status;
        private ImageActivityViewModel viewModel;
        private ActivityImageBinding activityImageBinding;
        private ImageFragmentBinding binding;
        private boolean isRunning;
        //private int current=-1;
        private ImageActivity activity;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity= (ImageActivity) getActivity();
            status=activity.adapter.getCurrent();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
             super.onCreateView(inflater, container, savedInstanceState);
             binding=ImageFragmentBinding.inflate(inflater);
             activityImageBinding=activity.getBinding();
             viewModel=activity.getViewModel();
            Log.d(TAG, "onCreateView: position frag : "+activity.pagerIndex);

            if(status.getMediaStatus().equals(Status.MEDIA_STATUS_LOCAL)) {
                 Log.d(TAG, "onCreateView: image Uri"+status.getStatusUri());
                 binding.statusProgress.setVisibility(View.GONE);
                 Glide.with(requireContext()).load(status.getStatusUri())
                         .diskCacheStrategy(DiskCacheStrategy.NONE)
                         .skipMemoryCache(true)
                         .into(binding.statusImage);
                 binding.statusText.setText(status.getStatusText());
             }else{
                 binding.statusProgress.setVisibility(View.VISIBLE);
                 new StatusMediaDownload(status,requireContext(),viewModel.getStatusDao()).start();
             }
             return binding.getRoot();
        }

        @Override
        public void onResume() {
            super.onResume();
            isRunning=true;
            Handler handler=new Handler(Looper.getMainLooper());
            Log.d(TAG, "onResume: viewModel in image frag "+viewModel);
            if(status.getStatusId()!=null)
            viewModel.currentStatus=status;
            else
                viewModel.currentStatus=null;
            if(activityImageBinding.statusList.canScrollHorizontally(ViewPager2.LAYOUT_DIRECTION_LTR)){
               handler.postDelayed(()->{
                   if(isRunning)
                   activityImageBinding.statusList.setCurrentItem(activityImageBinding.statusList.getCurrentItem()+1);
             },5000);

            }
        }

        @Override
        public void onPause() {
            super.onPause();
            isRunning=false;
        }
    }

    public static class VideoFragment extends Fragment {
        private  Status status;
        private  ImageActivityViewModel viewModel;
        private  VideoFragmentBinding binding;
        private  ActivityImageBinding activityImageBinding;
        private boolean isRunning=false;
       // private int current=-1;
        private ImageActivity activity;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity= (ImageActivity) getActivity();
            status=activity.adapter.getCurrent();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            binding=VideoFragmentBinding.inflate(inflater);
            ImageActivity activity= (ImageActivity) getActivity();
            activityImageBinding=activity.getBinding();
            viewModel=activity.getViewModel();
            Log.d(TAG, "onCreateView: position frag : "+activity.pagerIndex);


            if(status.getMediaStatus().equals(Status.MEDIA_STATUS_NOT_LOCAL)){

                binding.statusProgress.setVisibility(View.VISIBLE);
                new StatusMediaDownload(status,requireContext(),viewModel.getStatusDao()).start();
            }else{
                //play video add listeners

                binding.statusText.setText(status.getStatusText());
                binding.statusProgress.setVisibility(View.GONE);
                binding.statusVideo.setVideoURI(Uri.parse(status.getStatusUri()));

                binding.getRoot().setOnClickListener(view1 -> {
                   if(binding.statusVideo.isPlaying()){
                       binding.statusVideo.pause();
                       binding.playButton.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.fade_in));
                       binding.playButton.setVisibility(View.VISIBLE);
                       binding.playButton.setForeground(requireContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                   }else{
                       binding.statusVideo.start();
                       new TrackStatus().start();
                       binding.playButton.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.fade_in));
                       binding.playButton.setVisibility(View.GONE);
                       binding.playButton.setForeground(requireContext().getDrawable(R.drawable.ic_pause));

                   }
                });
            }
            return binding.getRoot();
        }
         private class TrackStatus extends Thread{
             @Override
             public void run() {
                     Handler handler=new Handler(Looper.getMainLooper());
                     int duration=binding.statusVideo.getDuration();
                 int current=0;
                     while (binding.statusVideo.isPlaying()){
                        current =binding.statusVideo.getCurrentPosition();
                         int percent=(current*100)/duration;
                         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                             handler.post(()->binding.videoProgress.setProgress(percent,true));
                         }else{

                              handler.post(()->binding.videoProgress.setProgress(percent));
                          }

                     }
                     if(activityImageBinding.statusList.canScrollHorizontally(ViewPager2.LAYOUT_DIRECTION_LTR)){
                         int finalCurrent = current;
                         handler.post(()->
                         {
                             if((duration- finalCurrent)<200&&isRunning)
                             activityImageBinding.statusList.setCurrentItem(activityImageBinding.statusList.getCurrentItem() + 1);
                         });

                     }

             }
         }
        @Override
        public void onResume() {
            super.onResume();
            isRunning=true;
            if(status.getMediaStatus().equals(Status.MEDIA_STATUS_LOCAL)){
                binding.statusVideo.start();
                new TrackStatus().start();
            }
            if(status.getStatusId()!=null)
                viewModel.currentStatus=status;
            else
                viewModel.currentStatus=null;

        }

        @Override
        public void onPause() {
            super.onPause();
            isRunning=false;
            if(binding.statusVideo.isPlaying()){
                binding.statusVideo.pause();
            }
        }
    }
    public static class AudioFragment extends Fragment{
        private  Status status;
        private  ImageActivityViewModel viewModel;
        private MediaPlayer player;
        private AudioFragmentBinding binding;
        private ActivityImageBinding activityImageBinding;
        private boolean isRunning=false;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ImageActivity activity = (ImageActivity) getActivity();
            status=activity.adapter.getCurrent();
        }
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
             super.onCreateView(inflater, container, savedInstanceState);
             binding=AudioFragmentBinding.inflate(inflater);
            ImageActivity activity= (ImageActivity) getActivity();
            activityImageBinding=activity.getBinding();
            viewModel=activity.getViewModel();
            Log.d(TAG, "onCreateView: position frag : "+activity.pagerIndex);
            if(status.getMediaStatus().equals(Status.MEDIA_STATUS_LOCAL)) {

                 binding.statusText.setText(status.getStatusText());
                 binding.statusProgress.setVisibility(View.GONE);
                 player = MediaPlayer.create(requireContext(), Uri.parse(status.getStatusUri()));

                binding.audioStatusImg.setOnClickListener(view1 -> {
                     if(player==null)
                         return;
                     if(player.isPlaying()){
                         player.pause();
                         binding.playButton.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.fade_in));
                         binding.playButton.setVisibility(View.VISIBLE);
                         binding.playIcon.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                     }else{
                         player.start();
                         new TrackStatus().start();
                         binding.playButton.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.fade_in));
                         binding.playButton.setVisibility(View.GONE);
                         binding.playIcon.setImageDrawable(requireContext().getDrawable(R.drawable.ic_pause));

                     }
                 });
             }else{

                 binding.statusProgress.setVisibility(View.VISIBLE);
                 new StatusMediaDownload(status,requireContext(),viewModel.getStatusDao()).start();
             }
            return binding.getRoot();

        }
        private class TrackStatus extends Thread{
            @Override
            public void run() {
                Handler handler=new Handler(Looper.getMainLooper());
                int duration=player.getDuration();
                int current=0;
                while (player.isPlaying()){
                    current=player.getCurrentPosition();
                    Log.d(TAG, "run: track "+current+"/"+duration);
                    int percent=(current*100)/duration;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        handler.post(()->binding.audioProgress.setProgress(percent,true));
                    }else{

                        handler.post(()->binding.audioProgress.setProgress(percent));
                    }

                }
                if(activityImageBinding.statusList.canScrollHorizontally(ViewPager2.LAYOUT_DIRECTION_LTR)){
                    int finalCurrent = current;
                    handler.post(()->
                    {
                        Log.d(TAG, "run: def : "+(duration- finalCurrent));
                        if((duration- finalCurrent)<=200&&isRunning)
                            activityImageBinding.statusList.setCurrentItem(activityImageBinding.statusList.getCurrentItem() + 1);
                    });}

            }
        }
        @Override
        public void onResume() {
            super.onResume();
            isRunning=true;
            if(player!=null){

                binding.playButton.startAnimation(AnimationUtils.loadAnimation(requireContext(),R.anim.fade_in));
                binding.playButton.setVisibility(View.GONE);
                player.start();
                new TrackStatus().start();

            }
            if(status.getStatusId()!=null)
                viewModel.currentStatus=status;
            else
                viewModel.currentStatus=null;
        }

        @Override
        public void onPause() {
            super.onPause();
            isRunning=false;
            if (player!=null&&player.isPlaying()){
                player.pause();

            }
        }
    }
    public static class StatusMediaDownload extends Thread{
        private static final String TAG = "StatusMediaDownload";
        private final Status status;
        private final Context context;
        private final StatusDao statusDao;
        private MutableLiveData<Integer> liveData;

        public StatusMediaDownload(Status status, Context context, StatusDao statusDao) {
            this.status = status;
            this.context = context;
            this.statusDao = statusDao;
        }

        @Override
        public void run() {
            Log.d(TAG, "run: started");
            String ext;
            if(status.getStatusType().equals(Status.STATUS_TYPE_IMAGE))
                ext=".png";
            else if(status.getStatusType().equals(Status.STATUS_TYPE_VIDEO))
                ext=".mp4";
            else ext=".mp3";

            File filedir=new File(context.getFilesDir(),"statuses/");
            if(!filedir.exists()){
                filedir.mkdirs();

            }
            File file=new File(filedir,status.getStatusId()+ext);
            Log.d(TAG, "run: Download started name "+file.getName());
            //TODO replace file.getName() with status.getStatusId() maybe work
            FileDownloadTask task= FirebaseStorage.getInstance().getReference("statuses/"+file.getName()).getFile(file);
            task.addOnProgressListener(snapshot -> {
                long transfer=snapshot.getBytesTransferred();
                Log.d(TAG, "run: progress update to : "+transfer);
                long total=snapshot.getTotalByteCount();
                int percentage= (int) ((transfer*100)/total);
                if(liveData!=null)
                    liveData.postValue(percentage);
            }) ;
            task.addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "run: Download Done : "+taskSnapshot.getTotalByteCount());
                status.setMediaStatus(Status.MEDIA_STATUS_LOCAL);
                status.setStatusUri(file.getAbsolutePath());
               new Thread(()-> statusDao.setStatus(status)).start();
            });
        }
    }

}
