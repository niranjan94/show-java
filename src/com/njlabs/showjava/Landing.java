package com.njlabs.showjava;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class Landing extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);
		getActionBar().setIcon(R.drawable.ic_action_bar);
	}
	public void OpenAppListing(View v)
	{
		Intent i = new Intent(getApplicationContext(), AppListing.class);
		startActivity(i);
	}
	public void OpenFilePicker(View v)
	{
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.landing, menu);
		return true;
	}

}
