package com.njlabs.showjava.ui;

import android.os.Bundle;

import com.njlabs.showjava.R;

public class About extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupLayoutNoActionBar(R.layout.activity_about);
	}
}
