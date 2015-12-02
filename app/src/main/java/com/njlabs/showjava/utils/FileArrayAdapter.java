package com.njlabs.showjava.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.njlabs.showjava.R;
import com.njlabs.showjava.modals.Item;

import org.apache.commons.io.FilenameUtils;

import java.util.List;

public class FileArrayAdapter extends ArrayAdapter<Item> {

    private final Context context;
    private final int id;
    private final List<Item> items;

    public FileArrayAdapter(Context context, int textViewResourceId,
                            List<Item> objects) {
        super(context, textViewResourceId, objects);
        this.context = context;
        id = textViewResourceId;
        items = objects;
    }

    public Item getItem(int i) {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final Item o = items.get(position);
        if (o != null) {

            TextView filenameView = (TextView) v.findViewById(R.id.file_name);
            TextView fileSizeView = (TextView) v.findViewById(R.id.file_size);

            ImageView fileIconView = (ImageView) v.findViewById(R.id.file_icon);

            Drawable image = context.getResources().getDrawable(o.getImage());

            if (FilenameUtils.getExtension(o.getPath()).equals("png") || FilenameUtils.getExtension(o.getPath()).equals("jpg")) {
                image = Drawable.createFromPath(o.getPath());
            }

            fileIconView.setImageDrawable(image);

            filenameView.setText(o.getName());
            fileSizeView.setText(o.getData());
        }
        return v;
    }
}
