package com.example.jtestapk;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.model.DiceModelObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class LuckyDiceActivity extends AppCompatActivity {

    private Ringtone notificationSound;
    private Switch luckyDiceOrderSwitch;
//    private boolean isAfterSelectedDice;
    private boolean luckyDiceHideRollRow = false;
    private Switch luckyDiceHideSwitch;
    private Switch luckyDiceShakeSwitch;
    private boolean luckyDiceShakeEnabled = false;
//    private TableLayout luckyDiceResultTable;
//    private TableRow diceResultRow;
//    private TableRow diceCheckboxRollRow;
    private SensorManager mSensorManager;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private boolean isLuckyDiceRollButtonLongPressed = false;
    private List<DiceModelObject> diceModelObjectList;
    private HashMap<Integer, DiceModelObject> checkBoxDiceObjMap;
    private List<Integer> luckyDiceCheckBoxIdList;
    private TableLayout luckyDiceRollingTable;
    private TableRow diceRollRow;
    private Button luckyDiceRollButton;
    private boolean postRolling;
    private int forceRoll;
    private int randomRoll;
    private Handler luckyDiceRollHandler = new Handler();
    private Runnable luckyDiceRollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (luckyDiceRollButton.isPressed() && !postRolling) {
                // run luckyDiceRollingRunnable() and recall the runnable in 200 milliseconds.
                luckyDiceOrderSwitch.setChecked(false);
//                rollDice();
                rollDiceNewVersion();
                vibration();
                luckyDiceRollHandler.postDelayed(luckyDiceRollingRunnable, 200);
            } else if (postRolling && forceRoll < randomRoll) {
                forceRoll += 1;
                if (forceRoll == randomRoll) {
                    mapDiceObj();
                    rollingProgressBar.setVisibility(View.INVISIBLE);
                } else {
                    luckyDiceOrderSwitch.setChecked(false);
                    rollDiceNewVersion();
                    vibration();
                }
                luckyDiceRollHandler.postDelayed(luckyDiceRollingRunnable, 200);
            }
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (luckyDiceRollHandler != null) {
            luckyDiceRollHandler.removeCallbacks(luckyDiceRollingRunnable);
        }
    }
    private ProgressBar rollingProgressBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_lucky_dice);

        //init Dices
        diceModelObjectList = new ArrayList<DiceModelObject>();
        for (int i = 0; i <= 4; i++) {
            DiceModelObject diceModelObject = new DiceModelObject();
            diceModelObject.setDiceId(i + 1);
            int diceNumber = new Random().nextInt(6) + 1;
            diceModelObject.setDiceNumberInt(diceNumber);
            diceModelObject.setDiceNumberStr(convertIntToDiceStr(diceNumber));
            diceModelObject.setLocked(false);
            diceModelObject.setRolled(false);
            diceModelObject.setSelected(false);
            diceModelObjectList.add(diceModelObject);
//            Log.v("INFO", diceModelObject.toString());
        }

        //init Notification Sound
        notificationSound = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));


        //init TableLayout
        luckyDiceCheckBoxIdList = new ArrayList<Integer>();
        checkBoxDiceObjMap = new HashMap<Integer, DiceModelObject>();
        luckyDiceRollingTable = (TableLayout) findViewById(R.id.luckyDiceRollingTable);
        luckyDiceRollingTable.setBackgroundResource(R.drawable.common_table_border);
//        setUpDiceRollingTableLayout();
        initDiceRollingTableLayout();

        //init prograssBar
        rollingProgressBar = new ProgressBar(this);
        luckyDiceRollingTable.addView(rollingProgressBar);
        rollingProgressBar.setVisibility(View.INVISIBLE);

        //map CheckBox
        for (int luckyDiceCheckBoxId : luckyDiceCheckBoxIdList) {
//            Log.v("INFO", "luckyDiceCheckBoxId = " + luckyDiceCheckBoxId);
//            Log.v("INFO", "findViewById(" + luckyDiceCheckBoxId + ") = " + findViewById(luckyDiceCheckBoxId));
            CheckBox luckyDiceCheckBox = (CheckBox) findViewById(luckyDiceCheckBoxId);
            luckyDiceCheckBox.setOnClickListener(luckyDiceCheckBoxOnClickListener);
        }

        luckyDiceOrderSwitch = (Switch) findViewById(R.id.luckDiceOrderSwitch);
