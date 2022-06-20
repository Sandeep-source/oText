package com.sk.mymassenger.dialog;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.sk.mymassenger.databinding.StatusDialogBinding;

public class StatusUploadDialog extends Dialog {
    private StatusDialogBinding binding;
    private View.OnClickListener listener;

    public StatusUploadDialog(@NonNull Context context) {

        super(context);

        binding=StatusDialogBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());
        binding.cancel.setOnClickListener((v)->{cancel();});
        binding.uploadButton.setOnClickListener((v)->{
            binding.statusUploadProgress.setVisibility(View.VISIBLE);
            binding.uploadText.setText("Uploading...");
            if(listener!=null)
                listener.onClick(v);

        });

    }
    public void addUploadListener(View.OnClickListener listener){
        this.listener=listener;

    }
    public void setImage(Uri uri){
        Glide.with(getContext()).load(uri).fitCenter().into(binding.statusThumb);
    }
    public void setName(String name){
        binding.statusFileName.setText(name);
    }
    public String getStatusText(){
        return binding.statusTextInput.getText().toString();
    }
}
