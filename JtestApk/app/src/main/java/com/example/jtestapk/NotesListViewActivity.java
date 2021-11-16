package com.example.jtestapk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.adapter.NotesFullListGridAdapter;
import com.example.model.NoteModelObject;
import com.example.utils.BlurUtils;
import com.example.utils.CustomAnimationUtils;
import com.example.utils.EncryptDecryptUtil;
import com.example.utils.ImageConvertUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class NotesListViewActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefMongoDb;
    private SharedPreferences sharedPrefFirebase;

    private App mongoApp;
    private User mongoUser;
    private MongoClient mongoClient;
    private MongoDatabase javaApplicationDB;
    private MongoCollection notesFromAndroidAppCollection;
    private MongoCollection keyCollection;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference dataBaseRef;

    private List<NoteModelObject> noteModelObjectList;
    private List<NoteModelObject> noteSearchList;

    private NotesFullListGridAdapter notesFullListGridAdapter;
    private FloatingActionButton commonFab, notesFloatSearch, notesFloatNew;
    boolean subFabVisible;
    private TextView textFabSearch, textFabNew;
    private TextInputLayout notesFullListSearch;
    private TextInputEditText notesSearchInput;
    private FloatingActionButton notesFloatFirebase;
    private TextView textFabFirebase;

//    private List<String> resultJsonList;
    private Map<NoteModelObject, String> rsObjMap;
    private NoteModelObject selectedNote;
    private ProgressBar commonProgressBar;
    private static final int DELETE_ACTION_CONTEXT_MENU = 1;
    private String deleteQueryKey;

    private String onlineKey;
    private boolean foundKey;

    private boolean isPinLock;
    private boolean pinLockPass;
    private boolean isFPAvailable;
    private boolean isFPLock;
    private BiometricPrompt biometricPrompt;
    private boolean isDeleteNoteEvent;

    BlurUtils blurUtils = new BlurUtils();
    CustomAnimationUtils customAnimationUtils = new CustomAnimationUtils();

    private LayoutInflater inflater;

    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);

        //init variable
        isPinLock = false;
        pinLockPass = false;
        isFPLock = false;

        //init inflater
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //Finger print authentication
        BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                isFPAvailable = true;