//        luckyDiceOrderSwitch.setOnCheckedChangeListener(luckyDiceListeners.luckyDiceOrderSwitchOnCheckedChangeListener);
        luckyDiceOrderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Collections.sort(diceModelObjectList, Comparator.comparing(DiceModelObject::getDiceNumberInt));
                } else {
                    Collections.sort(diceModelObjectList, Comparator.comparing(DiceModelObject::getDiceId));
                }
                int i = 0;
                for (Map.Entry<Integer, DiceModelObject> entry : checkBoxDiceObjMap.entrySet()) {
                    entry.setValue(diceModelObjectList.get(i));
                    CheckBox diceCheckBox = (CheckBox) findViewById(entry.getKey());
                    diceCheckBox.setText(diceModelObjectList.get(i).getDiceNumberStr());
                    if ((diceModelObjectList.get(i).getDiceNumberInt() == 1 || diceModelObjectList.get(i).getDiceNumberInt() == 4) && !diceModelObjectList.get(i).isSelected()) {
                        diceCheckBox.setTextColor(Color.RED);
                    } else {
                        diceCheckBox.setTextColor(Color.WHITE);
                    }
                    if (diceModelObjectList.get(i).isSelected()) {
                        diceCheckBox.setChecked(true);
                        diceCheckBox.setTextColor(Color.GREEN);
                    } else {
                        diceCheckBox.setChecked(false);
                    }
                    i++;
                }
            }
        });

        luckyDiceHideSwitch = (Switch) findViewById(R.id.diceHideSwitch);
        luckyDiceHideSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    luckyDiceHideRollRow = true;
                    diceRollRow.setVisibility(View.INVISIBLE);
                } else {
                    luckyDiceHideRollRow = false;
                    diceRollRow.setVisibility(View.VISIBLE);
                }
            }
        });

        luckyDiceShakeSwitch = (Switch) findViewById(R.id.diceShakeSwitch);
        luckyDiceShakeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    luckyDiceShakeEnabled = true;
                    luckyDiceRollButton.setEnabled(false);
                    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    Objects.requireNonNull(mSensorManager).registerListener(luckyDiceShakeEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
                    mAccel = 10f;
                    mAccelCurrent = SensorManager.GRAVITY_EARTH;
                    mAccelLast = SensorManager.GRAVITY_EARTH;
                } else {
                    luckyDiceShakeEnabled = false;
                    luckyDiceRollButton.setEnabled(true);
                    mSensorManager.unregisterListener(luckyDiceShakeEventListener);
                    mSensorManager = null;
                }
            }
        });

        Button luckyDiceUnlockButton = (Button) findViewById(R.id.luckyDiceUnlockButton);
        luckyDiceUnlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Lucky Dice Unlock button Clicked");
                try {
                    for (Map.Entry<Integer, DiceModelObject> entry : checkBoxDiceObjMap.entrySet()) {
                        entry.getValue().setSelected(false);
                    }
                    for (DiceModelObject diceModelObject : diceModelObjectList) {
                        diceModelObject.setSelected(false);
                    }
                    for (int checkBoxId : luckyDiceCheckBoxIdList) {
                        CheckBox checkBox = (CheckBox) findViewById(checkBoxId);
                        checkBox.setChecked(false);
                        if (convertDiceStrToInt(checkBox.getText().toString()) == 1 || convertDiceStrToInt(checkBox.getText().toString()) == 4) {
                            checkBox.setTextColor(Color.RED);
                        } else {
                            checkBox.setTextColor(Color.WHITE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
                Log.v("INFO", "Done onCheckCurClicked");
            }
        });

        luckyDiceRollButton = (Button) findViewById(R.id.luckyDiceRollButton);
        luckyDiceRollButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean  onLongClick(View view) {
                Log.v("INFO", "Lucky Dice Roll Button LONG CLICKED");
                try {
                    forceRoll = 0;
                    randomRoll = new Random().nextInt(5) + 3;
                    postRolling = false;
                    notificationSound.play();
                    luckyDiceRollingRunnable.run();
                    luckyDiceOrderSwitch.setClickable(false);
                    luckyDiceShakeSwitch.setClickable(false);
//                    Toast.makeText(getApplicationContext(),"ROLL", Toast.LENGTH_SHORT).show();
                    rollingProgressBar.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_SHORT).show();
                }
                isLuckyDiceRollButtonLongPressed = true;
                return true;
            }
        });
        luckyDiceRollButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View pView, MotionEvent pEvent) {
                pView.onTouchEvent(pEvent);
                if (pEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (isLuckyDiceRollButtonLongPressed) {
                        Log.v("INFO", "Lucky Dice Roll Button RELEASED");
                        try {
//                            Log.v("INFO", diceModelObjectList);
                            luckyDiceOrderSwitch.setClickable(true);
                            luckyDiceShakeSwitch.setClickable(true);
//                            mapDiceObj();
//                            rollingProgressBar.setVisibility(View.INVISIBLE);
                            postRolling = true;
                            Toast.makeText(getApplicationContext(),"DONE", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                        isLuckyDiceRollButtonLongPressed = false;
                    }
                }
                return false;
            }
        });
    }

    private View.OnClickListener luckyDiceCheckBoxOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CheckBox checkBox = (CheckBox) findViewById(view.getId());
            if (checkBox.isChecked()) {
                checkBox.setTextColor(Color.GREEN);
                int diceId = 0;
                for (Map.Entry<Integer, DiceModelObject> entry : checkBoxDiceObjMap.entrySet()) {
                    if (entry.getKey() == view.getId()) {
                        diceId = entry.getValue().getDiceId();
                        entry.getValue().setSelected(true);
                        break;
                    }
                }
                for (DiceModelObject diceModelObject : diceModelObjectList) {
                    if (diceModelObject.getDiceId() == diceId) {
                        diceModelObject.setSelected(true);
                    }
                }
            } else {
                int diceId = 0;
                for (Map.Entry<Integer, DiceModelObject> entry : checkBoxDiceObjMap.entrySet()) {
                    if (entry.getKey() == view.getId()) {
                        diceId = entry.getValue().getDiceId();
                        entry.getValue().setSelected(false);
                        break;
                    }
                }
                for (DiceModelObject diceModelObject : diceModelObjectList) {
                    if (diceModelObject.getDiceId() == diceId) {
                        diceModelObject.setSelected(false);
                    }
                }
                if (convertDiceStrToInt(checkBox.getText().toString()) == 1 || convertDiceStrToInt(checkBox.getText().toString()) == 4) {
                    checkBox.setTextColor(Color.RED);
                } else {
                    checkBox.setTextColor(Color.WHITE);
                }
            }
        }
    };

    private final SensorEventListener luckyDiceShakeEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            if (mAccel > 12) {
                notificationSound.play();
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(500);
                }
