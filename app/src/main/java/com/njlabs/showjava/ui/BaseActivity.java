package com.njlabs.showjava.ui;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.njlabs.showjava.R;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    public Context baseContext;
    public Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseContext = this;
    }
    public void setupLayout(int layoutRef){
        setContentView(layoutRef);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    public void setupLayout(int layoutRef, String title){
        setContentView(layoutRef);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
    }
    public void setupLayoutNoActionBar(int layoutRef){
        setContentView(layoutRef);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.about_option) {
                        return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public Context getContext(){
        return baseContext;
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }
}
