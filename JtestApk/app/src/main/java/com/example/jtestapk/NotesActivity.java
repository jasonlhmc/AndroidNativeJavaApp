package com.example.jtestapk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.model.NoteModelObject;
import com.example.model.NoteTaskModelObject;
import com.example.model.NoteTaskObject;
import com.example.utils.EncryptDecryptUtil;
import com.example.utils.PropertiesUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

public class NotesActivity extends AppCompatActivity {

    private Properties appProperties;
    private SharedPreferences sharedPrefMongoDb;

    private static final int CONTEXT_MENU_ADD_TASK = 1;
    private static final int CONTEXT_MENU_ADD_TASK_TEST = 99;
    private static final int CONTEXT_MENU_EMBED = 2;
    private static final int CONTEXT_MENU_EMBED_PAINTS = 21;
    private static final int CONTEXT_MENU_LOCK = 3;
    private static final int CONTEXT_MENU_LOCK_PIN = 31;
    private static final int CONTEXT_MENU_LOCK_FP = 32;
    private static final int CONTEXT_MENU_LOCK_OPTION = 33;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private List<NoteModelObject> noteModelObjectList;
    private NoteModelObject noteModelObject;
    private Date noteDate;
    private String noteDateStr;
    private String noteTitle;
    private String noteContent;
//    private boolean isPaint;
    private Date editDate;
    private String editDateStr;
    private boolean isEdit;
    private boolean isFPLock;
    private boolean isPinLock;
    private String encryptedPin;
    private String resultJson;
    private NoteModelObject resultNoteObj;

    private List<NoteTaskModelObject> noteTaskModelObjectList;

    private TextInputEditText notesTitleTextInput;
    private TextInputEditText notesContentInput;
    private Button submitNotesButton;
    private TableLayout noteTable;
    private View donePopupView;
    private PopupWindow popupWindow;

    private App mongoApp;
    private User mongoUser;
    private MongoClient mongoClient;
    private MongoDatabase javaApplicationDB;
    private MongoCollection notesFromAndroidAppCollection;
    private MongoCollection keyCollection;

    private String onlineKey;
    private boolean foundKey;

    private LayoutInflater inflater;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_notes);

        //Resize when keyboard shows up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

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

        //init inflater
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
                    Toast.makeText(getApplicationContext(),"Key found on server, PIN lock available now.", Toast.LENGTH_LONG).show();
                }
                Log.v("EXAMPLE", "successfully found key.");
            } else {
                foundKey = false;
                Log.e("EXAMPLE", "failed to find key");
                Toast.makeText(getApplicationContext(),"Key not found: \r\n" + task.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(),"PIN Lock for notes Disabled", Toast.LENGTH_LONG).show();
            }
        });

        //init layout
        noteTable = (TableLayout) findViewById(R.id.noteTable);
        notesTitleTextInput = (TextInputEditText) findViewById(R.id.notesTitleInput);
        noteTitle = notesTitleTextInput.getText().toString();
        notesContentInput = (TextInputEditText) findViewById(R.id.notesContentInput);
        noteContent = notesContentInput.getText().toString();
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
                    Intent intent = new Intent(NotesActivity.this, NotesListViewActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        });

        //get context from before activity
        isEdit = getIntent().getBooleanExtra("isEdit", false);
        resultJson = getIntent().getStringExtra("resultJson");

        //setup layout if isEdit(from Full List View)
        noteTaskModelObjectList = new ArrayList<NoteTaskModelObject>();
        if (isEdit) {
            JsonObject jsonObject = JsonParser.parseString(resultJson).getAsJsonObject();
            resultNoteObj = new Gson().fromJson(jsonObject, NoteModelObject.class);
            notesTitleTextInput.setText(resultNoteObj.getTitle());
            notesContentInput.setText(resultNoteObj.getContent());
            if (resultNoteObj.isTask()) {
                for (NoteTaskObject noteTaskObject : resultNoteObj.getTaskList()) {
                    setupTaskTable(noteTaskObject);
                }
            }
        }

        //init variable
        isFPLock = false;
        isPinLock = false;
        encryptedPin = "";
        if (isEdit) {
            isFPLock = resultNoteObj.isFPLock();
            isPinLock = resultNoteObj.isPinLock();
            encryptedPin = resultNoteObj.getEncryptedPin();
        }

        //TODO
        //setup layout button listener
        Button moreActionNotesButton = (Button) findViewById(R.id.moreActionNotesButton);
        moreActionNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(NotesActivity.this, moreActionNotesButton);
                popupMenu.getMenu().add(Menu.NONE, CONTEXT_MENU_ADD_TASK, Menu.NONE, "Add Task");
