package com.njlabs.showjava.activities.decompiler

import android.os.Bundle
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import android.view.animation.LinearInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation

import kotlinx.android.synthetic.main.activity_decompiler.*

class DecompilerActivity : BaseActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setupLayoutNoActionBar(R.layout.activity_decompiler)
        setupGears()
    }

    private fun getGearAnimation(duration: Int = 1, isClockwise: Boolean = true): RotateAnimation {
        val animation = RotateAnimation(
                if (isClockwise) 0.0f else 360.0f,
                if (isClockwise) 360.0f else 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        )
        animation.repeatCount = Animation.INFINITE
        animation.duration = duration.toLong() * 1500
        animation.interpolator = LinearInterpolator()
        return animation
    }

    private fun setupGears() {
        leftProgressGear.post({ leftProgressGear.animation = getGearAnimation(2, true) })
        rightProgressGear.post({ rightProgressGear.animation = getGearAnimation(1, false) })
    }

}