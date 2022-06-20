package com.sk.mymassenger.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.sk.mymassenger.databinding.SettingUpProfileDialogLayoutBinding;
import com.sk.mymassenger.databinding.SpinnerBinding;

public class SetUpProfileDialog extends Dialog {
    private SettingUpProfileDialogLayoutBinding binding;


    public SetUpProfileDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=SettingUpProfileDialogLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);
        AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) binding.setupAccountDialog.getDrawable();
        if(drawable!=null){
            drawable.start();
        }
    }


}