//                SubMenu subMenuEmbed = popupMenu.getMenu().addSubMenu(Menu.NONE, CONTEXT_MENU_EMBED, Menu.NONE, "Embed");
//                subMenuEmbed.add(Menu.NONE, CONTEXT_MENU_EMBED_PAINTS, Menu.NONE, "Embed a Paint");
                SubMenu subMenuLock = popupMenu.getMenu().addSubMenu(Menu.NONE, CONTEXT_MENU_LOCK, Menu.NONE, "Lock");
                if (foundKey) {
                    subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_PIN, Menu.NONE, "Lock With PIN");
                } else {
                    subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_PIN, Menu.NONE, "Lock With PIN").setEnabled(false);
                }
                subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_FP, Menu.NONE, "Lock With Fingerprint");
//                subMenuLock.add(Menu.NONE, CONTEXT_MENU_LOCK_OPTION, Menu.NONE, "Lock Option");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case CONTEXT_MENU_ADD_TASK: {
                                setupTaskTable(null);   // add new
                                Toast.makeText(getApplicationContext(),"CONTEXT_MENU_ADD_TASK", Toast.LENGTH_SHORT).show();
                            }
                            break;
                            case CONTEXT_MENU_EMBED_PAINTS: {
                                Toast.makeText(getApplicationContext(),"CONTEXT_MENU_EMBED_PAINTS", Toast.LENGTH_SHORT).show();
                            }
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
                                        submitNotesButton.setText(getResources().getString(R.string.button_save_submit_pin_fp));
                                    } else {
                                        submitNotesButton.setText(getResources().getString(R.string.button_save_submit_fp));
                                    }
                                    Toast.makeText(getApplicationContext(),"Note LOCKED, click again to unlock.", Toast.LENGTH_LONG).show();
                                }
                            }
                            break;
                            case CONTEXT_MENU_LOCK_OPTION: {

                            }
                            break;
                        }
                        return true;
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
                if (!isTitleOrContentEmpty()) {
                    if (isEdit) {
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
                } else {
                    submitNotesButton.setEnabled(true);
                    Toast.makeText(getApplicationContext(),"Empty Title or Content.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button switchNotesButton = (Button) findViewById(R.id.switchNotesButton);
        switchNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isTitleContentEmpty()) {
                    Intent intent = new Intent(NotesActivity.this, NotePaintActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(NotesActivity.this);
                    dialogBuilder.setTitle(getResources().getString(R.string.dialog_switch_paint_title));
                    dialogBuilder.setMessage(getResources().getString(R.string.dialog_switch_paint_msg));
                    dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(NotesActivity.this, NotePaintActivity.class);
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
                }
            }
        });
    }

    private View.OnClickListener taskCancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            for (NoteTaskModelObject noteTaskModelObject : noteTaskModelObjectList) {
                if (noteTaskModelObject.getCancelButtonId() == view.getId()) {
                    TableRow cancelRow = (TableRow) findViewById(noteTaskModelObject.getRowId());
                    noteTable.removeView(cancelRow);
                    break;
                }
            }
        }
    };

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
        notesTitleTextInput = (TextInputEditText) findViewById(R.id.notesTitleInput);
        noteTitle = notesTitleTextInput.getText().toString();
        notesContentInput = (TextInputEditText) findViewById(R.id.notesContentInput);
        noteContent = notesContentInput.getText().toString();

        Date date = new Date();
        Document doc = new Document();
        doc.append("title", noteTitle);
        doc.append("content", noteContent);
        doc.append("isPaint", false);
        doc.append("isFPLock", isFPLock);
        doc.append("isPinLock", isPinLock);
        doc.append("encryptedPin", encryptedPin);

        if (noteTaskModelObjectList!= null && !noteTaskModelObjectList.isEmpty()) {
            doc.append("isTask", true);
            List<Document> documents = new ArrayList<>();
            for (NoteTaskModelObject noteTaskModelObject : noteTaskModelObjectList) {
                Document task = new Document();
                TextInputEditText taskInput = (TextInputEditText) findViewById(noteTaskModelObject.getTaskInputId());
                String taskInputStr = taskInput.getText().toString();
                ToggleButton toggleButton = findViewById(noteTaskModelObject.getToggleButtonId());
                if (toggleButton.isChecked()) {
                    task.append("isFinished", true);
                } else {
                    task.append("isFinished", false);
                }
                task.append("task", taskInputStr);
                documents.add(task);
            }
            doc.append("taskList", documents);
//            Log.v("MONGODB", "documents = " + documents.toString());
        }

        if (isEdit) {
            editDate = date;
            editDateStr = dateFormat.format(date);
            noteTitle = notesTitleTextInput.getText().toString();
            noteContent = notesContentInput.getText().toString();

            doc.append("editDateStr", editDateStr);
            doc.append("editDate", editDate);

            doc.append("dateStr", resultNoteObj.getDateStr());
            doc.append("date", resultNoteObj.getDate().getDate());
        } else {
            noteDate = date;
            noteDateStr = dateFormat.format(date);
            noteTitle = notesTitleTextInput.getText().toString();
            noteContent = notesContentInput.getText().toString();

            doc.append("dateStr", noteDateStr);
            doc.append("date", noteDate);

            doc.append("editDateStr", "");
            doc.append("editDate", null);

        }
        return doc;
    }

    //For switch activity
    private boolean isTitleContentEmpty() {
        boolean rs = false;
        noteTitle = notesTitleTextInput.getText().toString();
        noteContent = notesContentInput.getText().toString();
        if ((noteTitle == null || noteTitle.isEmpty()) &&
                (noteContent == null || noteContent.isEmpty())) {
            rs = true;
        }
        return rs;
    }

    //for upload submit
    private boolean isTitleOrContentEmpty() {
        boolean rs = false;
        noteTitle = notesTitleTextInput.getText().toString();
        noteContent = notesContentInput.getText().toString();
        if ((noteTitle == null || noteTitle.isEmpty()) ||
                (noteContent == null || noteContent.isEmpty())) {
            rs = true;
        }
        return rs;
    }

    private void setupTaskTable(NoteTaskObject noteTaskObject) {
        TableRow noteTaskRow = (TableRow) inflater.inflate(R.layout.note_task_row, noteTable, false);
        NoteTaskModelObject noteTaskModelObject = new NoteTaskModelObject();
        int rowId = View.generateViewId();
        int toggleButtonId = View.generateViewId();
        int taskInputId = View.generateViewId();
        int cancelButtonId = View.generateViewId();
        noteTaskModelObject.setRowId(rowId);
        noteTaskModelObject.setToggleButtonId(toggleButtonId);
        noteTaskModelObject.setTaskInputId(taskInputId);
        noteTaskModelObject.setCancelButtonId(cancelButtonId);
        noteTaskModelObjectList.add(noteTaskModelObject);
        noteTaskRow.setId(rowId);
        noteTable.addView(noteTaskRow);
        int childParts = noteTaskRow.getChildCount();
        if (noteTaskRow != null) {
            for (int x = 0; x < childParts; x++) {
                View viewChild = noteTaskRow.getChildAt(x);
                if (viewChild instanceof ToggleButton) {
                    viewChild.setId(toggleButtonId);
                    if (noteTaskObject != null) {
                        ((ToggleButton) viewChild).setChecked(noteTaskObject.isFinished());
                    }
                } else if (viewChild instanceof Button) {
                    viewChild.setId(cancelButtonId);
                    ((Button) viewChild).setOnClickListener(taskCancelButtonListener);//TODO
                } else if (viewChild instanceof TextInputLayout) {
                    TextInputEditText textInputLayoutChild = (TextInputEditText) ((TextInputLayout) viewChild).getEditText();
                    textInputLayoutChild.setId(taskInputId);
                    textInputLayoutChild.requestFocus();
                    if (noteTaskObject != null) {
                        textInputLayoutChild.setText(noteTaskObject.getTask());
                    }
                }
            }
        }
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
                Intent intent = new Intent(NotesActivity.this, NotesListViewActivity.class);
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
