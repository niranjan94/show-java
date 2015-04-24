package com.njlabs.showjava.utils.picker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.njlabs.showjava.R;

import java.util.List;

public class FileArrayAdapter extends ArrayAdapter<Option> {

	private Context c;
	private int id;
	private List<Option> items;

	public FileArrayAdapter(Context context, int textViewResourceId,
			List<Option> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
	}

	public Option getItem(int i) {
		return items.get(i);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) c
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(id, null);
		}
		final Option o = items.get(position);
		if (o != null) {
			ImageView im = (ImageView) v.findViewById(R.id.icon);
			TextView t1 = (TextView) v.findViewById(R.id.file_name);
			TextView t2 = (TextView) v.findViewById(R.id.file_type);
			
			if(o.getData().equalsIgnoreCase("folder")){
				im.setImageResource(R.drawable.ic_action_folder);
			} else if (o.getData().equalsIgnoreCase("parent directory")) {
				im.setImageResource(R.drawable.ic_action_arrow_back);
			} else {
				String name = o.getName().toLowerCase();
				if (name.endsWith(".apk"))
					im.setImageResource(R.drawable.ic_action_android);
				else
					im.setImageResource(R.drawable.ic_action_insert_drive_file);
			}
			if (t1 != null)
				t1.setText(o.getName());
			if (t2 != null)
				t2.setText(o.getData());				

		}
		return v;
	}

}
