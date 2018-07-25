package com.njlabs.showjava.activities.about

import android.os.Bundle
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity

class AboutActivity : BaseActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setupLayoutNoActionBar(R.layout.activity_about)
    }
}