//                Toast.makeText(getApplicationContext(), "BIOMETRIC_SUCCESS", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                isFPAvailable = false;
                Toast.makeText(getApplicationContext(), "Cannot init BiometricManager: BIOMETRIC_ERROR_NO_HARDWARE", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                isFPAvailable = false;
                Toast.makeText(getApplicationContext(), "Cannot init BiometricManager: BIOMETRIC_ERROR_HW_UNAVAILABLE", Toast.LENGTH_SHORT).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                isFPAvailable = false;
                Toast.makeText(getApplicationContext(), "Cannot init BiometricManager: BIOMETRIC_ERROR_NONE_ENROLLED", Toast.LENGTH_SHORT).show();
                break;
        }
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(NotesListViewActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (isDeleteNoteEvent) {
                    deleteNoteConfirmationDialog();
                } else {
                    switchActivityForSelectedNote();
                }
            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        //init sharedPref
        sharedPrefMongoDb = getSharedPreferences("MongoDb", MODE_PRIVATE);
        //init MongoDB Connection
        Realm.init(this);
        String appID = sharedPrefMongoDb.getString("mongoDB.appId", "");
        mongoApp = new App(new AppConfiguration.Builder(appID).build());
        mongoUser = mongoApp.currentUser();
        mongoClient = mongoUser.getMongoClient(sharedPrefMongoDb.getString("mongoDB.client", ""));
        javaApplicationDB = mongoClient.getDatabase(sharedPrefMongoDb.getString("mongoDB.database", ""));
        notesFromAndroidAppCollection = javaApplicationDB.getCollection(sharedPrefMongoDb.getString("mongoDB.collection.notes", ""));
        keyCollection = javaApplicationDB.getCollection(sharedPrefMongoDb.getString("mongoDB.collection.key", ""));
        //Get all notes from MongoDB
        noteModelObjectList = new ArrayList<NoteModelObject>();
//        resultJsonList = new ArrayList<String>();
        rsObjMap = new HashMap<NoteModelObject, String>();
        Document queryFilter = new Document();
        RealmResultTask<MongoCursor<Document>> findTask = notesFromAndroidAppCollection.find(queryFilter).projection(new BsonDocument("_id", new BsonInt32(0))).iterator();
        findTask.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                while (results.hasNext()) {
                    String resultJson = results.next().toJson();
                    try {
                        //convert to java obj using Gson
                        JsonObject jsonObject = JsonParser.parseString(resultJson).getAsJsonObject();
                        NoteModelObject convertedObject = new Gson().fromJson(jsonObject, NoteModelObject.class);
                        noteModelObjectList.add(convertedObject);
//                        resultJsonList.add(resultJson);
                        rsObjMap.put(convertedObject, resultJson);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
//                noteModelObjectList.stream().forEach(System.out::println);
                Collections.sort(noteModelObjectList, Comparator.comparing(obj -> obj.getDate().getDate()));
//                noteModelObjectList.stream().forEach(System.out::println);
                Collections.reverse(noteModelObjectList);
//                Collections.reverse(resultJsonList);
//                noteModelObjectList.stream().forEach(System.out::println);
                //init GridView
                setupGridView(null);
                //hide loading progressBar
                commonProgressBar.setVisibility(View.INVISIBLE);
                //enable firebase fab
                if (sharedPrefFirebase.getBoolean("isCompleted", false) && sharedPrefFirebase.getBoolean("isAuthenticated", false)) {
                    notesFloatFirebase.setEnabled(true);
                }
            } else {
                Log.e("EXAMPLE", "failed to find document with: ", task.getError());
                finish();
                Toast.makeText(getApplicationContext(),"Failed to find data: \r\n" + task.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });
        //get key from server for generate encrypted pin
        Document keyFilter = new Document();
        RealmResultTask<MongoCursor<Document>> findKeyTask = keyCollection.find(keyFilter).projection(new BsonDocument("_id", new BsonInt32(0))).iterator();
        findKeyTask.getAsync(task -> {
            if (task.isSuccess()) {
                MongoCursor<Document> results = task.get();
                while (results.hasNext()) {
                    String resultJson = results.next().toJson();
                    JsonObject jsonObject = JsonParser.parseString(resultJson).getAsJsonObject();
                    onlineKey = jsonObject.get("key").getAsString();
                    foundKey = true;
                    Toast.makeText(getApplicationContext(),"Key found on server, PIN unlock available now.", Toast.LENGTH_LONG).show();
                }
                Log.v("EXAMPLE", "successfully found key.");
            } else {
                foundKey = false;
                Log.e("EXAMPLE", "failed to find key");
                finish();
                Toast.makeText(getApplicationContext(),"Key not found: \r\n" + task.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });

        //test Firebase
        sharedPrefFirebase = getSharedPreferences("firebase", MODE_PRIVATE);
        if (!sharedPrefFirebase.getBoolean("isCompleted", false)) {
            Toast.makeText(getApplicationContext(),"firebase setting not found", Toast.LENGTH_LONG).show();
        } else {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId(sharedPrefFirebase.getString("settingFirebaseProjectId", ""))
                    .setApplicationId(sharedPrefFirebase.getString("settingFirebaseAppId", ""))
                    .setApiKey(sharedPrefFirebase.getString("settingFirebaseApiKey", ""))
                    .build();
            FirebaseApp firebaseApp = null;
            boolean initialized = false;
            List<FirebaseApp> firebaseAppList = FirebaseApp.getApps(this);
            for(FirebaseApp app : firebaseAppList){
                if(app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)){
                    initialized=true;
                    firebaseApp = app;
                }
            }
            if(!initialized) {
                firebaseApp = FirebaseApp.initializeApp(this, options);
            }
            //firebase auth
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("FIREBASE", "signInAnonymously:success");
                        sharedPrefFirebase.edit().putBoolean("isAuthenticated", true).apply();
                        database = FirebaseDatabase.getInstance(sharedPrefFirebase.getString("settingFirebaseInstanceUrl", ""));
                        dataBaseRef = database.getReference();
                        notesFloatFirebase.setEnabled(true);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("FIREBASE", "signInAnonymously:failure", task.getException());
                        sharedPrefFirebase.edit().putBoolean("isAuthenticated", false).apply();
                    }
                }
            });
        }

        setContentView(R.layout.activity_notes_full);

        //set loading screen
        commonProgressBar = (ProgressBar) findViewById(R.id.commonProgressBar);
        notesFullListSearch = (TextInputLayout) findViewById(R.id.notesFullListSearch);
        notesSearchInput = (TextInputEditText) findViewById(R.id.notesSearchInput);
        notesSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                noteSearchList = new ArrayList<NoteModelObject>();
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                for (NoteModelObject noteModelObject : noteModelObjectList) {
                    if (noteModelObject.getTitle().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT))) {
                        noteSearchList.add(noteModelObject);
                    }
                }
                Log.v("SEARCH", "noteSearchList.size() = " + noteSearchList.size());
                setupGridView(noteSearchList);
            }
            @Override
            public void afterTextChanged(Editable editable) {
                noteSearchList = new ArrayList<NoteModelObject>();
            }
        });
        //setup Floating action button
        subFabVisible = false;
        textFabSearch = (TextView) findViewById(R.id.textFabSearch);
        textFabNew = (TextView) findViewById(R.id.textFabNew);
        commonFab = (FloatingActionButton) findViewById(R.id.commonFab);
        notesFloatSearch = (FloatingActionButton) findViewById(R.id.notesFloatSearch);
        notesFloatNew = (FloatingActionButton) findViewById(R.id.notesFloatNew);
        //setup sync button
        notesFloatFirebase = (FloatingActionButton) findViewById(R.id.notesFloatFirebase);
        textFabFirebase = (TextView) findViewById(R.id.textFabFirebase);
        notesFloatFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dataBaseRef == null) {

                } else {
                    for (NoteModelObject noteModelObject : noteModelObjectList) {
                        NoteModelObject firebaseObject = new NoteModelObject(
                                noteModelObject.getContent(),
                                noteModelObject.getDateStr(),
                                noteModelObject.getEditDateStr(),
                                noteModelObject.getTitle(),
                                noteModelObject.isPaint(),
                                noteModelObject.isFPLock(),
                                noteModelObject.isPinLock(),
                                noteModelObject.getEncryptedPin(),
                                noteModelObject.isTask(),
                                noteModelObject.getTaskList()

                        );
                        dataBaseRef.child(sharedPrefFirebase.getString("settingChildNotes", "")).child(noteModelObject.getDateStr()).setValue(firebaseObject);
                    }
                    dataBaseRef.child(sharedPrefFirebase.getString("settingChildKey", "")).child("key").setValue(onlineKey);
                }
            }
        });
        commonFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!subFabVisible) {
                    notesFloatSearch.show();
                    notesFloatNew.show();
                    notesFloatFirebase.show();
                    textFabSearch.setVisibility(View.VISIBLE);
                    textFabNew.setVisibility(View.VISIBLE);
                    textFabFirebase.setVisibility(View.VISIBLE);
                    subFabVisible = true;
                } else {
                    notesFloatSearch.hide();
                    notesFloatNew.hide();
                    notesFloatFirebase.hide();
                    textFabSearch.setVisibility(View.GONE);
                    textFabNew.setVisibility(View.GONE);
                    textFabFirebase.setVisibility(View.GONE);
                    subFabVisible = false;
                }
            }
        });
        notesFloatSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (notesFullListSearch.getVisibility() == View.VISIBLE) {
                    notesFullListSearch.setAnimation(customAnimationUtils.fadeInAnimationDefault(getApplicationContext()));
                    notesFullListSearch.setVisibility(View.GONE);
                } else {
                    notesFullListSearch.setAnimation(customAnimationUtils.fadeInAnimationDefault(getApplicationContext()));
                    notesFullListSearch.setVisibility(View.VISIBLE);
                }
                commonFab.performClick();
            }
        });
        notesFloatNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NotesListViewActivity.this, NotesActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    //fingerprint prompt Info
    private BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint")
            .setDescription("This note is LOCKED. \r\nPlease using your biometric credential to continue.")
            .setNegativeButtonText("Cancel").build();

    private void setupGridView(List<NoteModelObject> filterList) {
        //setup custom Adapter
        if (filterList == null || filterList.isEmpty()) {
            for (NoteModelObject noteModelObject : noteModelObjectList) {
                if (noteModelObject.isPaint()) {
                    Bitmap paint = ImageConvertUtils.convertStringToBitmap(noteModelObject.getContent());
                    if (noteModelObject.isPinLock() || noteModelObject.isFPLock()) {
                        noteModelObject.setGridViewPaint(blurUtils.fastBlur(paint, 0.75f, 50));
                    } else {
                        noteModelObject.setGridViewPaint(paint);
                    }
                }
            }
            notesFullListGridAdapter = new NotesFullListGridAdapter(getApplicationContext(), noteModelObjectList);
        } else {
            for (NoteModelObject noteModelObject : filterList) {
                if (noteModelObject.isPaint()) {
                    Bitmap paint = ImageConvertUtils.convertStringToBitmap(noteModelObject.getContent());
                    if (noteModelObject.isPinLock() || noteModelObject.isFPLock()) {
                        noteModelObject.setGridViewPaint(blurUtils.fastBlur(paint, 0.75f, 50));
                    } else {
                        noteModelObject.setGridViewPaint(paint);
                    }
                }
            }
            notesFullListGridAdapter = new NotesFullListGridAdapter(getApplicationContext(), filterList);
        }

        //setup GridView
        GridView notesFullListGrid = (GridView) findViewById(R.id.notesFullListGrid);
        notesFullListGrid.setAdapter(notesFullListGridAdapter);
        notesFullListGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                isDeleteNoteEvent = false;
                selectedNote = noteModelObjectList.get(i);
                isPinLock = selectedNote.isPinLock();
                isFPLock = selectedNote.isFPLock();
                noteAccessValidation();
            }
        });
        notesFullListGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedNote = noteModelObjectList.get(i);
                isPinLock = selectedNote.isPinLock();
                isFPLock = selectedNote.isFPLock();
                deleteQueryKey = noteModelObjectList.get(i).getDateStr();
