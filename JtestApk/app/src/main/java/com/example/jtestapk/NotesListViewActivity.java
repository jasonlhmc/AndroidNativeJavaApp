package com.example.jtestapk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import com.example.utils.EncryptDecryptUtil;
import com.example.utils.ImageConvertUtils;
import com.example.utils.PropertiesUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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

    private Properties appProperties;

    private App mongoApp;
    private User mongoUser;
    private MongoClient mongoClient;
    private MongoDatabase javaApplicationDB;
    private MongoCollection notesFromAndroidAppCollection;
    private MongoCollection keyCollection;

    private List<NoteModelObject> noteModelObjectList;
    private List<NoteModelObject> noteSearchList;

    private NotesFullListGridAdapter notesFullListGridAdapter;
    private FloatingActionButton notesFullListFloatingActionButton, notesFloatSearch, notesFloatNew;
    boolean subFabVisible;
    private TextView textFabSearch, textFabNew;
    private TextInputLayout notesFullListSearch;
    private TextInputEditText notesSearchInput;

//    private List<String> resultJsonList;
    private Map<NoteModelObject, String> rsObjMap;
    private NoteModelObject selectedNote;
    private ProgressBar loadingNotesList;
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

    private LayoutInflater inflater;

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

        //init MongoDB Connection
        Realm.init(this);
        String appID = appProperties.getProperty("mongoDB.appId");
        mongoApp = new App(new AppConfiguration.Builder(appID).build());
//        Credentials credentials = Credentials.apiKey("DEwFRdullYJKDibKqvxQ6XuoJRc4xKBN52WGZHC6jHK14frFLItXjq1LMw0002Gz");
//        AtomicReference<User> authUser = new AtomicReference<User>();
//        app.loginAsync(credentials, it -> {
//            if (it.isSuccess()) {
//                Log.v("AUTH", "Successfully authenticated using an API Key.");
//                authUser.set(app.currentUser());
//            } else {
//                Log.e("AUTH", it.getError().toString());
//            }
//        });
        mongoUser = mongoApp.currentUser();
        mongoClient = mongoUser.getMongoClient(appProperties.getProperty("mongoDB.client"));
        javaApplicationDB = mongoClient.getDatabase(appProperties.getProperty("mongoDB.database"));
        notesFromAndroidAppCollection = javaApplicationDB.getCollection(appProperties.getProperty("mongoDB.collection.notes"));
        keyCollection = javaApplicationDB.getCollection(appProperties.getProperty("mongoDB.collection.key"));
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
                loadingNotesList.setVisibility(View.INVISIBLE);
            } else {
                Log.e("EXAMPLE", "failed to find document with: ", task.getError());
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
            }
        });
        setContentView(R.layout.activity_notes_full);
        //set loading screen
        loadingNotesList = (ProgressBar) findViewById(R.id.loadingNotesList);
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
        notesFullListFloatingActionButton = (FloatingActionButton) findViewById(R.id.notesFullListFloatingActionButton);
        notesFloatSearch = (FloatingActionButton) findViewById(R.id.notesFloatSearch);
        notesFloatNew = (FloatingActionButton) findViewById(R.id.notesFloatNew);
        notesFullListFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!subFabVisible) {
                    notesFloatSearch.show();
                    notesFloatNew.show();
                    textFabSearch.setVisibility(View.VISIBLE);
                    textFabNew.setVisibility(View.VISIBLE);
                    subFabVisible = true;
                } else {
                    notesFloatSearch.hide();
                    notesFloatNew.hide();
                    textFabSearch.setVisibility(View.GONE);
                    textFabNew.setVisibility(View.GONE);
                    subFabVisible = false;
                }
            }
        });
        notesFloatSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (notesFullListSearch.getVisibility() == View.VISIBLE) {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fragment_fade_exit);
                    anim.setStartOffset(75);
                    notesFullListSearch.setAnimation(anim);
                    notesFullListSearch.setVisibility(View.GONE);
                } else {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fragment_fade_enter);
                    anim.setStartOffset(75);
                    notesFullListSearch.setAnimation(anim);
                    notesFullListSearch.setVisibility(View.VISIBLE);
                }
                notesFullListFloatingActionButton.performClick();
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
                view.setBackgroundColor(Color.GRAY);
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
                        view.setBackgroundColor(Color.DKGRAY);
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