//                rollDice();
                rollDiceNewVersion();
                mapDiceObj();
//                Toast.makeText(getApplicationContext(),"ROLL", Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private boolean diceIsValidatedForResultTableLayout(DiceModelObject diceModelObject) {
        boolean isValidated = false;
        if ((diceModelObject.isSelected() && diceModelObject.isLocked()) || diceModelObject.isRolled()) {
            isValidated = true;
        }
        return isValidated;
    }

    private boolean diceIsValidatedForBackToRollingTableLayout(DiceModelObject diceModelObject) {
        boolean isValidated = false;
        if (diceModelObject.isSelected() && diceModelObject.isLocked() && !diceModelObject.isRolled()) {
            isValidated = true;
        }
        return isValidated;
    }

    private void initDiceRollingTableLayout() {
        diceRollRow = new TableRow(this);
        diceRollRow.setPadding(16, 8, 16, 8);
        diceRollRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        diceRollRow.setWeightSum(1f);
        diceRollRow.removeAllViews();
        for (DiceModelObject diceModelObject : diceModelObjectList) {
            CheckBox diceCheckBox = new CheckBox(this);
            diceCheckBox.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 0.2f));
            diceCheckBox.setWidth(0);
            diceCheckBox.setTextSize(108);
            diceCheckBox.setGravity(Gravity.CENTER);
            diceCheckBox.setButtonDrawable(null);
            TypedValue typeValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple,typeValue, true);
            diceCheckBox.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, typeValue.resourceId);
            diceCheckBox.setText(diceModelObject.getDiceNumberStr());
            if (diceModelObject.getDiceNumberInt() == 1 || diceModelObject.getDiceNumberInt() == 4) {
                diceCheckBox.setTextColor(Color.RED);
            }
//            diceCheckBox.setClickable(false);
            int checkBoxId = View.generateViewId();
            diceCheckBox.setId(checkBoxId);
//            Log.v("INFO", "diceCheckBox.getId() = " + diceCheckBox.getId());
            luckyDiceCheckBoxIdList.add(checkBoxId);
            checkBoxDiceObjMap.put(checkBoxId, diceModelObject);
            diceRollRow.addView(diceCheckBox);
        }
        if (luckyDiceHideRollRow) {
            diceRollRow.setVisibility(View.INVISIBLE);
        } else {
            diceRollRow.setVisibility(View.VISIBLE);
        }
        luckyDiceRollingTable.addView(diceRollRow);
    }

