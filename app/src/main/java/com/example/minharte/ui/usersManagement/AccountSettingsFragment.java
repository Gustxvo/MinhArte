package com.example.minharte.ui.usersManagement;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.minharte.databinding.FragmentAccountSettingsBinding;
import com.example.minharte.databinding.FragmentSearchBinding;


public class AccountSettingsFragment extends Fragment {

   FragmentAccountSettingsBinding binding;

    public AccountSettingsFragment() {}


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();



        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}