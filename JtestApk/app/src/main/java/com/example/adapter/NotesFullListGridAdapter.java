package com.example.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.jtestapk.R;
import com.example.model.NoteModelObject;
import com.example.model.NoteTaskObject;
import com.example.utils.BlurUtils;

import org.w3c.dom.Text;

import java.util.Base64;
import java.util.List;

public class NotesFullListGridAdapter extends BaseAdapter {

    BlurUtils blurUtils = new BlurUtils();
    Context context;
    List<NoteModelObject> noteModelObjectList;
    LayoutInflater inflater;
    public NotesFullListGridAdapter(Context applicationContext, List<NoteModelObject> noteModelObjectList) {
        this.context = applicationContext;
        this.noteModelObjectList = noteModelObjectList;
        inflater = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return noteModelObjectList.size();
    }

    @Override
    public Object getItem(int i) {
        return noteModelObjectList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        NoteModelObject noteModelObject = noteModelObjectList.get(i);
        if (noteModelObject.isPaint()) {
            view = inflater.inflate(R.layout.notes_grid_paint, viewGroup, false);
            view.setBackgroundColor(Color.DKGRAY);
            TextView noteTextLock = (TextView) view.findViewById(R.id.noteTextLock);
            if (noteModelObject.isPinLock()) {
                noteTextLock.setText(context.getResources().getString(R.string.text_lock_pin));
            }
            if (noteModelObject.isFPLock()) {
                noteTextLock.setText(context.getResources().getString(R.string.text_lock_fp));
            }
            if (noteModelObject.isPinLock() && noteModelObject.isFPLock()) {
                noteTextLock.setText(context.getResources().getString(R.string.text_lock_pin_fp));
            }
            if (!noteModelObject.isPinLock() && !noteModelObject.isFPLock()) {
                noteTextLock.setVisibility(View.GONE);
            }
            TextView noteTextDate = (TextView) view.findViewById(R.id.noteTextDate);
            noteTextDate.setText(noteModelObject.getDateStr());
            TextView noteTextEditDate = (TextView) view.findViewById(R.id.noteTextEditDate);
            if (noteModelObject.getEditDate() != null) {
                noteTextEditDate.setText(context.getResources().getString(R.string.text_last_edit) + noteModelObject.getEditDateStr());
            } else {
                noteTextEditDate.setVisibility(View.GONE);
            }
            ImageView notePaintImageView = (ImageView) view.findViewById(R.id.notePaintImageView);
//            if (noteModelObject.isPinLock() || noteModelObject.isFPLock()) {
//                //Blur Image if locked
//                image = blurUtils.fastBlur(convertStringToBitmap(noteModelObject.getContent()), 0.75f, 50);
//            } else {
//                image = convertStringToBitmap(noteModelObject.getContent());
//            }
            notePaintImageView.setImageBitmap(noteModelObject.getGridViewPaint());
        } else {
            String content = "";
            view = inflater.inflate(R.layout.notes_grid_text, viewGroup, false);
            view.setBackgroundColor(Color.DKGRAY);
            TextView noteTextLock = (TextView) view.findViewById(R.id.noteTextLock);
            if (noteModelObject.isPinLock()) {
                noteTextLock.setText(context.getResources().getString(R.string.text_lock_pin));
            }
            if (noteModelObject.isFPLock()) {
                noteTextLock.setText(context.getResources().getString(R.string.text_lock_fp));
            }
            if (noteModelObject.isPinLock() && noteModelObject.isFPLock()) {
                noteTextLock.setText(context.getResources().getString(R.string.text_lock_pin_fp));
            }
            if (!noteModelObject.isPinLock() && !noteModelObject.isFPLock()) {
                noteTextLock.setVisibility(View.GONE);
            }
            TextView noteTextTitle = (TextView) view.findViewById(R.id.noteTextTitle);
            noteTextTitle.setText(noteModelObject.getTitle());
            TextView noteTextDate = (TextView) view.findViewById(R.id.noteTextDate);
            noteTextDate.setText(noteModelObject.getDateStr());
            TextView noteTextEditDate = (TextView) view.findViewById(R.id.noteTextEditDate);
            if (noteModelObject.getEditDate() != null) {
                noteTextEditDate.setText(context.getResources().getString(R.string.text_last_edit) + noteModelObject.getEditDateStr());
            } else {
                noteTextEditDate.setVisibility(View.GONE);
            }
            TextView noteTextContent = (TextView) view.findViewById(R.id.noteTextContent);
            if (noteModelObject.isPinLock() || noteModelObject.isFPLock()) {
                content = noteModelObject.getContent().replaceAll(".",  "*");
            } else {
                content = noteModelObject.getContent();
            }
            noteTextContent.setText(content);
            TableLayout gridNoteTaskTable = (TableLayout) view.findViewById(R.id.gridNoteTaskTable);
            if (noteModelObject.isTask()) {
                for (NoteTaskObject noteTaskObject : noteModelObject.getTaskList()) {
                    LinearLayout taskLinearLayout = (LinearLayout) inflater.inflate(R.layout.note_grid_text_task, gridNoteTaskTable, false);
                    ((TextView) taskLinearLayout.getChildAt(0)).setText((noteTaskObject.isFinished() ? context.getResources().getString(R.string.toggle_task_on) : context.getResources().getString(R.string.toggle_task_off)));
                    if (noteModelObject.isPinLock() || noteModelObject.isFPLock()) {
                        ((TextView) taskLinearLayout.getChildAt(1)).setText(noteTaskObject.getTask().replaceAll(".",  "*"));
                    } else {
                        ((TextView) taskLinearLayout.getChildAt(1)).setText(noteTaskObject.getTask());
                    }
                    gridNoteTaskTable.addView(taskLinearLayout);
                }
            } else {
                gridNoteTaskTable.setVisibility(View.GONE);
            }
        }
        return view;
    }

}
