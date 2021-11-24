package com.artifex.menu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.artifex.mupdfdemo.R;
import com.skydoves.powermenu.MenuBaseAdapter;

public class CustomMenuAdapter extends MenuBaseAdapter<DefaultEditMenu> {
    @Override
    public View getView(int index, View view, ViewGroup viewGroup) {
        DefaultEditMenu editMenu = (DefaultEditMenu) getItem(index);
        final Context context = viewGroup.getContext();
        if(view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_menu, viewGroup, false);
        }
        TextView textView = view.findViewById(R.id.tv_text);
        textView.setText(editMenu.getText());
        textView.setBackgroundColor(Color.TRANSPARENT);
        textView.setPaintFlags(textView.getPaintFlags()  & (~ Paint.STRIKE_THRU_TEXT_FLAG));
        switch (editMenu.getType()){
            case HIGHLIGHT:
                textView.setBackgroundColor(Color.parseColor("#F2C94C"));
                break;
            case UNDERLINE:
                SpannableString content = new SpannableString(editMenu.getText());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                textView.setText(content);
                break;
            case STRIKEOUT:
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                break;
            default:

                break;
        }
        return super.getView(index, view, viewGroup);
    }
}
