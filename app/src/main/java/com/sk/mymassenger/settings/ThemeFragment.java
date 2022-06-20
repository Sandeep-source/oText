package com.sk.mymassenger.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.sk.mymassenger.R;
import com.sk.mymassenger.databinding.ThemeSelectionBinding;

public class ThemeFragment extends Fragment {

    private ThemeSelectionBinding binding;
    @SuppressLint("ResourceType")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding=ThemeSelectionBinding.inflate(getLayoutInflater());
        TypedValue value=new TypedValue();
        requireActivity().getTheme().resolveAttribute(R.attr.colorPrimary,value,true);
        binding.rec.msgContainer.setBackgroundColor(value.data);
        binding.themeSelection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int theme=R.style.oText_Theme;
                switch (radioGroup.getId()){
                    case R.id.defaultTheme:
                          theme=R.style.oText_Theme;
                          break;
                    case R.id.cyanTheme:
                        theme=R.style.oText_Theme_Cyan;
                        break;
                    case R.id.skyBlue:
                        theme=R.style.oText_Theme_Blue;
                        break;
                    case R.id.monoPink:
                        theme=R.style.oText_Theme_Mono;
                        break;
                    case R.id.monoPinkLight:
                        theme=R.id.monoPinkLight;
                }

                requireActivity().setTheme(theme);
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putInt("theme",theme).apply();

            }
        });
        return binding.getRoot();
    }
}
