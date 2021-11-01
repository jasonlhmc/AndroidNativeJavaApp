package com.example.jtestapk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.adapter.CurCalGridAdapter;
import com.example.model.CurrencyCalRowObject;
import com.example.model.CurrencyObject;
import com.example.utils.PropertiesUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class CheckCurrencyActivity extends AppCompatActivity {

    private Properties appProperties;

    private List<CurrencyObject> currencyObjectList = new ArrayList<CurrencyObject>();
    private List<CurrencyObject> curSearchRsList = new ArrayList<CurrencyObject>();
    private TableLayout currencyFullListTable;
    private Map<String, BigDecimal> currencyRateMap = new HashMap<>();
    private Map<String, String> countriesIsoMap = new HashMap<>();

    public String jsonResponseStr;
    public String getJsonResponseStr() {
        return jsonResponseStr;
    }
    public void setJsonResponseStr(String jsonResponseStr) {
        this.jsonResponseStr = jsonResponseStr;
    }

    private Location gps_loc;
    private Location network_loc;
    private Location final_loc;
    private double longitude;
    private double latitude;
    private String userCountry, userAddress;

    private CharSequence curSearchCharSequence;
    private CharSequence curAmountCharSequence;
    private BigDecimal curCalResult;
    private Map<Integer, CurrencyObject> idCurObjMap = new HashMap<>();

    private List<String> rowCurList;
    private CurrencyObject selectedCur;
    private List<CurrencyObject> filterCurList;
    private AutoCompleteTextView curCalFilter;
    private Button curCalSubmitButton;

    private TableLayout curCalTableLayout;
    private Map<Integer, CurrencyCalRowObject> curCalRowMap = new HashMap<Integer, CurrencyCalRowObject>();
    private Map<Integer, Integer> cancelRowMap = new HashMap<Integer, Integer>();

    private CurCalGridAdapter curCalGridAdapter;
    private LayoutInflater inflater;

    private DecimalFormat decimalFormatter = new DecimalFormat("###,###,###,###,###.###");

    public void onCreate(Bundle savedInstanceState) {
        Log.v("INFO", "Check Currency button Clicked");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        //get app.properties
        try {
            appProperties = PropertiesUtils.getAppProperties(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        //Resize when keyboard shows up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        //init inflater
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //init countriesIsoMap for get user locale, location currency
        for (String iso : Locale.getISOCountries()) {
            Locale l = new Locale("", iso);
            countriesIsoMap.put(l.getDisplayCountry(), iso);
        }

        getUserLocation();

        setContentView(R.layout.activity_currency_full);

        currencyFullListTable = (TableLayout) findViewById(R.id.currencyFullListTable);

        //getJsonFromApi
        try {
            getJsonResponse(new CheckCurrencyCallback() {
                @Override
                public void onSuccess(String result) {
                    setJsonResponseStr(result);
                    setupTableLayoutForCurrencyFull(convertStrToObjectList(result));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
        }

        Button currencyRefreshButton = (Button) findViewById(R.id.currencyRefreshButton2);
        currencyRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("INFO", "Check Currency Full List Refresh button Clicked");
                Toast.makeText(getApplicationContext(), "Refresh", Toast.LENGTH_SHORT).show();
                Gson gson = new Gson();
                try {
//                    getJsonResponse();
                    getJsonResponse(new CheckCurrencyCallback() {
                        @Override
                        public void onSuccess(String result) {
                            setJsonResponseStr(result);
                            setupTableLayoutForCurrencyFull(convertStrToObjectList(result));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error :" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        curSearchCharSequence = "";
        curAmountCharSequence = "";
        TextInputEditText curSearchInput = (TextInputEditText) findViewById(R.id.curFullListSearchBar);
        curSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Log.v("INFO", "curSearchInput.beforeTextChanged run");
                curSearchRsList = new ArrayList<CurrencyObject>();
//                curSearchCharSequence = "";
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Log.v("INFO", "charSequence = " + charSequence);
                curSearchCharSequence = charSequence;
                if (!curAmountCharSequence.toString().equals("")) {
                    if (!TextUtils.isDigitsOnly(curAmountCharSequence)) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.warn_digit_only), Toast.LENGTH_LONG).show();
                        return;
                    }
                    double amountInput = Double.valueOf(curAmountCharSequence.toString());
                    for (CurrencyObject currencyObject : currencyObjectList) {
                        if (currencyObject.getCurCode().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT)) ||
                                currencyObject.getCurName().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT))) {
                            BigDecimal rateMultiplyAmount = BigDecimal.valueOf(currencyObject.getRate() * amountInput).setScale(3, RoundingMode.FLOOR);
                            currencyObject.setAmountRate(rateMultiplyAmount.doubleValue());
                            curSearchRsList.add(currencyObject);
                        }
                    }
                } else {
                    for (CurrencyObject currencyObject : currencyObjectList) {
                        if (currencyObject.getCurCode().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT)) ||
                                currencyObject.getCurName().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT))) {
                            curSearchRsList.add(currencyObject);
                        }
                    }
                }
                setupTableLayoutForCurrencyFull(curSearchRsList);
            }
            @Override
            public void afterTextChanged(Editable editable) {
//                Log.v("INFO", "curSearchInput.afterTextChanged run");
                curSearchRsList = new ArrayList<CurrencyObject>();
            }
        });

        TextInputEditText curAmountInput = (TextInputEditText) findViewById(R.id.curFullListAmountInput);
        curAmountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Log.v("INFO", "curAmountInput.beforeTextChanged run");
                curSearchRsList = new ArrayList<CurrencyObject>();
