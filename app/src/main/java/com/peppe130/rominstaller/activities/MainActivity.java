package com.peppe130.rominstaller.activities;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import java.io.File;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.iconics.IconicsDrawable;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.peppe130.rominstaller.ControlCenter;
import com.peppe130.rominstaller.FragmentsCollector;
import com.peppe130.rominstaller.R;
import com.peppe130.rominstaller.core.Utils;
import com.peppe130.rominstaller.core.CustomViewPager;
import com.peppe130.rominstaller.core.CheckFile;
import com.peppe130.rominstaller.core.CustomFileChooser;
import com.peppe130.rominstaller.core.InstallPopupDialog;
import com.peppe130.rominstaller.core.SystemProperties;
import cn.pedant.SweetAlert.SweetAlertDialog;


@SuppressLint("CommitPrefEdits")
@SuppressWarnings("unused, ResultOfMethodCallIgnored, ConstantConditions")
public class MainActivity extends AppCompatActivity implements CustomFileChooser.FileCallback {

    SharedPreferences SP;
    SharedPreferences.Editor mEditor;
    public static ImageButton NEXT, BACK, DONE;
    public static CustomViewPager mViewPager;
    public static FragmentPagerAdapter mFragmentPagerAdapter;
    public static SmartTabLayout mSmartTabLayout;
    Integer mLatestPage, mCurrentItem, mPosition;
    Boolean mLastPageScrolled = false, mShouldShowToast = false, mNextOrInstallHint = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);

        if (Utils.SHOULD_CLOSE_ACTIVITY) {
            Utils.SHOULD_CLOSE_ACTIVITY = false;
            if (ControlCenter.SHOULD_SHOW_DISCLAIMER_SCREEN) {
                AgreementActivity.mActivity.finish();
            }
        }

        SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mEditor = SP.edit();
        NEXT = (ImageButton) findViewById(R.id.next);
        BACK = (ImageButton) findViewById(R.id.back);
        DONE = (ImageButton) findViewById(R.id.done);
        mViewPager = (CustomViewPager) findViewById(R.id.viewpager);
        mFragmentPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mSmartTabLayout = (SmartTabLayout) findViewById(R.id.viewpager_indicator);
        assert mSmartTabLayout != null;
        mSmartTabLayout.setSelectedIndicatorColors(Utils.FetchAccentColor());

        FragmentsCollector.Setup();

        if (ControlCenter.TEST_MODE) {
            Utils.SHOULD_LOCK_UI = false;
            invalidateOptionsMenu();
            if (ControlCenter.BUTTON_UI) {
                if (mFragmentPagerAdapter.getCount() == 1) {
                    DONE.setVisibility(View.VISIBLE);
                } else {
                    NEXT.setVisibility(View.VISIBLE);
                }
            }
            mViewPager.setAdapter(mFragmentPagerAdapter);
            mSmartTabLayout.setViewPager(mViewPager);
            mEditor.putBoolean("default_options", true).apply();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mViewPager.setAdapter(mFragmentPagerAdapter);
                    mSmartTabLayout.setViewPager(mViewPager);
                }
            }, 200);
        } else {
            if (SP.getBoolean("default_options", true)) {
                mViewPager.removeAllViews();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String mConfirmDevice = (String.format(getString(R.string.detect_device_dialog_message), SystemProperties.get("ro.product.model")));
                        SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText(getString(R.string.detect_device_dialog_title))
                                .setContentText(mConfirmDevice)
                                .setConfirmText(getString(R.string.positive_button))
                                .setCancelText(getString(R.string.list_button))
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        sDialog.dismiss();
                                        Utils.MODEL = SystemProperties.get("ro.product.model");
                                        Setup();
                                    }
                                })
                                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        sweetAlertDialog.dismiss();
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                new MaterialDialog.Builder(MainActivity.this)
                                                        .positiveText(getString(R.string.not_in_list_button))
                                                        .items(ControlCenter.DEVICE_COMPATIBILITY_LIST)
                                                        .itemsCallback(new MaterialDialog.ListCallback() {
                                                            @Override
                                                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                                                Utils.MODEL = String.valueOf(text);
                                                                Setup();
                                                            }
                                                        })
                                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                            @Override
                                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                                new Handler().postDelayed(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(Utils.ACTIVITY, SweetAlertDialog.ERROR_TYPE)
                                                                                .setTitleText(Utils.ACTIVITY.getString(R.string.incompatible_device_dialog_title))
                                                                                .setContentText(Utils.ACTIVITY.getString(R.string.device_not_in_list_dialog_message))
                                                                                .setConfirmText(Utils.ACTIVITY.getString(R.string.close_button))
                                                                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                                                    @Override
                                                                                    public void onClick(SweetAlertDialog sDialog) {
                                                                                        Utils.ACTIVITY.finishAffinity();
                                                                                    }
                                                                                });
                                                                        sweetAlertDialog.setCancelable(false);
                                                                        sweetAlertDialog.show();
                                                                    }
                                                                }, 300);
                                                            }
                                                        })
                                                        .cancelable(false)
                                                        .show();
                                            }
                                        }, 300);
                                    }
                                });
                        sweetAlertDialog.setCancelable(false);
                        sweetAlertDialog.show();
                    }
                }, 1000);
            } else {
                Utils.SHOULD_LOCK_UI = false;
                invalidateOptionsMenu();
                if (ControlCenter.BUTTON_UI) {
                    if (mFragmentPagerAdapter.getCount() == 1) {
                        DONE.setVisibility(View.VISIBLE);
                    } else {
                        NEXT.setVisibility(View.VISIBLE);
                    }
                }
                mViewPager.setAdapter(mFragmentPagerAdapter);
                mSmartTabLayout.setViewPager(mViewPager);
                mEditor.putBoolean("default_options", true).apply();
                Utils.ZIP_FILE = new File(SP.getString("file_path", ""));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mViewPager.setAdapter(mFragmentPagerAdapter);
                        mSmartTabLayout.setViewPager(mViewPager);
                    }
                }, 200);
            }
        }

        NEXT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BACK.setVisibility(View.VISIBLE);

                if (mViewPager.getCurrentItem() != mFragmentPagerAdapter.getCount()) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                }
            }
        });

        NEXT.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Utils.ToastShort(MainActivity.this, getString(R.string.next_button));
                return true;
            }
        });

        BACK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNextOrInstallHint = false;
                DONE.setVisibility(View.GONE);
                NEXT.setVisibility(View.VISIBLE);

                if (mViewPager.getCurrentItem() != 0) {
                    mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                }
            }
        });

        BACK.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Utils.ToastShort(MainActivity.this, getString(R.string.back_button));
                return true;
            }
        });

        DONE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!ControlCenter.TEST_MODE) {
                    ControlCenter.ExportPreferences();
                    getFragmentManager().beginTransaction()
                            .add(new InstallPopupDialog(Utils.ZIP_FILE.toString()), "install_fragment")
                            .commitAllowingStateLoss();
                } else {
                    Utils.ToastShort(MainActivity.this, getString(R.string.can_not_install));
                }
            }
        });

        DONE.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Utils.ToastShort(MainActivity.this, getString(R.string.done_button));
                return true;
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                if (mLatestPage.equals(position)) {
                    if (ControlCenter.BUTTON_UI) {
                        mLastPageScrolled = false;
                        mNextOrInstallHint = true;
                        NEXT.setVisibility(View.GONE);
                        DONE.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mPosition = position;
                mLatestPage = mFragmentPagerAdapter.getCount() - 1;
                if (mViewPager.getCurrentItem() > 0) {
                    setTitle(R.string.toolbar_title2);
                } else {
                    setTitle(R.string.app_name);
                    BACK.setVisibility(View.GONE);
                }
                if (mShouldShowToast && mLatestPage.equals(position)) {
                    if (!ControlCenter.BUTTON_UI) {
                        mShouldShowToast = false;
                        if (!ControlCenter.TEST_MODE) {
                            Utils.ToastShort(MainActivity.this, getString(R.string.swipe_right_to_install));
                        }
                    }
                }
                if (!ControlCenter.BUTTON_UI) {
                    if (mLastPageScrolled && mLatestPage.equals(position)) {
                        mLastPageScrolled = false;
                        if (!ControlCenter.TEST_MODE) {
                            ControlCenter.ExportPreferences();
                            getFragmentManager().beginTransaction()
                                    .add(new InstallPopupDialog(Utils.ZIP_FILE.toString()), "install_fragment")
                                    .commitAllowingStateLoss();
                        } else {
                            Utils.ToastShort(MainActivity.this, getString(R.string.can_not_install));
                        }
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                mLatestPage = mFragmentPagerAdapter.getCount() - 1;
                mCurrentItem = mViewPager.getCurrentItem();
                mLastPageScrolled = mCurrentItem.equals(mLatestPage) && state == 1;
                if (mViewPager.getCurrentItem() == mFragmentPagerAdapter.getCount() -2) {
                    mShouldShowToast = true;
                }
            }
        });

    }

    public void Setup() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText(getString(R.string.set_path_dialog_title))
                        .setContentText(getString(R.string.set_path_dialog_message))
                        .setConfirmText(getString(R.string.ok_button))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Utils.FileChooser();
                                    }
                                }, 300);
                                sDialog.dismissWithAnimation();
                            }
                        });
                sweetAlertDialog.setCancelable(false);
                sweetAlertDialog.show();
            }
        }, 500);
    }

    public static class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return FragmentsCollector.mListFragment.size();
        }

        @Override
        public Fragment getItem(int position) {
            return FragmentsCollector.mListFragment.get(position);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        Utils.ACTIVITY = this;

        File mROMFolder = new File(Environment.getExternalStorageDirectory().getPath() + "/" + getString(R.string.rom_folder));
        File mSampleZIP = new File(mROMFolder.getPath() + "/" + "Sample.zip");

        if(!mROMFolder.exists()) {
            mROMFolder.mkdirs();
        }

        if(ControlCenter.TRIAL_MODE && !mSampleZIP.exists()) {
            Utils.copyAssetFolder(getAssets(), "sample", mROMFolder.toString());
        }

    }

    @Override
    public void onBackPressed() {

        if (ControlCenter.BUTTON_UI && mFragmentPagerAdapter.getCount() != 1) {
            mNextOrInstallHint = false;
            DONE.setVisibility(View.GONE);
            NEXT.setVisibility(View.VISIBLE);
        }

        if (mViewPager.getCurrentItem() == 0 && !Utils.SHOULD_LOCK_UI) {
            SweetAlertDialog mSweetAlertDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.exit_dialog_title))
                    .setContentText(getString(R.string.exit_dialog_message))
                    .showCancelButton(true)
                    .setCancelText(getString(R.string.negative_button))
                    .setConfirmText(getString(R.string.positive_button))
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            finishAffinity();
                        }
                    });
            mSweetAlertDialog.setCanceledOnTouchOutside(true);
            mSweetAlertDialog.show();
        } else if (mViewPager.getCurrentItem() > 0) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }

    }

    @Override
    public void onFileSelection(@NonNull CustomFileChooser dialog, @NonNull final File file) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String mContent = (String.format(getString(R.string.confirm_file_dialog_message), file.getAbsolutePath()));
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText(getString(R.string.confirm_file_dialog_title))
                        .setContentText(mContent)
                        .setConfirmText(getString(R.string.ok_button))
                        .setCancelText(getString(R.string.negative_button))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                Utils.ZIP_FILE = new File(file.getAbsolutePath());
                                Utils.FILE_NAME = file.getName();
                                mEditor.putString("file_path", file.getAbsolutePath()).apply();
                                new CheckFile().execute();
                                sDialog.dismissWithAnimation();
                            }
                        })
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Utils.FileChooser();
                                    }
                                }, 300);
                                sDialog.dismissWithAnimation();
                            }
                        });
                sweetAlertDialog.setCancelable(false);
                sweetAlertDialog.show();
            }
        }, 300);

    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        menu.findItem(R.id.settings).setEnabled(!Utils.SHOULD_LOCK_UI);
        menu.findItem(R.id.changelog).setEnabled(!Utils.SHOULD_LOCK_UI);
        menu.findItem(R.id.default_options).setEnabled(!Utils.SHOULD_LOCK_UI);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        IconicsDrawable mSettingsIcon = new IconicsDrawable(MainActivity.this)
                .icon(ControlCenter.SETTINGS_ICON)
                .actionBar()
                .color(Color.WHITE)
                .sizeDp(30);
        IconicsDrawable mChangelogIcon = new IconicsDrawable(MainActivity.this)
                .icon(ControlCenter.CHANGELOG_ICON)
                .actionBar()
                .color(Color.WHITE)
                .sizeDp(30);
        IconicsDrawable mDefaultOptionsIcon = new IconicsDrawable(MainActivity.this)
                .icon(ControlCenter.DEFAULT_OPTIONS_ICON)
                .actionBar()
                .color(Color.WHITE)
                .sizeDp(35);
        MenuItem mSettings, mChangelog, mDefaultOptions;
        mSettings = menu.findItem(R.id.settings);
        mChangelog = menu.findItem(R.id.changelog);
        mDefaultOptions = menu.findItem(R.id.default_options);
        mSettings.setIcon(mSettingsIcon);
        mChangelog.setIcon(mChangelogIcon);
        mDefaultOptions.setIcon(mDefaultOptionsIcon);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.default_options:
                SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText(getString(R.string.restore_default))
                        .setContentText(getString(R.string.confirm_restore_default))
                        .setConfirmText(getString(R.string.positive_button))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                ControlCenter.DefaultValues();
                                sDialog.setTitleText(getString(R.string.restored_default_options_title))
                                        .setContentText(getString(R.string.restored_default_options_message))
                                        .setConfirmText(getString(R.string.ok_button))
                                        .showCancelButton(false)
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sDialog) {
                                                finishAffinity();
                                                startActivity(getIntent());
                                                mEditor.putBoolean("default_options", false).commit();
                                            }
                                        });
                                sDialog.setCancelable(false);
                                sDialog.setCanceledOnTouchOutside(false);
                                sDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                            }
                        })
                        .setCancelText(getString(R.string.negative_button))
                        .showCancelButton(true);
                sweetAlertDialog.setCanceledOnTouchOutside(true);
                sweetAlertDialog.show();
                break;
            case R.id.changelog:
                startActivity(new Intent(MainActivity.this, ChangelogActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            default:
                break;
        }
        return true;
    }

}