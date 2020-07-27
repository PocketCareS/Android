package com.ub.pocketcares.introduction;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.appintro.SlidePolicy;
import com.github.appintro.SlideSelectionListener;

import com.ub.pocketcares.R;

import static android.app.Activity.RESULT_CANCELED;

public class BluetoothOnFragment extends Fragment implements SlidePolicy, SlideSelectionListener {
    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bluetooth_enable_fragment, container, false);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return rootView;
    }

    @Override
    public boolean isPolicyRespected() {
        if (mBluetoothAdapter == null) {
            return true;
        }
        return mBluetoothAdapter.isEnabled();
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getContext(), getString(R.string.bluetooth_off), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSlideDeselected() {

    }

    @Override
    public void onSlideSelected() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
}