//                curAmountCharSequence = "";
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                Log.v("INFO", "charSequence = " + charSequence);
                curAmountCharSequence = charSequence;
                if (!TextUtils.isDigitsOnly(charSequence)) {
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.warn_digit_only), Toast.LENGTH_LONG).show();
                    return;
                }
                double amountInput = 0;
                if (!charSequence.toString().equals("")) {
                    amountInput = Double.valueOf(charSequence.toString());
                    if (!curSearchCharSequence.toString().equals("")) {
                        for (CurrencyObject currencyObject : currencyObjectList) {
                            if (currencyObject.getCurCode().toLowerCase(Locale.ROOT).contains(curSearchCharSequence.toString().toLowerCase(Locale.ROOT)) ||
                                    currencyObject.getCurName().toLowerCase(Locale.ROOT).contains(curSearchCharSequence.toString().toLowerCase(Locale.ROOT))) {
                                BigDecimal rateMultiplyAmount = BigDecimal.valueOf(currencyObject.getRate() * amountInput).setScale(3, RoundingMode.FLOOR);
                                currencyObject.setAmountRate(rateMultiplyAmount.doubleValue());
                                curSearchRsList.add(currencyObject);
                            }
                        }
                    } else {
                        for (CurrencyObject currencyObject : currencyObjectList) {
                            BigDecimal rateMultiplyAmount = BigDecimal.valueOf(currencyObject.getRate() * amountInput).setScale(3, RoundingMode.FLOOR);
                            currencyObject.setAmountRate(rateMultiplyAmount.doubleValue());
                            curSearchRsList.add(currencyObject);
                        }
                    }
                    setupTableLayoutForCurrencyFull(curSearchRsList);
                } else {
                    if (!curSearchCharSequence.toString().equals("")) {
                        for (CurrencyObject currencyObject : currencyObjectList) {
                            if (currencyObject.getCurCode().toLowerCase(Locale.ROOT).contains(curSearchCharSequence.toString().toLowerCase(Locale.ROOT)) ||
                                    currencyObject.getCurName().toLowerCase(Locale.ROOT).contains(curSearchCharSequence.toString().toLowerCase(Locale.ROOT))) {
                                BigDecimal rateMultiplyAmount = BigDecimal.valueOf(currencyObject.getRate() * amountInput).setScale(3, RoundingMode.FLOOR);
                                currencyObject.setAmountRate(rateMultiplyAmount.doubleValue());
                                curSearchRsList.add(currencyObject);
                            }
                        }
                    } else {
                        for (CurrencyObject currencyObject : currencyObjectList) {
                            BigDecimal rateMultiplyAmount = BigDecimal.valueOf(currencyObject.getRate() * amountInput).setScale(3, RoundingMode.FLOOR);
                            currencyObject.setAmountRate(rateMultiplyAmount.doubleValue());
                            curSearchRsList.add(currencyObject);
                        }
                    }
                    setupTableLayoutForCurrencyFull(curSearchRsList);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
//                Log.v("INFO", "curAmountInput.afterTextChanged run");
                curSearchRsList = new ArrayList<CurrencyObject>();
            }
        });
    }

    private void setupTableLayoutForCurrencyFull(List<CurrencyObject> currencyObjectList) {
        currencyFullListTable.removeAllViews();
        for (CurrencyObject currencyObject : currencyObjectList) {
            TableRow tableRow = new TableRow(this);
            tableRow.setBackgroundResource(R.drawable.common_table_border);
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tableRow.setWeightSum(1f);
            tableRow.setMinimumHeight(100);
            TextView countryTextView = new TextView(this);
            countryTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 0.65f));
            countryTextView.setWidth(0);
            countryTextView.setPadding(16, 0, 16, 0);
            countryTextView.setGravity(Gravity.CENTER_VERTICAL);
            if (currencyObject.getFlag() != null || !currencyObject.getFlag().isEmpty()) {
                countryTextView.setText(currencyObject.getFlag() + " " + currencyObject.getCurName());
            } else {
                countryTextView.setText(currencyObject.getCurName());
            }
            countryTextView.setTextSize(22);
            tableRow.addView(countryTextView);

            TextView rateTextView = new TextView(this);
            rateTextView.setBackgroundResource(R.drawable.common_table_border);
            rateTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 0.35f));
            rateTextView.setWidth(0);
            rateTextView.setPadding(16, 0, 0, 0);
            rateTextView.setGravity(Gravity.CENTER_VERTICAL);
            if (currencyObject.getAmountRate() > currencyObject.getRate()) {
                rateTextView.setText(currencyObject.getSymbol() + " " + decimalFormatter.format(currencyObject.getAmountRate()));
            } else {
                rateTextView.setText(currencyObject.getSymbol() + " " + decimalFormatter.format(currencyObject.getRate()));
            }
            rateTextView.setTextSize(18);
            tableRow.addView(rateTextView);
            int rowId = View.generateViewId();
            tableRow.setId(rowId);
            idCurObjMap.put(rowId, currencyObject);
            tableRow.setOnClickListener(curTableElementOnclickListener);
            currencyFullListTable.addView(tableRow);
        }
    }

    private View.OnClickListener curTableElementOnclickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //for result currency
            curCalResult = BigDecimal.ZERO;
            rowCurList = new ArrayList<String>();
            selectedCur = idCurObjMap.get(view.getId());
            setContentView(R.layout.activity_cur_cal);
            setupCurCalGridLayout(true);
            curCalFilter = (AutoCompleteTextView) findViewById(R.id.curCalFilter);
            curCalFilter.addTextChangedListener(curCalFiltertInputListener);
            curCalSubmitButton = (Button) findViewById(R.id.curCalSubmit);
            curCalSubmitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    currencyObjectList.stream().forEach(System.out::println);
                    if (curCalInputValidation()) {
                        curCalAction();
                    }
                }
            });
        }
    };

    private boolean curCalInputValidation() {
        boolean rs = true;
        for (Map.Entry<Integer, CurrencyCalRowObject> curCalRow : curCalRowMap.entrySet()) {
            TextInputEditText input = (TextInputEditText) findViewById(curCalRow.getValue().getRowInputTextId());
            input.setBackgroundResource(0);
            String errMsg = "";
            if (input.getText().toString() == null || input.getText().toString().isEmpty()) {
                input.setBackgroundResource(R.drawable.textinput_surface_yellow);
                TextInputLayout textParent = (TextInputLayout) input.getParent().getParent();
                textParent.setHint("EMPTY");
                rs = false;
            } else {
                try {
                    BigDecimal amountInput = new BigDecimal(input.getText().toString());
                } catch (Exception e) {
                    input.setBackgroundResource(R.drawable.textinput_surface_red);
                    TextInputLayout textParent = (TextInputLayout) input.getParent().getParent();
                    textParent.setHint("INVALID");
                    rs = false;
                }
            }
            errMsg = "Error found in input field.";
            updateCurCalResult(null, true, errMsg);
        }
        return rs;
    }

    private void curCalAction() {
        curCalResult = BigDecimal.ZERO;
        for (Map.Entry<Integer, CurrencyCalRowObject> curCalRow : curCalRowMap.entrySet()) {
            TextInputEditText input = (TextInputEditText) findViewById(curCalRow.getValue().getRowInputTextId());
            if (input.getText().toString() != null || !input.getText().toString().isEmpty()) {
                BigDecimal amountInput = new BigDecimal(input.getText().toString());
                BigDecimal rateFromCur = getRateOfCurCode(curCalRow.getValue());
                String textHint = "= " + selectedCur.getCurCode() + " " + decimalFormatter.format(rateFromCur.multiply(amountInput));
                curCalResult  = curCalResult.add(rateFromCur.multiply(amountInput));
                TextInputLayout textParent = (TextInputLayout) input.getParent().getParent();
                textParent.setHint(textHint);
            }
        }
        curCalResult = curCalResult.setScale(3, BigDecimal.ROUND_FLOOR);
        updateCurCalResult(selectedCur, false, "");
    }

    private void setupCurCalGridLayout(boolean isInit) {
        //start setupCurCalGridLayout
        if (isInit) {
            //init
            curCalGridAdapter = new CurCalGridAdapter(getApplicationContext(), currencyObjectList);
        } else {
            //isUpdate
            curCalGridAdapter = new CurCalGridAdapter(getApplicationContext(), filterCurList);
        }
        GridView curCalGridView = (GridView) findViewById(R.id.curCalGrid);
        curCalGridView.setAdapter(curCalGridAdapter);
        curCalTableLayout = (TableLayout) findViewById(R.id.curCalTableLayout);
        curCalGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CurrencyObject currencyObject = null;
                if (isInit) {
                    //init
                    currencyObject = currencyObjectList.get(i);
                } else {
                    //isUpdate
                    currencyObject = filterCurList.get(i);
                }
                //checking
                if (rowCurList.contains(currencyObject.getCurCode())) {
                    Toast.makeText(getApplicationContext(),"Already added to list.", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    rowCurList.add(currencyObject.getCurCode());
                }
                if (rowCurList.size() > 8) {
                    rowCurList.remove(currencyObject.getCurCode());
                    Toast.makeText(getApplicationContext(),"Max 8 items.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //clear filter's input text
//                curCalFilter.setText("");
                curCalFilter.getText().clear();

                //add to map
                int rowId = View.generateViewId();
                int curCalTitleId = View.generateViewId();
                int curCalAmountInputTextId = View.generateViewId();
                int curCalCancelButtonId = View.generateViewId();
                CurrencyCalRowObject currencyCalRowObject = new CurrencyCalRowObject();
                currencyCalRowObject.setCurCode(currencyObject.getCurCode());
                currencyCalRowObject.setRowTextViewId(curCalTitleId);
                currencyCalRowObject.setRowInputTextId(curCalAmountInputTextId);
                curCalRowMap.put(rowId, currencyCalRowObject);
                cancelRowMap.put(curCalCancelButtonId, rowId);

                //setup TableLayout's rows
                TableRow curCalRow = (TableRow) inflater.inflate(R.layout.cur_cal_row, curCalTableLayout, false);
                curCalRow.setId(rowId);
                curCalTableLayout.addView(curCalRow);
                //find child in tableRow and set id
                int childParts = curCalRow.getChildCount();
                if (curCalRow != null) {
                    for (int x = 0; x < childParts; x++) {
                        View viewChild = curCalRow.getChildAt(x);
                        if (viewChild instanceof Button) {
                            viewChild.setId(curCalCancelButtonId);
                            ((Button) viewChild).setOnClickListener(curCalCancelButtonListener);
                        } else if (viewChild instanceof TextView) {
                            ((TextView) viewChild).setText(currencyObject.getFlag() + currencyObject.getCurCode());
                            ((TextView) viewChild).setSelected(true);
                            viewChild.setId(curCalTitleId);
                        } else if (viewChild instanceof TextInputLayout) {
                            TextInputEditText textInputLayoutChild = (TextInputEditText) ((TextInputLayout) viewChild).getEditText();
                            textInputLayoutChild.setId(curCalAmountInputTextId);
                            textInputLayoutChild.requestFocus();
//                            textInputLayoutChild.addTextChangedListener(curCalAmountInputListener);
                        }
                    }
                }
            }

        });
        updateCurCalResult(selectedCur, false, "");
    }

    private TextWatcher curCalFiltertInputListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            filterCurList = new ArrayList<CurrencyObject>();
        }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            for (CurrencyObject currencyObject : currencyObjectList) {
                if (currencyObject.getCurCode().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT)) ||
                        currencyObject.getCurName().toLowerCase(Locale.ROOT).contains(charSequence.toString().toLowerCase(Locale.ROOT))) {
                    filterCurList.add(currencyObject);
                }
            }
            setupCurCalGridLayout(false);
        }
        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private View.OnClickListener curCalCancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TableRow cancelRow = (TableRow) findViewById(cancelRowMap.get(view.getId()));
            int textViewId = curCalRowMap.get(cancelRow.getId()).getRowTextViewId();
            TextView textView = (TextView) findViewById(textViewId);
            String textViewText = textView.getText().toString();
