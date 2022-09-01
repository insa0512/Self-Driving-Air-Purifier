package com.example.aduino2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class start extends Activity {

        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logo);

        Handler hand = new Handler(); //시간지연으로 처리 천천히 화면 전환

        hand.postDelayed(new Runnable() {

            @Override
            public void run() { //인텐드로 화면 전환
                // TODO Auto-generated method stub
                Intent i = new Intent(start.this, MainActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out); //화면 부드럽게 전환
                finish();

            }
        }, 3000);


    }
}