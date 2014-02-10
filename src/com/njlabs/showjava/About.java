package com.njlabs.showjava;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

public class About extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		getActionBar().hide();
		
		Typeface face = Typeface.createFromAsset(getAssets(), "roboto_light.ttf"); 
		TextView tv=(TextView)findViewById(R.id.AppName); 
		tv.setTypeface(face);
		tv=(TextView)findViewById(R.id.AppVersion); 
		tv.setTypeface(face);
		tv=(TextView)findViewById(R.id.ThirdPartySoftwaresText); 
		tv.setTypeface(face);
		tv=(TextView)findViewById(R.id.ThirdPartySoftwaresList); 
		tv.setTypeface(face);
		tv=(TextView)findViewById(R.id.AppDeveloper); 
		tv.setTypeface(face);
	}
}