//            Log.v("INFO", "textViewText = " + textViewText);
            int index = 0;
            for (int i = 0; i < rowCurList.size(); i++) {
                if (textViewText.contains(rowCurList.get(i))) {
                    index = i;
                }
            }
            rowCurList.remove(index);
            curCalRowMap.remove(cancelRow.getId());
            curCalTableLayout.removeView(cancelRow);
            curCalSubmitButton.performClick();
        }
    };

    private void updateCurCalResult(CurrencyObject currencyObject, boolean isErrorHandling, String errMsg) {
        TextView curCalResultTextView = (TextView) findViewById(R.id.curCalResult);
        String curCalResultText = "";
        if (isErrorHandling) {
            curCalResultText = errMsg;
        } else {
            curCalResultText = "= " + currencyObject.getFlag() + currencyObject.getCurCode() + " " + currencyObject.getSymbol() + decimalFormatter.format(curCalResult);
        }
        curCalResultTextView.setText(curCalResultText);
    }

    private BigDecimal getRateOfCurCode(CurrencyCalRowObject curCalRowObj) {
        BigDecimal rs = BigDecimal.ZERO;
        double rate = 0;
        for (CurrencyObject currencyObject : currencyObjectList) {
            if (currencyObject.getCurCode().equals(curCalRowObj.getCurCode())) {
                rate = currencyObject.getRate();
                break;
            }
        }
        rs = new BigDecimal(selectedCur.getRate() / rate);
//        Log.v("INFO", curCalRowObj.getCurCode() + selectedCur.getCurCode() + " | " + selectedCur.getRate() + " | " + rate + " | " + rs);
        return rs;
    }

