package com.example.jtestapk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.dialog.ColorPickerDialog;
import com.example.model.NoteModelObject;
import com.example.utils.EncryptDecryptUtil;
import com.example.utils.ImageConvertUtils;
import com.example.utils.PropertiesUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class NotePaintActivity extends AppCompatActivity implements ColorPickerDialog.OnColorChangedListener {

    private Properties appProperties;
    private SharedPreferences sharedPrefMongoDb;

    MyView mv;
    private ConstraintLayout constraintLayout;
    private View seekBarView;
    private int strokeWidth = 15;
    private boolean seekBarViewVisibility;

    private LayoutInflater inflater;
    private View donePopupView;
    private PopupWindow popupWindow;

    private AlertDialog.Builder dialogBuilder;
    private int alphaInputInt = 255;
    private int redInputInt = 0;
    private int greenInputInt = 0;
    private int blueInputInt = 0;

    private static final int COLOR_MENU_ID = 1;
    private static final int COLOR_PICKER_MENU_ID = 11;
    private static final int ARGB_MENU_ID = 12;
    private static final int STROKE_MENU_ID = 2;
    private static final int STYLE_MENU_ID = 3;
    private static final int STYLE_STROKE_MENU_ID = 31;
    private static final int STYLE_FILL_MENU_ID = 32;
    private static final int STYLE_FILL_AND_STROKE_MENU_ID = 33;
    private static final int SPECIAL_MENU_ID = 4;
    private static final int EMBOSS_MENU_ID = 41;
    private static final int BLUR_MENU_ID = 42;
    private static final int ERASE_MENU_ID = 43;
    private static final int SRCATOP_MENU_ID = 44;
    private static final int CLEAR_MENU_ID = 5;
    private static final int CONTEXT_MENU_LOCK = 6;
    private static final int CONTEXT_MENU_LOCK_PIN = 61;
    private static final int CONTEXT_MENU_LOCK_FP = 62;
    private static final int Save = 99;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date noteDate;
    private String noteDateStr;
    private String noteTitle;
    private String noteContent;
    private boolean isEdit;
    private Date editDate;
    private String editDateStr;
    private boolean isFPLock;
    private boolean isPinLock;
    private String encryptedPin;
    private String resultJson;
    private NoteModelObject resultNoteObj;

    private boolean isDrew;

    private App mongoApp;
    private User mongoUser;
    private MongoClient mongoClient;
    private MongoDatabase javaApplicationDB;
    private MongoCollection notesFromAndroidAppCollection;
    private MongoCollection keyCollection;

    private String onlineKey;
    private boolean foundKey;

    Button submitNotesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);

        //get app.properties
        try {
            appProperties = PropertiesUtils.getAppProperties(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        //init sharedPref
        sharedPrefMongoDb = getSharedPreferences("MongoDb", MODE_PRIVATE);

        //init MongoDB Connection
        foundKey = false;
        Realm.init(this);
        String appID = sharedPrefMongoDb.getString("mongoDB.appId", "");
        mongoApp = new App(new AppConfiguration.Builder(appID).build());
        mongoUser = mongoApp.currentUser();
        mongoClient = mongoUser.getMongoClient(sharedPrefMongoDb.getString("mongoDB.client", ""));
        javaApplicationDB = mongoClient.getDatabase(sharedPrefMongoDb.getString("mongoDB.database", ""));
        notesFromAndroidAppCollection = javaApplicationDB.getCollection(sharedPrefMongoDb.getString("mongoDB.collection.notes", ""));
        keyCollection = javaApplicationDB.getCollection(sharedPrefMongoDb.getString("mongoDB.collection.key", ""));
        //get key from server for generate encrypted pin
        Document queryFilter  = new Document();
        RealmResultTask<MongoCursor<Document>> findTask = keyCollection.find(queryFilter).projection(new BsonDocument("_id", new BsonInt32(0))).iterator();
        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                while (results.hasNext()) {
                    String resultJson = results.next().toJson();
                    JsonObject jsonObject = JsonParser.parseString(resultJson).getAsJsonObject();
                    onlineKey = jsonObject.get("key").getAsString();
                    foundKey = true;
                }
                Log.v("EXAMPLE", "successfully found key.");
            } else {
                foundKey = false;
                Log.e("EXAMPLE", "failed to find key");
                Toast.makeText(getApplicationContext(),"Key not found: \r\n" + task.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(),"PIN Lock for notes Disabled", Toast.LENGTH_LONG).show();
            }
        });

        constraintLayout = new ConstraintLayout(this);
        ConstraintLayout.LayoutParams constraintLayoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
        constraintLayout.setLayoutParams(constraintLayoutParams);
        mv= new MyView(this);
        mv.setDrawingCacheEnabled(true);
        constraintLayout.addView(mv);
        setContentView(constraintLayout);

        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View toolsLayout = inflater.inflate(R.layout.notes_tools_layout, constraintLayout, false);
        constraintLayout.addView(toolsLayout);
        donePopupView = inflater.inflate(R.layout.common_pop_window_done, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        popupWindow = new PopupWindow(donePopupView, width, height, true);
        popupWindow.setAnimationStyle(R.style.customDialogInOutAnimation);
        donePopupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (isEdit) {
                    Intent intent = new Intent(NotePaintActivity.this, NotesListViewActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        });

        seekBarView = inflater.inflate(R.layout.common_seekbar, constraintLayout, false);
        seekBarViewVisibility = false;
        seekBarView.setVisibility(View.INVISIBLE);
        constraintLayout.addView(seekBarView);
        TextView commonSeekBarTextView = (TextView) findViewById(R.id.commonSeekBarTextView);
        commonSeekBarTextView.setText(getResources().getString(R.string.display_stroke_width) + strokeWidth);
        SeekBar seekBar = (SeekBar) findViewById(R.id.commonSeekBar);
        seekBar.setProgress(strokeWidth);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                strokeWidth = i;
                commonSeekBarTextView.setText(getResources().getString(R.string.display_stroke_width) + strokeWidth);
                mPaint.setStrokeWidth(strokeWidth);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //get context from before activity
        isEdit = getIntent().getBooleanExtra("isEdit", false);
        resultJson = getIntent().getStringExtra("resultJson");

        //setup layout if isEdit(from Full List View)
        if (isEdit) {
            JsonObject jsonObject = JsonParser.parseString(resultJson).getAsJsonObject();
            resultNoteObj = new Gson().fromJson(jsonObject, NoteModelObject.class);
        }

        //init variable
        isDrew = false;
        isFPLock = false;
        isPinLock = false;
        encryptedPin = "";
        if (isEdit) {
            isFPLock = resultNoteObj.isFPLock();
            isPinLock = resultNoteObj.isPinLock();
            encryptedPin = resultNoteObj.getEncryptedPin();
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFC0C0C0);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(strokeWidth);
        mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 },
                0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);

        Button moreActionNotesButton = (Button) findViewById(R.id.moreActionNotesButton);
        moreActionNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(NotePaintActivity.this, moreActionNotesButton);
                popupMenu.getMenu().add(0, CLEAR_MENU_ID, 0, getResources().getString(R.string.menu_clean));
                SubMenu subMenuColor = popupMenu.getMenu().addSubMenu(Menu.NONE, COLOR_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_color));
                subMenuColor.add(Menu.NONE, COLOR_PICKER_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_color_picker));
                subMenuColor.add(Menu.NONE, ARGB_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_argb_input));
                popupMenu.getMenu().add(0, STROKE_MENU_ID, 0, getResources().getString(R.string.menu_stroke_width));
                SubMenu subMenuLink = popupMenu.getMenu().addSubMenu(Menu.NONE, STYLE_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_stroke_style));
                subMenuLink.add(Menu.NONE, STYLE_STROKE_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_style_stroke));
                subMenuLink.add(Menu.NONE, STYLE_FILL_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_style_fill));
                subMenuLink.add(Menu.NONE, STYLE_FILL_AND_STROKE_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_style_fill_stroke));
                SubMenu subMenuSpecial = popupMenu.getMenu().addSubMenu(Menu.NONE, SPECIAL_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_special));
                subMenuSpecial.add(Menu.NONE, EMBOSS_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_special_emboss));
                subMenuSpecial.add(Menu.NONE, BLUR_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_special_blur));
                subMenuSpecial.add(Menu.NONE, ERASE_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_special_erase));
                subMenuSpecial.add(Menu.NONE, SRCATOP_MENU_ID, Menu.NONE, getResources().getString(R.string.menu_special_srcatop));
                SubMenu subMenuLock = popupMenu.getMenu().addSubMenu(Menu.NONE, CONTEXT_MENU_LOCK, Menu.NONE, "LOCK");
                if (foundKey) {
                    subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_PIN, Menu.NONE, "Lock With PIN");
                } else {
                    subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_PIN, Menu.NONE, "Lock With PIN").setEnabled(false);
                }
                subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_FP, Menu.NONE, "Lock With Fingerprint");
                popupMenu.getMenu().add(0, Save, 0, getResources().getString(R.string.menu_save));
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        return menuItemClickedAction(menuItem);
                    }
                });
                popupMenu.show();
            }
        });
        submitNotesButton = (Button) findViewById(R.id.submitNotesButton);
        if (isEdit) {
            if (resultNoteObj.isPinLock()) {
                submitNotesButton.setText(getResources().getString(R.string.button_save_submit_pin));
            } else if (resultNoteObj.isFPLock()){
                submitNotesButton.setText(getResources().getString(R.string.button_save_submit_fp));
            }
            if (resultNoteObj.isPinLock() && resultNoteObj.isFPLock()) {
                submitNotesButton.setText(getResources().getString(R.string.button_save_submit_pin_fp));
            }
        }
        submitNotesButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                submitNotesButton.setEnabled(false);
                if (isEdit) {
                    if (!isDrew) {
                        Toast.makeText(getApplicationContext(), "No change.", Toast.LENGTH_SHORT).show();
                        submitNotesButton.setEnabled(true);
                        return;
                    }
                    Document queryFilter = new Document("dateStr", resultNoteObj.getDateStr());
                    notesFromAndroidAppCollection.updateOne(queryFilter, setupDocumentForUpload()).getAsync(task -> {
                        if (task.isSuccess()) {
                            Log.v("EXAMPLE", "successfully updated a document.");
                            Toast.makeText(getApplicationContext(),"Note in MongoDB is updated.", Toast.LENGTH_SHORT).show();
                            //show done popup
                            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
                        } else {
                            Log.e("EXAMPLE", "failed to update document with: ", task.getError());
                            Toast.makeText(getApplicationContext(),"Upload Failed: " + task.getError(), Toast.LENGTH_SHORT).show();
                            submitNotesButton.setEnabled(true);
                        }
                    });
                } else {
                    if (!isDrew) {
                        submitNotesButton.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Cannot submit empty paint.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    notesFromAndroidAppCollection.insertOne(setupDocumentForUpload()).getAsync(task -> {
                        if (task.isSuccess()) {
                            Log.v("EXAMPLE", "successfully inserted documents into the collection.");
                            Toast.makeText(getApplicationContext(),"Inserted into MongoDB.", Toast.LENGTH_SHORT).show();
                            //show done popup
                            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
                        } else {
                            Log.e("EXAMPLE", "failed to insert documents with: ", task.getError());
                            Toast.makeText(getApplicationContext(),"Upload Failed: " + task.getError(), Toast.LENGTH_SHORT).show();
                            submitNotesButton.setEnabled(true);
                        }
                    });
                }
            }
        });
        Button switchNotesButton = (Button) findViewById(R.id.switchNotesButton);
        switchNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDrew) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(NotePaintActivity.this);
                    dialogBuilder.setTitle(getResources().getString(R.string.dialog_switch_text_title));
                    dialogBuilder.setMessage(getResources().getString(R.string.dialog_switch_text_msg));
                    dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(NotePaintActivity.this, NotesActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                    dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_negative), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    dialogBuilder.show();
                } else {
                    Intent intent = new Intent(NotePaintActivity.this, NotesActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private Paint mPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;

    public void colorChanged(int color) {
        mPaint.setColor(color);
    }

    public class MyView extends View {

        private static final float MINP = 0.25f;
        private static final float MAXP = 0.75f;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Path mPath;
        private Paint mBitmapPaint;
        Context context;

        public MyView(Context c) {
            super(c);
            context=c;
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            //for Edit Paint
            if (isEdit) {
                Bitmap fromJsonStr = ImageConvertUtils.convertStringToBitmap(resultNoteObj.getContent());
                mBitmap = fromJsonStr.copy(Bitmap.Config.ARGB_8888, true);
            } else {
                mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            }
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            //showDialog();
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;

        }
        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
            //mPaint.setMaskFilter(null);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            isDrew = true;

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:

                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }

    public boolean menuItemClickedAction(MenuItem item) {
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xFF);

        switch (item.getItemId()) {
            case CLEAR_MENU_ID:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                break;
            case COLOR_PICKER_MENU_ID:
                new ColorPickerDialog(this, this, mPaint.getColor()).show();
                break;
            case ARGB_MENU_ID:
                //setup ARGB Dialog
                dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle(getResources().getString(R.string.dialog_argb_title));
                LinearLayout verticaLinearLayout = new LinearLayout(this);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                verticaLinearLayout.setLayoutParams(param);
                verticaLinearLayout.setWeightSum(1f);
                verticaLinearLayout.setPadding(25, 15, 15, 25);
                verticaLinearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout horizontalTextLinearLayout = new LinearLayout(this);
                LinearLayout.LayoutParams inputTextParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f);
                EditText alphaInput = new EditText(this);
                alphaInput.setHint("A:");
                alphaInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                horizontalTextLinearLayout.addView(alphaInput, inputTextParam);
                EditText redInput = new EditText(this);
                redInput.setHint("R:");
                redInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                horizontalTextLinearLayout.addView(redInput, inputTextParam);
                EditText greenInput = new EditText(this);
                greenInput.setHint("G:");
                greenInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                horizontalTextLinearLayout.addView(greenInput, inputTextParam);
                EditText blueInput = new EditText(this);
                blueInput.setHint("B:");
                blueInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                horizontalTextLinearLayout.addView(blueInput, inputTextParam);
                verticaLinearLayout.addView(horizontalTextLinearLayout);
                dialogBuilder.setView(verticaLinearLayout);
                dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!alphaInput.getText().toString().equals("") && !redInput.getText().toString().equals("") &&
                                !greenInput.getText().toString().equals("") && !blueInput.getText().toString().equals("")) {
                            alphaInputInt = Integer.parseInt(alphaInput.getText().toString());
                            redInputInt = Integer.parseInt(redInput.getText().toString());
                            greenInputInt = Integer.parseInt(greenInput.getText().toString());
                            blueInputInt = Integer.parseInt(blueInput.getText().toString());
                            if (alphaInputInt > 255 ||  redInputInt > 255 || greenInputInt > 255 || blueInputInt > 255) {
                                Toast.makeText(getApplicationContext(),"Out of range. (0-255)", Toast.LENGTH_SHORT).show();
                            } else {
                                mPaint.setARGB(alphaInputInt, redInputInt, greenInputInt, blueInputInt);
                            }
                        } else {
                            Toast.makeText(getApplicationContext(),"No empty field allowed.", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                });
                dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_negative), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialogBuilder.show();
                break;
            case STROKE_MENU_ID:
                if (seekBarViewVisibility) {
                    seekBarViewVisibility = false;
                    seekBarView.setVisibility(View.INVISIBLE);
                } else {
                    seekBarViewVisibility = true;
                    seekBarView.setVisibility(View.VISIBLE);
                }
                break;
            case STYLE_STROKE_MENU_ID:
                mPaint.setStyle(Paint.Style.STROKE);
                break;
            case STYLE_FILL_MENU_ID:
                mPaint.setStyle(Paint.Style.FILL);
                break;
            case STYLE_FILL_AND_STROKE_MENU_ID:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
            case EMBOSS_MENU_ID:
                if (mPaint.getMaskFilter() != mEmboss) {
                    mPaint.setMaskFilter(mEmboss);
                } else {
                    mPaint.setMaskFilter(null);
                }
                break;
            case BLUR_MENU_ID:
                if (mPaint.getMaskFilter() != mBlur) {
                    mPaint.setMaskFilter(mBlur);
                } else {
                    mPaint.setMaskFilter(null);
                }
                break;
            case ERASE_MENU_ID:
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                mPaint.setAlpha(0x80);
                break;
            case SRCATOP_MENU_ID:
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                mPaint.setAlpha(0x80);
                break;
            case CONTEXT_MENU_LOCK_PIN: {
                if (!foundKey) {
                    Toast.makeText(getApplicationContext(),"Failed to found key on server, cannot use this method now.", Toast.LENGTH_LONG).show();
                } else {
                    if (isPinLock) {
                        isPinLock = false;
                        encryptedPin = "";
                        submitNotesButton.setText(getResources().getString(R.string.button_save_submit));
                        Toast.makeText(getApplicationContext(),"Note UNLOCKED, click again to lock.", Toast.LENGTH_LONG).show();
                    } else {
                        isPinLock = true;
                        setupPinAlertDialog();
                    }
                    Toast.makeText(getApplicationContext(),"CONTEXT_MENU_LOCK_PIN", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case CONTEXT_MENU_LOCK_FP: {
                if (isFPLock) {
                    isFPLock = false;
                    if (isPinLock) {
                        submitNotesButton.setText(getResources().getString(R.string.button_save_submit_pin));
                    } else {
                        submitNotesButton.setText(getResources().getString(R.string.button_save_submit));
                    }
                    Toast.makeText(getApplicationContext(),"Note UNLOCKED, click again to lock.", Toast.LENGTH_LONG).show();
                } else {
                    isFPLock = true;
                    if (isPinLock) {
                        submitNotesButton.setText("[\uD83D\uDD12+\uD83D\uDD11] Save & submit");
                    } else {
                        submitNotesButton.setText("[\uD83D\uDD12] Save & submit");
                    }
                    Toast.makeText(getApplicationContext(),"Note LOCKED, click again to unlock.", Toast.LENGTH_LONG).show();
                }
            }
            break;
            case Save:
                AlertDialog.Builder editalert = new AlertDialog.Builder(NotePaintActivity.this);
                editalert.setTitle(getResources().getString(R.string.dialog_save_title));
                final EditText input = new EditText(NotePaintActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                editalert.setView(input);
                editalert.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String name= input.getText().toString();
                        Bitmap bitmap = mv.getDrawingCache();

                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/"+name+".png");
                        try
                        {
                            if(!file.exists())
                            {
                                file.createNewFile();
                            }
                            FileOutputStream ostream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 10, ostream);
                            ostream.close();
                            mv.invalidate();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }finally
                        {

                            mv.setDrawingCacheEnabled(false);
                        }
                    }
                });
                editalert.show();
                break;
        }
        return true;
    }

    private void setupPinAlertDialog() {
        View NotePinInputLayout = inflater.inflate(R.layout.note_pin_input, null);
        TextView notePinMsg = (TextView) NotePinInputLayout.findViewById(R.id.notePinMsg);
        TextInputEditText notePinInput = (TextInputEditText) NotePinInputLayout.findViewById(R.id.notePinInput);
        notePinInput.requestFocus();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(NotePinInputLayout)
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String input = notePinInput.getText().toString();
                        if (input == null || input.isEmpty()) {
                            notePinInput.setBackgroundResource(R.drawable.textinput_surface_red);
                            Toast.makeText(getApplicationContext(),"input PIN must not null", Toast.LENGTH_LONG).show();
                        } else {
                            try{
                                byte[] key = EncryptDecryptUtil.generateKey(onlineKey);
                                encryptedPin = EncryptDecryptUtil.encrypt(key, input);
                            } catch(Exception e) {
                                isPinLock = false;
                                encryptedPin = "";
                                Toast.makeText(getApplicationContext(),"Error when encrypting.", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                            if (isFPLock) {
                                submitNotesButton.setText(getResources().getString(R.string.button_save_submit_pin_fp));
                            } else {
                                submitNotesButton.setText(getResources().getString(R.string.button_save_submit_pin));
                            }
                            isPinLock = true;
                            Toast.makeText(getApplicationContext(),"PIN SET, click again to unlock.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    }
                });
                Button negativeButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        isPinLock = false;
                        encryptedPin = "";
                        dialog.cancel();
                    }
                });
            }
        });
        notePinInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notePinMsg.setText("Input you pin below:");
                notePinInput.setBackgroundResource(0);
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString() == null || charSequence.toString().isEmpty()) {
                    notePinMsg.setText("Input pin must not null");
                    notePinInput.setBackgroundResource(R.drawable.textinput_surface_red);
                    return;
                } else if (charSequence.toString().length() < 6) {
                    notePinMsg.setText("Recommended: \r\nSet a pin greater than or equal to 6 digits");
                    notePinInput.setBackgroundResource(R.drawable.textinput_surface_yellow);
                    return;
                } else if (charSequence.toString().length() >= 6) {
                    notePinMsg.setText("Press YES to confirm the pin. \r\nYou can change it whenever you want.");
                    notePinInput.setBackgroundResource(0);
                    return;
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        dialog.show();
    }

    private Document setupDocumentForUpload() {
        Date date = new Date();
        Document doc = new Document();
        noteTitle = "Paint-" + noteDateStr;
        //setup content
        mv.buildDrawingCache(true);
        Bitmap bitmap = mv.getDrawingCache(true).copy(Bitmap.Config.RGB_565, false);
        mv.destroyDrawingCache();
        try {
            mv.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mv.setDrawingCacheEnabled(false);
        }
        doc.append("isFPLock", isFPLock);
        doc.append("isPinLock", isPinLock);
        doc.append("encryptedPin", encryptedPin);
        noteContent = ImageConvertUtils.convertBitmapToString(bitmap);
        if (isEdit) {
            noteDate = resultNoteObj.getDate().getDate();
            noteDateStr = resultNoteObj.getDateStr();
            editDate = date;
            editDateStr = dateFormat.format(date);
            doc.append("dateStr", noteDateStr);
            doc.append("date", noteDate);
            doc.append("isPaint", true);
            doc.append("editDateStr", editDateStr);
            doc.append("editDate", editDate);
        } else {
            noteDate = date;
            noteDateStr = dateFormat.format(date);

            doc.append("dateStr", noteDateStr);
            doc.append("date", noteDate);
            doc.append("isPaint", true);
            doc.append("editDateStr", "");
            doc.append("editDate", null);
        }
        doc.append("title", noteTitle);
        doc.append("content", noteContent);
        return doc;
    }

    //For back pressed twice to exit
    private static final int TIME_INTERVAL = 2000;
    private boolean doubleBackToExitPressedOnce;
    private Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doubleBackToExitPressedOnce = false;
        }
    };
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            if (isEdit) {
                Intent intent = new Intent(NotePaintActivity.this, NotesListViewActivity.class);
                startActivity(intent);
                finish();
            } else {
                super.onBackPressed();
                return;
            }
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(mRunnable, TIME_INTERVAL);
    }
}
