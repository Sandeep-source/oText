package com.sk.mymassenger;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.sk.mymassenger.databinding.ActivityHomeBinding;
import com.sk.mymassenger.viewmodels.HomeViewModel;

public class HomeActivity extends FragmentActivity {
    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityHomeBinding.inflate(getLayoutInflater());
        viewModel=HomeViewModel.getInstance(this);
        viewModel.mode=getIntent().getStringExtra("userMode");
        setContentView(binding.getRoot());


    }


}