//                view.setBackgroundColor(Color.GRAY);
                PopupMenu popupMenu = new PopupMenu(NotesListViewActivity.this, view);
                popupMenu.getMenu().add(0, DELETE_ACTION_CONTEXT_MENU, 0, getResources().getString(R.string.grid_menu_delete));
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        return menuItemClickedAction(menuItem);
                    }
                });
                popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu popupMenu) {
//                        view.setBackgroundColor(Color.DKGRAY);
                    }
                });
                popupMenu.show();
                return true;
            }
        });
    }

    private void restartRefresh() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private boolean menuItemClickedAction(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ACTION_CONTEXT_MENU:
                isDeleteNoteEvent = true;
                noteAccessValidation();
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
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String decryptedPin = "";
                        try {
                            byte[] key = EncryptDecryptUtil.generateKey(onlineKey);
                            decryptedPin = EncryptDecryptUtil.decrypt(key, selectedNote.getEncryptedPin());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(),"Error when decrypting.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                        String input = notePinInput.getText().toString();
                        if (decryptedPin.equals(input)) {
                            Toast.makeText(getApplicationContext(),"PIN lock pass.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
//                            switchActivityForSelectedNote();
                            if (!isFPLock) {
                                if (isDeleteNoteEvent) {
                                    deleteNoteConfirmationDialog();
                                } else {
                                    switchActivityForSelectedNote();
                                }
                            } else {
                                biometricPrompt.authenticate(promptInfo);
                            }
                        } else {
                            notePinMsg.setText("!!WRONG PIN!!");
                            notePinInput.setBackgroundResource(R.drawable.textinput_surface_red);
                        }
                    }
                });
                Button negativeButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.cancel();
                    }
                });
            }
        });
        dialog.show();
    }

    private void deleteNoteConfirmationDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(NotesListViewActivity.this);
        dialogBuilder.setTitle(getResources().getString(R.string.dialog_delete_title));
        dialogBuilder.setMessage(getResources().getString(R.string.dialog_delete_msg));
        dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Document queryFilter = new Document("dateStr", deleteQueryKey);
                notesFromAndroidAppCollection.deleteOne(queryFilter).getAsync(task -> {
                    if (task.isSuccess()) {
                        Log.v("EXAMPLE", "successfully deleted a document.");
                        Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        restartRefresh();
                    } else {
                        Log.e("EXAMPLE", "failed to delete document with: ", task.getError());
                    }
                });
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

    private void noteAccessValidation() {
        if (isFPLock && isPinLock) {
            Toast.makeText(getApplicationContext(), "PIN and FINGERPRINT are needed.", Toast.LENGTH_SHORT).show();
            if (foundKey) {
                setupPinAlertDialog();
            } else {
                Toast.makeText(getApplicationContext(), "Key not found(yet).", Toast.LENGTH_SHORT).show();
            }
        } else if (isPinLock) {
            Toast.makeText(getApplicationContext(), "PIN is needed.", Toast.LENGTH_SHORT).show();
            if (foundKey) {
                setupPinAlertDialog();
            } else {
                Toast.makeText(getApplicationContext(), "Key not found(yet).", Toast.LENGTH_SHORT).show();
            }
        } else if (isFPLock) {
            if (isFPAvailable) {
                Toast.makeText(getApplicationContext(), "FINGERPRINT is needed.", Toast.LENGTH_SHORT).show();
                biometricPrompt.authenticate(promptInfo);
            } else {
                Toast.makeText(getApplicationContext(), "FINGERPRINT is not available on your device.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (isDeleteNoteEvent) {
                deleteNoteConfirmationDialog();
            } else {
                switchActivityForSelectedNote();
            }
        }
    }

    private void switchActivityForSelectedNote() {
        Intent intent;
        if (!selectedNote.isPaint()) {
            intent = new Intent(NotesListViewActivity.this, NotesActivity.class);
        } else {
            intent = new Intent(NotesListViewActivity.this, NotePaintActivity.class);
        }
        intent.putExtra("isEdit", true);
        intent.putExtra("resultJson", rsObjMap.get(selectedNote));
        startActivity(intent);
        finish();
    }

}
