package com.sk.mymassenger.chat;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sk.mymassenger.R;
import com.sk.mymassenger.databinding.FragmentImageBoardBinding;
import com.sk.mymassenger.pixabay.ImageResponse;
import com.sk.mymassenger.pixabay.Images;
import com.sk.mymassenger.pixabay.PixabayApi;
import com.sk.mymassenger.viewmodels.HomeViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImageBoard extends BottomSheetDialogFragment {

    private final String key;
    private static final String TAG = "ImageBoard";
    FragmentImageBoardBinding binding;
    private HomeViewModel viewModel;
    private long msg_ids_count;

    public ImageBoard() {
        // Required empty public constructor
        key=getString(R.string.PIXABAY_API);
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        viewModel=new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding=FragmentImageBoardBinding.inflate(inflater);
        init();
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("https://pixabay.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        PixabayApi api=retrofit.create(PixabayApi.class);
        load(api,"recent");
        binding.searchImage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence!=null&&charSequence.length()>0)
                 load(api,charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return binding.getRoot();
    }

    private void init() {
        new Thread(()->{
            FirebaseFirestore.getInstance().collection( "Messages" )
                    .document( viewModel.getUser().getUserId()+"to"+viewModel.getReceiver(null)
                            .getUserId()+"messageidscount" )
                    .get().addOnCompleteListener( task -> {
                DocumentSnapshot documentSnapshot=task.getResult();
                if(documentSnapshot!=null&&documentSnapshot.exists()) {
                    //noinspection ConstantConditions
                    msg_ids_count = documentSnapshot.getLong( "count" );
                }else{
                    msg_ids_count= 0L;
                }
            } ).addOnFailureListener( e -> {

            } );
        });
    }

    public void load(PixabayApi api,String val){
        Call<ImageResponse> call= api.getImages(key,val);
        call.enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                Log.d(TAG, "onResponse: done "+call);
                if(!response.isSuccessful()){
                    try {
                        InputStream body=response.errorBody().byteStream();
                        byte arr[]=new byte[body.available()];
                        body.read(arr);
                        Log.d(TAG, "onResponse: error : "+new String(arr));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "onResponse: message : "+response.message() );
                    return;
                }
                Log.d(TAG, "onResponse: body : ");
                binding.imageList.setAdapter(new ImageAdapter(response.body().getImagesList()));
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {

            }
        });
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private List<Images> imageList;
        private static final String TAG = "ImageAdapter";

        public ImageAdapter(List<Images> imagesList) {
            this.imageList=imagesList;
            Log.d(TAG, "ImageAdapter: "+(imagesList==null?null:imagesList.size()));
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.grid_image,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Images image=imageList.get(position);
            Glide.with(requireContext()).load(image.getPreviewURL()).into(holder.image);
            holder.image.setOnClickListener(view -> {
                new Thread(()->{
                    viewModel.sendImage(Uri.parse(image.getLargeImage()),"png", "sent/images/",msg_ids_count,"pixa");
                }).start();
                 getActivity().onBackPressed();
            });
        }



        @Override
        public int getItemCount() {
            if(imageList==null)
                return 0;
            else
                return imageList.size();
        }
        private class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                image=itemView.findViewById(R.id.image_item);
            }
        }
    }
    }