//    private Map<String, CurrencyObject> curCodeObjMap;
    private List<CurrencyObject> convertStrToObjectList(String strIn) {
        currencyObjectList = new ArrayList<CurrencyObject>();
        JsonElement element = JsonParser.parseString(strIn);
        JsonObject convertedObject = element.getAsJsonObject();
        List keys = new ArrayList();
        keys.addAll(convertedObject.keySet());
//        Log.v("INFO", "countriesIsoMap = " + countriesIsoMap);
        for (Object key : keys) {
            CurrencyObject currencyObject = new CurrencyObject();
            currencyObject.setCountry(key.toString());
            double rate = convertedObject.getAsJsonObject(key.toString()).get("Exrate").getAsDouble();
            currencyObject.setRate(rate);
            currencyObject.setOriginalRate(rate);
            currencyObject.setSource(convertedObject.getAsJsonObject(key.toString()).toString());
            currencyObjectList.add(currencyObject);
        }
//        currencyObjectList.stream().forEach(System.out::println);
//        Log.v("INFO", "currencyObjectList = " + currencyObjectList);
        //Get user locale for localization currency
        Locale locale = Locale.getDefault();
        if (!userCountry.equals("Unknown")) {
            locale = new Locale("", countriesIsoMap.get(userCountry));
        }
        Currency localeCur = Currency.getInstance(locale);
//        Log.v("INFO", "locale = " + locale + "; localeCur = " + localeCur);
        double localeRate = 0;
        for (CurrencyObject currencyObject : currencyObjectList) {
            if (("USD" + localeCur.getCurrencyCode()).equals(currencyObject.getCountry())) {
                localeRate = currencyObject.getOriginalRate();
                break;
            }
        }
        for (CurrencyObject currencyObject : currencyObjectList) {
            String flag = "";
            String curName = currencyObject.getCountry();
            String curCode = currencyObject.getCountry();
            String curSign = "$";
            BigDecimal roundedRate = new BigDecimal(currencyObject.getOriginalRate()).setScale(3, RoundingMode.FLOOR);
            BigDecimal rateToLocaleRate = new BigDecimal(currencyObject.getOriginalRate()).setScale(3, RoundingMode.FLOOR);
            BigDecimal roundedRateToLocaleRate = new BigDecimal(currencyObject.getOriginalRate()).setScale(3, RoundingMode.FLOOR);
            if (localeRate != 0) {
                roundedRateToLocaleRate = BigDecimal.valueOf(currencyObject.getOriginalRate() / localeRate).setScale(3, RoundingMode.FLOOR);
            }
//            Log.v("INFO", "currencyObject.getRate() = " + currencyObject.getRate() + "; localeRate = " + localeRate + "; rateToLocaleRate = " + rateToLocaleRate);
            if (currencyObject.getCountry().length() == 6) {
                curCode = currencyObject.getCountry().substring(3, 6);
                try {
                    curName = Currency.getInstance(curCode).getDisplayName().replace("(", "\r\n(");
                    if (!((Currency.getInstance(curCode).getSymbol()).contains("$")) && !((Currency.getInstance(curCode).getSymbol()).equals(curCode))) {
                        curSign =  Currency.getInstance(curCode).getSymbol();
                    }
                    //No flag for exceptional cases
                    if (!curName.equals("BTC") && !curName.equals("BRX") && !curName.equals("HUX") && !curName.equals("LTC") && !curName.equals("XAUX")) {
                        int flagOffset = 0x1F1E6;
                        int asciiOffset = 0x41;
                        String cntryCode = curCode.substring(0, 2);
                        int firstChar = Character.codePointAt(cntryCode, 0) - asciiOffset + flagOffset;
                        int secondChar = Character.codePointAt(cntryCode, 1) - asciiOffset + flagOffset;
                        flag = new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("INFO", e + curCode + "; " + curSign + "; " + flag);
                }
            }
            currencyObject.setFlag(flag);
            currencyObject.setCurName(curName);
            currencyObject.setSymbol(curSign);
            currencyObject.setCurCode(curCode);
            currencyObject.setLocaleCur(localeCur.getCurrencyCode());
            currencyObject.setLocaleRate(localeRate);
            currencyObject.setRoundedRate(roundedRate.doubleValue());
            currencyObject.setAmountRate(roundedRateToLocaleRate.doubleValue());
            currencyObject.setRate(roundedRateToLocaleRate.doubleValue());
        }
        Collections.sort(currencyObjectList, Comparator.comparing(CurrencyObject::getCurName));
//        currencyObjectList.stream().forEach(System.out::println);
        return currencyObjectList;
    }

    private void getJsonResponse(final CheckCurrencyCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = appProperties.getProperty("currency.api");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
//                        Toast.makeText(getApplicationContext(),"Response :" + response.toString(), Toast.LENGTH_LONG).show();
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Toast.makeText(getApplicationContext(),"Error :" + e, Toast.LENGTH_LONG).show();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void getUserLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            network_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (gps_loc != null) {
            final_loc = gps_loc;
            latitude = final_loc.getLatitude();
            longitude = final_loc.getLongitude();
        } else if (network_loc != null) {
            final_loc = network_loc;
            latitude = final_loc.getLatitude();
            longitude = final_loc.getLongitude();
        } else {
            latitude = 0.0;
            longitude = 0.0;
        }

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE}, 1);

        try {

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                userCountry = addresses.get(0).getCountryName();
                userAddress = addresses.get(0).getAddressLine(0);
//                Log.v("INFO", "userCountry = " + userCountry + "; userAddress = " + userAddress);
            }
            else {
                userCountry = "Unknown";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
