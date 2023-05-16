package com.quectel.camera.display;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("text","1245");
        setResult(RESULT_OK,intent);
        super.onBackPressed();

    }
}