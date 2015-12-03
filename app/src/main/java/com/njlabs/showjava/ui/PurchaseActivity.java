package com.njlabs.showjava.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.Constants;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.njlabs.showjava.BuildConfig;
import com.njlabs.showjava.R;
import com.njlabs.showjava.utils.AesCbcWithIntegrity;
import com.njlabs.showjava.utils.Verify;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class PurchaseActivity extends BaseActivity implements BillingProcessor.IBillingHandler {

    private BillingProcessor bp;
    private ProgressBar progressBar;
    private LinearLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupLayout(R.layout.activity_purchase);

        try {

            AesCbcWithIntegrity.SecretKeys keys = new AesCbcWithIntegrity.SecretKeys(getResources().getString(R.string.cc),getResources().getString(R.string.ii));
            String plainText = AesCbcWithIntegrity.decryptString(BuildConfig.GOOGLE_PLAY_LICENSE_KEY, keys);
            bp = new BillingProcessor(this, plainText, this);

        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            Crashlytics.logException(e);
            Toast.makeText(this,"An unexpected error occurred",Toast.LENGTH_SHORT).show();
            finish();
        }

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);


        if(!Verify.good(baseContext)){
            put(false);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(baseContext, R.style.AlertDialog);
            alertDialog.setCancelable(false);
            alertDialog.setMessage("You cannot purchase Show Java Pro. Either you have Lucky Patcher (or) Freedom (or) the apk has been tampered with. If you have really purchased Pro, please fix the above mentioned errors to get the purchase restored.");
            alertDialog.setPositiveButton("I Understand", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(baseContext, "Thanks for understanding ... :)", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            alertDialog.setNegativeButton("I'm a Pirate", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(baseContext, "Well... I'm not :)", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            alertDialog.show();
        }

    }

    public void initiateBuy(View v) {
        progressBar.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
        bp.purchase(this, BuildConfig.IAP_PRODUCT_ID);
    }

    @Override
    public void onBillingInitialized() {
        bp.loadOwnedPurchasesFromGoogle();
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails transactionDetails) {
        if(transactionDetails.productId.equals(BuildConfig.IAP_PRODUCT_ID)) {
            put(true);
            Toast.makeText(this, "Thank you for purchasing Show Java Pro :)", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(baseContext, Landing.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBillingError(int i, Throwable throwable) {
        progressBar.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);

        switch (i) {
            case Constants.BILLING_RESPONSE_RESULT_USER_CANCELED:
                Toast.makeText(this,"The process was cancelled",Toast.LENGTH_SHORT).show();
                break;
            case Constants.BILLING_RESPONSE_RESULT_ERROR:
                Toast.makeText(this,"An error occurred while processing your payment. Please try again",Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this,"An error occurred while processing your request",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        try {
            TransactionDetails transactionDetails = bp.getPurchaseTransactionDetails(BuildConfig.IAP_PRODUCT_ID);
            if(transactionDetails.productId.equals(BuildConfig.IAP_PRODUCT_ID)) {
                put(true);
                Toast.makeText(this, "Thank you for purchasing Show Java Pro :)", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(baseContext, Landing.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                put(false);
            }
        } catch (Exception ignored) {
            put(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        if (bp != null)
            bp.release();
        super.onDestroy();
    }
}
