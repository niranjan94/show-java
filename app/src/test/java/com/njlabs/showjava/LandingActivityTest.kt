package com.njlabs.showjava

import com.njlabs.showjava.activities.landing.LandingActivity
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(25))
class LandingActivityTest {
    private lateinit var activity: LandingActivity

    @Before
    @Throws(Exception::class)
    fun setUp() {
        activity = Robolectric.setupActivity(LandingActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun shouldHaveCorrectAppName() {
        val appName = activity.resources.getString(R.string.app_name)
        Assert.assertThat(appName, CoreMatchers.equalTo("Show Java"))
    }
}