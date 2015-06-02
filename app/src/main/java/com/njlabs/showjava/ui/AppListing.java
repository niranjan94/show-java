package com.njlabs.showjava.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.njlabs.showjava.R;
import com.njlabs.showjava.utils.logging.Ln;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListing extends BaseActivity {

    ProgressDialog PackageLoadDialog;
    ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_app_listing, "Show Java");

        PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);
        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
        PackageLoadDialog.setMessage("Loading installed applications...");

        listView = (ListView) findViewById(R.id.list);
        PackageLoadDialog.show();

        ApplicationLoader runner = new ApplicationLoader();
        runner.execute();

    }

    private class ApplicationLoader extends AsyncTask<String, String, ArrayList<PackageInfoHolder>> {

        @Override
        protected ArrayList<PackageInfoHolder> doInBackground(String... params) {
            publishProgress("Retrieving installed application");
            return getInstalledApps(this);
        }

        @Override
        protected void onPostExecute(ArrayList<PackageInfoHolder> AllPackages) {
            setupList(AllPackages);
            PackageLoadDialog.dismiss();
        }

        public void doProgress(String value) {
            publishProgress(value);
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... text) {
            PackageLoadDialog.setMessage(text[0]);
        }
    }

    private static class ViewHolder {
        TextView packageLabel;
        TextView packageName;
        TextView packageVersion;
        TextView packageFilePath;
        ImageView packageIcon;
        int position;
    }

    public void setupList(ArrayList<PackageInfoHolder> AllPackages) {
        ArrayAdapter<PackageInfoHolder> aa = new ArrayAdapter<PackageInfoHolder>(getBaseContext(), R.layout.package_list_item, AllPackages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.package_list_item, parent);
                }

                PackageInfoHolder pkg = getItem(position);

                ViewHolder holder = new ViewHolder();

                holder.packageLabel = (TextView) convertView.findViewById(R.id.pkg_name);
                holder.packageName = (TextView) convertView.findViewById(R.id.pkg_id);
                holder.packageVersion = (TextView) convertView.findViewById(R.id.pkg_version);
                holder.packageFilePath = (TextView) convertView.findViewById(R.id.pkg_dir);
                holder.packageIcon = (ImageView) convertView.findViewById(R.id.pkg_img);

                convertView.setTag(holder);

                holder.packageLabel.setText(pkg.packageLabel);
                holder.packageName.setText(pkg.packageName);
                holder.packageVersion.setText("version " + pkg.packageVersion);
                holder.packageFilePath.setText(pkg.packageFilePath);

                holder.packageIcon.setImageDrawable(pkg.packageIcon);

                return convertView;
            }
        };
        listView.setAdapter(aa);
        listView.setTextFilterEnabled(true);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final ViewHolder holder = (ViewHolder) view.getTag();

                String myApp = "com.njlabs.tester";
                if (holder.packageName.getText().toString().toLowerCase().contains(myApp.toLowerCase())) {
                    Toast.makeText(getApplicationContext(), "The application " + holder.packageName.getText().toString() + " cannot be decompiled !", Toast.LENGTH_SHORT).show();
                } else {
                    final File JavaOutputDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava" + "/" + holder.packageName.getText().toString() + "/java_output");
                    if (JavaOutputDir.isDirectory()) {

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(AppListing.this, R.style.Theme_AppCompat_Dialog);
                        alertDialog.setTitle("This Package has already been decompiled");
                        alertDialog.setMessage("This application has already been decompiled once and the source exists on your sdcard. What would you like to do ?");
                        alertDialog.setPositiveButton("View Source", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
                                i.putExtra("java_source_dir", JavaOutputDir + "/");
                                i.putExtra("package_id", holder.packageName.getText().toString());
                                startActivity(i);
                            }
                        });

                        alertDialog.setNegativeButton("Decompile", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                JavaOutputDir.delete();
                                Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                                i.putExtra("package_label", holder.packageLabel.getText().toString());
                                i.putExtra("package_file_path", holder.packageFilePath.getText().toString());
                                startActivity(i);
                            }
                        });
                        alertDialog.show();

                    } else {
                        Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                        i.putExtra("package_label", holder.packageLabel.getText().toString());
                        i.putExtra("package_file_path", holder.packageFilePath.getText().toString());
                        startActivity(i);
                    }
                }
            }
        });
    }

    class PackageInfoHolder {
        private String packageLabel = "";
        private String packageName = "";
        private String packageVersion = "";
        private String packageFilePath = "";
        private Drawable packageIcon;

        public String getPackageLabel() {
            return packageLabel;
        }
    }


    private ArrayList<PackageInfoHolder> getInstalledApps(ApplicationLoader task) {
        ArrayList<PackageInfoHolder> res = new ArrayList<>();
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);

        int totalPackages = packages.size();

        for (int i = 0; i < totalPackages; i++) {
            PackageInfo p = packages.get(i);
            if (!isSystemPackage(p)) {
                ApplicationInfo appInfo = null;
                try {
                    appInfo = getPackageManager().getApplicationInfo(p.packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Ln.e(e);
                }
                int count = i + 1;
                int progressVal = (count / totalPackages) * 100;
                final PackageInfoHolder newInfo = new PackageInfoHolder();
                newInfo.packageLabel = p.applicationInfo.loadLabel(getPackageManager()).toString();

                task.doProgress("Loading application " + count + " of " + totalPackages + " (" + newInfo.packageLabel + ")");

                newInfo.packageName = p.packageName;
                newInfo.packageVersion = p.versionName;

                if (appInfo != null) {
                    newInfo.packageFilePath = appInfo.publicSourceDir;
                }

                newInfo.packageIcon = p.applicationInfo.loadIcon(getPackageManager());
                res.add(newInfo);
            }
        }
        Comparator<PackageInfoHolder> AppNameComparator = new Comparator<PackageInfoHolder>() {
            public int compare(PackageInfoHolder o1, PackageInfoHolder o2) {
                return o1.getPackageLabel().compareTo(o2.getPackageLabel());
            }
        };
        Collections.sort(res, AppNameComparator);
        return res;
    }

    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }
}