//    private void rollDice() {
//        diceRollRow.removeAllViews();
//        for (DiceModelObject diceModelObject : diceModelObjectList) {
//            if (!diceModelObject.isLocked()) {
//                int diceNumber = new Random().nextInt(6) + 1;
//                diceModelObject.setDiceNumberInt(diceNumber);
//                diceModelObject.setDiceNumberStr(convertIntToDiceStr(diceNumber));
//
//                CheckBox diceCheckBox = new CheckBox(this);
//                diceCheckBox.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 0.2f));
//                diceCheckBox.setWidth(0);
//                diceCheckBox.setTextSize(108);
//                diceCheckBox.setGravity(Gravity.CENTER);
//                diceCheckBox.setButtonDrawable(null);
//                TypedValue typeValue = new TypedValue();
//                getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, typeValue, true);
//                diceCheckBox.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, typeValue.resourceId);
////                diceCheckBox.setClickable(false);
//                diceCheckBox.setText(diceModelObject.getDiceNumberStr());
//                if (diceModelObject.getDiceNumberInt() == 1 || diceModelObject.getDiceNumberInt() == 4) {
//                    diceCheckBox.setTextColor(Color.RED);
//                } else {
//                    diceCheckBox.setTextColor(Color.WHITE);
//                }
//                diceRollRow.addView(diceCheckBox);
//            }
//        }
//    }

    private void rollDiceNewVersion() {
        for (int luckyDiceCheckBoxId : luckyDiceCheckBoxIdList) {
            CheckBox luckyDiceCheckBox = (CheckBox) findViewById(luckyDiceCheckBoxId);
            if (!luckyDiceCheckBox.isChecked()) {
                int diceNumber = new Random().nextInt(6) + 1;
                checkBoxDiceObjMap.get(luckyDiceCheckBoxId).setDiceNumberInt(diceNumber);
                checkBoxDiceObjMap.get(luckyDiceCheckBoxId).setDiceNumberStr(convertIntToDiceStr(diceNumber));
                luckyDiceCheckBox.setText(convertIntToDiceStr(diceNumber));
                if (convertDiceStrToInt(luckyDiceCheckBox.getText().toString()) == 1 || convertDiceStrToInt(luckyDiceCheckBox.getText().toString()) == 4) {
                    luckyDiceCheckBox.setTextColor(Color.RED);
                } else {
                    luckyDiceCheckBox.setTextColor(Color.WHITE);
                }
            }
        }
    }

    private void mapDiceObj() {
//        Log.v("DICE", "in mapDiceObj");
        for (Map.Entry<Integer, DiceModelObject> entry : checkBoxDiceObjMap.entrySet()) {
            if (!entry.getValue().isSelected()) {
                for (DiceModelObject diceModelObject : diceModelObjectList) {
                    if (entry.getValue().getDiceId() == diceModelObject.getDiceId()) {
                        diceModelObject.setDiceNumberInt(entry.getValue().getDiceNumberInt());
                        diceModelObject.setDiceNumberStr(entry.getValue().getDiceNumberStr());
                    }
                }
            }
        }
    }

    private String convertIntToDiceStr(int IntIn) {
        String strOut = "";
        switch (IntIn){
            case 1:
                strOut = getResources().getString(R.string.dice_number_1);
                break;
            case 2:
                strOut = getResources().getString(R.string.dice_number_2);
                break;
            case 3:
                strOut = getResources().getString(R.string.dice_number_3);
                break;
            case 4:
                strOut = getResources().getString(R.string.dice_number_4);
                break;
            case 5:
                strOut = getResources().getString(R.string.dice_number_5);
                break;
            case 6:
                strOut = getResources().getString(R.string.dice_number_6);
                break;
        }
        return strOut;
    }

    private int convertDiceStrToInt(String strIn) {
        int intOut = 0;
        switch (strIn){
            case "⚀":
                intOut = 1;
                break;
            case "⚁":
                intOut = 2;
                break;
            case "⚂":
                intOut = 3;
                break;
            case "⚃":
                intOut = 4;
                break;
            case "⚄":
                intOut = 5;
                break;
            case "⚅":
                intOut = 6;
                break;
        }
        return intOut;
    }

    public void vibration() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }
    }
}
