package com.example.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.jtestapk.R;
import com.example.model.CurrencyObject;
import com.example.model.NoteModelObject;

import java.util.List;

public class CurCalGridAdapter extends BaseAdapter {
    Context context;
    List<CurrencyObject> currencyObjectList;
    LayoutInflater inflater;
    public CurCalGridAdapter(Context applicationContext, List<CurrencyObject> currencyObjectList) {
        this.context = applicationContext;
        this.currencyObjectList = currencyObjectList;
        inflater = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return currencyObjectList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        CurrencyObject CurrencyObject = currencyObjectList.get(i);
        view = inflater.inflate(R.layout.cur_cal_flag, viewGroup, false);
        TextView curCalFlagText = (TextView) view.findViewById(R.id.curCalFlagText);
        curCalFlagText.setText(CurrencyObject.getFlag() + " " + CurrencyObject.getCurCode());
        return view;
    }
}
