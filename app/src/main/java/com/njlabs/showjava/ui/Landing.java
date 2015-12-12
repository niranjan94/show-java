package com.njlabs.showjava.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.njlabs.showjava.BuildConfig;
import com.njlabs.showjava.Constants;
import com.njlabs.showjava.R;
import com.njlabs.showjava.utils.AesCbcWithIntegrity;
import com.njlabs.showjava.utils.SourceInfo;
import com.njlabs.showjava.utils.Utils;
import com.njlabs.showjava.utils.Verify;
import com.njlabs.showjava.utils.logging.Ln;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import xyz.codezero.apl.SV;


@SuppressWarnings("unused")
public class Landing extends BaseActivity  implements BillingProcessor.IBillingHandler{

    private static final int FILE_PICKER = 0;

    private LinearLayout welcomeLayout;
    private ListView listView;

    private BillingProcessor bp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupLayout(R.layout.activity_landing);

        listView = (ListView) findViewById(R.id.history_list);
        View header = getLayoutInflater().inflate(R.layout.history_header_view, listView, false);
        listView.addHeaderView(header, null, false);

        welcomeLayout = (LinearLayout) findViewById(R.id.welcome_layout);

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.navbar_header)
                .addProfiles(
                        new ProfileDrawerItem().withName(getResources().getString(R.string.app_name)+(isPro()?" Pro":"")).withEmail("Version " + BuildConfig.VERSION_NAME).setSelectable(false)
                )
                .withSelectionListEnabledForSingleProfile(false)
                .build();

        ArrayList<IDrawerItem> drawerItems = new ArrayList<>();

        drawerItems.add(new PrimaryDrawerItem().withName("Home").withIcon(R.drawable.ic_action_home).withCheckable(false));
        drawerItems.add(new DividerDrawerItem());
        drawerItems.add(new PrimaryDrawerItem().withName("Report a Bug").withIcon(R.drawable.ic_action_bug_report).withCheckable(false));
        drawerItems.add(new PrimaryDrawerItem().withName("About the app").withIcon(R.drawable.ic_action_info).withCheckable(false));
        drawerItems.add(new PrimaryDrawerItem().withName("Settings").withIcon(R.drawable.ic_action_settings).withCheckable(false));

        if(!isPro()) {
            drawerItems.add(new DividerDrawerItem());
            drawerItems.add(new PrimaryDrawerItem().withName("Get Show Java Pro").withIcon(R.mipmap.ic_logo_plain).withCheckable(false));
        } else {
            if(!Verify.good(baseContext)){
                put(false);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(baseContext, R.style.AlertDialog);
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Show Java Pro has been disabled. Either you have Lucky Patcher (or) Freedom (or) the apk has been tampered with. If you have really purchased Pro, please fix the above mentioned errors to get the purchase restored.");
                alertDialog.setPositiveButton("I Understand", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(baseContext, "Thanks for understanding ... :)", Toast.LENGTH_LONG).show();
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

        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .withDrawerItems(drawerItems)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        switch (position) {
                            case 2:
                                Uri uri = Uri.parse("https://github.com/niranjan94/show-java/issues/new");
                                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                break;
                            case 3:
                                startActivity(new Intent(baseContext, About.class));
                                break;
                            case 4:
                                startActivity(new Intent(baseContext, SettingsActivity.class));
                                break;
                            case 6:
                                startActivity(new Intent(baseContext, PurchaseActivity.class));
                                break;
                        }
                        return false;
                    }
                })
                .withCloseOnClick(true)
                .build();

        if(isMarshmallow()){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,  new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, Constants.STORAGE_PERMISSION_REQUEST);
            } else {
                initHistoryLoader();
            }
        } else {
            initHistoryLoader();
        }


        try {

            AesCbcWithIntegrity.SecretKeys keys = new AesCbcWithIntegrity.SecretKeys(getResources().getString(R.string.cc),getResources().getString(R.string.ii));
            String plainText = AesCbcWithIntegrity.decryptString(BuildConfig.GOOGLE_PLAY_LICENSE_KEY, keys);
            bp = new BillingProcessor(this, plainText, this);

        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            Crashlytics.logException(e);
        }
    }

    public void initHistoryLoader(){
        HistoryLoader historyLoader = new HistoryLoader();
        historyLoader.execute();
    }
    public void SetupList(List<SourceInfo> AllPackages) {

        if (AllPackages.size() < 1) {
            listView.setVisibility(View.GONE);
            welcomeLayout.setVisibility(View.VISIBLE);
        } else {
            welcomeLayout.setVisibility(View.INVISIBLE);

            ArrayAdapter<SourceInfo> decompileHistoryItemArrayAdapter = new ArrayAdapter<SourceInfo>(getBaseContext(), R.layout.history_list_item, AllPackages) {
                @SuppressLint("InflateParams")
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.history_list_item, null);
                    }

                    SourceInfo pkg = getItem(position);

                    ViewHolder holder = new ViewHolder();

                    holder.packageLabel = (TextView) convertView.findViewById(R.id.history_item_label);
                    holder.packageName = (TextView) convertView.findViewById(R.id.history_item_package);
                    holder.packageIcon = (ImageView) convertView.findViewById(R.id.history_item_icon);

                    convertView.setTag(holder);

                    holder.packageLabel.setText(pkg.getPackageLabel());
                    holder.packageName.setText(pkg.getPackageName());

                    if (pkg.getPackageLabel().equalsIgnoreCase(pkg.getPackageName())) {
                        holder.packageName.setVisibility(View.INVISIBLE);
                    }

                    String iconPath = Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + pkg.getPackageName() + "/icon.png";

                    if (new File(iconPath).exists()) {
                        Bitmap iconBitmap = BitmapFactory.decodeFile(iconPath);
                        holder.packageIcon.setImageDrawable(new BitmapDrawable(getResources(), iconBitmap));
                    } else {
                        holder.packageIcon.setImageResource(R.drawable.generic_icon);
                    }

                    return convertView;
                }
            };
            listView.setAdapter(decompileHistoryItemArrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final ViewHolder holder = (ViewHolder) view.getTag();
                    final File sourceDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + holder.packageName.getText().toString() + "");
                    Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
                    i.putExtra("java_source_dir", sourceDir + "/");
                    i.putExtra("package_id", holder.packageName.getText().toString());
                    startActivity(i);
                }
            });

            listView.setVisibility(View.VISIBLE);

        }
    }

    public void OpenAppListing(View v) {
        Intent i = new Intent(getApplicationContext(), AppListing.class);
        startActivity(i);
    }

    public void OpenFilePicker(View v) {
        Intent i = new Intent(this, FilePicker.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_PICKER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER) {
            if (data != null) {

                Uri uri = data.getData();
                File apkFile = new File(uri.getPath());
                final String PackageDir = apkFile.getAbsolutePath();

                Ln.d(PackageDir);

                final String PackageName;
                final String PackageId;

                if (FilenameUtils.isExtension(PackageDir, "apk")) {
                    PackageManager pm = getPackageManager();
                    PackageInfo info = pm.getPackageArchiveInfo(PackageDir, PackageManager.GET_ACTIVITIES);
                    if (info != null) {
                        ApplicationInfo appInfo = info.applicationInfo;

                        if (Build.VERSION.SDK_INT >= 8) {
                            appInfo.sourceDir = PackageDir;
                            appInfo.publicSourceDir = PackageDir;
                        }
                        PackageName = info.applicationInfo.loadLabel(getPackageManager()).toString();
                        PackageId = info.packageName;

                    } else {
                        PackageName = "";
                        PackageId = "";
                    }

                    if(!prefs.getBoolean("hide_decompiler_select",false)){
                        final CharSequence[] items = { "CFR 0.102", "JaDX 0.6.1" };
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Pick a decompiler");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                                i.putExtra("package_id", PackageId);
                                i.putExtra("package_label", PackageName);
                                i.putExtra("package_file_path", PackageDir);
                                i.putExtra("decompiler", (item==1?"jadx":"cfr"));
                                startActivity(i);
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                        i.putExtra("package_id", PackageId);
                        i.putExtra("package_label", PackageName);
                        i.putExtra("package_file_path", PackageDir);
                        i.putExtra("decompiler", prefs.getString("decompiler", "cfr"));
                        startActivity(i);
                    }
                }
            }
        } else {
            bp.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void cleanOldSources() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/ShowJava");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.getName().equalsIgnoreCase("sources")) {
                    try {
                        if(file.exists()){
                            if(file.isDirectory()){
                                FileUtils.deleteDirectory(file);
                            } else {
                                file.delete();
                            }
                        }
                    } catch (Exception e) {
                        Ln.d(e);
                    }
                }
            }
        } else {
            dir.mkdirs();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        rerunHistoryLoader();
    }

    private void rerunHistoryLoader() {
        HistoryLoader historyLoaderTwo = new HistoryLoader();
        historyLoaderTwo.execute();
    }

    private static class ViewHolder {
        TextView packageLabel;
        TextView packageName;
        ImageView packageIcon;
        int position;
    }

    private class HistoryLoader extends AsyncTask<String, String, List<SourceInfo>> {

        @Override
        protected List<SourceInfo> doInBackground(String... params) {

            List<SourceInfo> historyItems = new ArrayList<>();
            File showJavaDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/");
            showJavaDir.mkdirs();

            File nomedia = new File(showJavaDir, ".nomedia");

            if (!nomedia.exists() || !nomedia.isFile()) {
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    Ln.d(e);
                }
            }

            File dir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources");

            if (dir.exists()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (Utils.sourceExists(file)) {
                        historyItems.add(Utils.getSourceInfoFromSourcePath(file));
                    } else {
                        if (!Utils.isProcessorServiceRunning(baseContext)) {
                            try {
                                if(file.exists()){
                                    if(file.isDirectory()){
                                        FileUtils.deleteDirectory(file);
                                    } else {
                                        file.delete();
                                    }
                                }

                            } catch (Exception e) {
                                Ln.d(e);
                            }
                        }
                        if (file.exists()&&!file.isDirectory()) {
                            file.delete();
                        }
                    }
                }
            }

            return historyItems;
        }

        @Override
        protected void onPostExecute(List<SourceInfo> AllPackages) {
            SetupList(AllPackages);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.STORAGE_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initHistoryLoader();
                } else {
                    Toast.makeText(baseContext, "Storage permission is required to use this app", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

        }
    }

    @Override
    public void onBillingInitialized() {
        if(!isPro()) {
            bp.loadOwnedPurchasesFromGoogle();
        }
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails transactionDetails) {

    }

    @Override
    public void onBillingError(int i, Throwable throwable) {
        Ln.e(throwable);
    }

    @Override
    public void onPurchaseHistoryRestored() {
        try {
            final TransactionDetails transactionDetails = bp.getPurchaseTransactionDetails(BuildConfig.IAP_PRODUCT_ID);
            if(transactionDetails.productId.equals(BuildConfig.IAP_PRODUCT_ID)) {

                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();

                params.put("payload", SV.gen(baseContext,transactionDetails.purchaseToken));
                params.put("order_id", transactionDetails.orderId);

                client.post(com.njlabs.showjava.Constants.VERIFICATION_URL, params,
                        new JsonHttpResponseHandler() {

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    if (response.has("status") && response.getString("status").equals("ok")) {
                                        if (response.has("payload")) {
                                            if (SV.good(baseContext, response.getString("payload"), transactionDetails.purchaseToken)) {
                                                showPurchased();
                                            } else {
                                                showError();
                                            }
                                        } else {
                                            showError();
                                        }
                                    } else {
                                        showError();
                                    }
                                } catch (JSONException e) {
                                    showError();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                Ln.e(throwable);
                            }
                        });
            } else {
                put(false);
            }
        } catch (Exception ignored) {
            put(false);
        }
    }

    @Override
    public void onDestroy() {
        if (bp != null)
            bp.release();
        super.onDestroy();
    }

    public void showError() {
        Toast.makeText(this, "Your purchase could not be verified.", Toast.LENGTH_SHORT).show();
    }

    public void showPurchased() {
        put(true);
        Toast.makeText(this, "Thank you for purchasing Show Java Pro :)", Toast.LENGTH_SHORT).show();
    }


}
