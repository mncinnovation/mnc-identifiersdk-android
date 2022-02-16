package id.mncinnovation.mncidentifiersdk.java;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import id.mncinnovation.face_detection.MNCIdentifier;
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity  {

    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
    }
}