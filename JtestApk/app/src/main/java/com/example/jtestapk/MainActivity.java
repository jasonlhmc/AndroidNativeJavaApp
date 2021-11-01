package com.example.jtestapk;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
//        setContentView(R.layout.activity_main);
//    }
//
//    public void onStartButtonPress(View view) {
//        Log.v("INFO", "Start button Clicked");
        setContentView(R.layout.activity_menu);

        Button luckyDiceButton = (Button) findViewById(R.id.luckyDiceButton);
        luckyDiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Lucky Dice button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, LuckyDiceActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        Button checkCurButton = (Button) findViewById(R.id.checkCurrencyButton);
        checkCurButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Check Currency button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, CheckCurrencyActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        Button qrCodeButton = (Button) findViewById(R.id.menuQRCodeButton);
        qrCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "QR Code button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, QRCodeActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        qrCodeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean  onLongClick(View view) {
                Log.v("INFO", "QR Code button LONG CLICKED");
                try {
                    Intent intent = new Intent(MainActivity.this, QRCodeSubMenuActivity.class);
                    startActivity(intent);
//                    Toast.makeText(getApplicationContext(),"ROLL", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        Button jotNotesButton = (Button) findViewById(R.id.jotNotesButton);
        jotNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "QR Code button Clicked");
                try {
                    Intent intent = new Intent(MainActivity.this, NotesActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        jotNotesButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    Intent intent = new Intent(MainActivity.this, NotesListViewActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

}

//TODO
//link up with raspberry Pi cloud service
//cur click in full list and calculate