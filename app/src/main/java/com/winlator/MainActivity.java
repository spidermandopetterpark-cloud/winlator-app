package com.winlator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

import com.winlator.contentdialog.AboutDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.LocaleHelper;
import com.winlator.core.PreloaderDialog;
import com.winlator.xenvironment.RootFSInstaller;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "WinlatorMainActivity";

    /*
     * ============================================================
     * CONFIGURAÇÃO PRINCIPAL
     * ============================================================
     */

    public static final boolean DEBUG_MODE = false;

    public static final @IntRange(from = 1, to = 19)
    byte CONTAINER_PATTERN_COMPRESSION_LEVEL = 9;

    /*
     * ============================================================
     * REQUEST CODES
     * ============================================================
     */

    public static final byte PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    public static final byte OPEN_FILE_REQUEST_CODE = 2;
    public static final byte EDIT_INPUT_CONTROLS_REQUEST_CODE = 3;
    public static final byte OPEN_DIRECTORY_REQUEST_CODE = 4;

    /*
     * ============================================================
     * UI
     * ============================================================
     */

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBar actionBar;

    /*
     * ============================================================
     * WINLATOR
     * ============================================================
     */

    public final PreloaderDialog preloaderDialog =
            new PreloaderDialog(this);

    private boolean editInputControls = false;
    private int selectedProfileId;

    private Callback<Uri> openFileCallback;

    private SharedPreferences preferences;

    private Fragment currentFragment;

    /*
     * ============================================================
     * MEMORY MANAGER
     * ============================================================
     *
     * O MemoryManager fica separado do Activity.
     *
     * MainActivity -> MemoryManager -> JNI -> C++
     *
     * AndroidMemory.cpp / WineMemory.cpp NÃO devem ser colocados
     * dentro deste arquivo.
     */

    private MemoryManager memoryManager;

    /*
     * ============================================================
     * ACTIVITY
     * ============================================================
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppUtils.setActivityTheme(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        /*
         * --------------------------------------------------------
         * Inicializa o gerenciador de memória
         * --------------------------------------------------------
         */

        initializeMemoryManager();

        /*
         * --------------------------------------------------------
         * Drawer
         * --------------------------------------------------------
         */

        drawerLayout = findViewById(R.id.DrawerLayout);

        navigationView = findViewById(R.id.NavigationView);

        navigationView.setNavigationItemSelectedListener(this);

        /*
         * --------------------------------------------------------
         * Toolbar
         * --------------------------------------------------------
         */

        setSupportActionBar(findViewById(R.id.Toolbar));

        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        /*
         * --------------------------------------------------------
         * Preferences
         * --------------------------------------------------------
         */

        preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * --------------------------------------------------------
         * Intent
         * --------------------------------------------------------
         */

        Intent intent = getIntent();

        editInputControls =
                intent.getBooleanExtra(
                        "edit_input_controls",
                        false
                );

        /*
         * ========================================================
         * MODO EDIÇÃO DOS CONTROLES
         * ========================================================
         */

        if (editInputControls) {

            selectedProfileId =
                    intent.getIntExtra(
                            "selected_profile_id",
                            0
                    );

            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(
                        R.drawable.icon_action_bar_back
                );
            }

            MenuItem inputControlsItem =
                    navigationView.getMenu().findItem(
                            R.id.menu_item_input_controls
                    );

            if (inputControlsItem != null) {

                onNavigationItemSelected(
                        inputControlsItem
                );

                navigationView.setCheckedItem(
                        R.id.menu_item_input_controls
                );
            }

        }

        /*
         * ========================================================
         * MODO NORMAL
         * ========================================================
         */

        else {

            boolean showShortcutsFirst =
                    preferences.getBoolean(
                            "show_shortcuts_first",
                            false
                    );

            int selectedMenuItemId =
                    intent.getIntExtra(
                            "selected_menu_item_id",
                            0
                    );

            int menuItemId;

            if (selectedMenuItemId > 0) {

                menuItemId = selectedMenuItemId;

            }
            else {

                menuItemId =
                        showShortcutsFirst
                                ? R.id.menu_item_shortcuts
                                : R.id.menu_item_containers;
            }

            if (actionBar != null) {

                actionBar.setHomeAsUpIndicator(
                        R.drawable.icon_action_bar_menu
                );
            }

            MenuItem initialItem =
                    navigationView.getMenu().findItem(
                            menuItemId
                    );

            if (initialItem != null) {

                onNavigationItemSelected(
                        initialItem
                );

                navigationView.setCheckedItem(
                        menuItemId
                );
            }

            /*
             * ----------------------------------------------------
             * Permissões / RootFS
             * ----------------------------------------------------
             */

            if (!requestAppPermissions()) {

                RootFSInstaller.installIfNeeded(this);
            }

            /*
             * ----------------------------------------------------
             * Abrir container específico
             * ----------------------------------------------------
             */

            int containerId =
                    intent.getIntExtra(
                            "container_id",
                            0
                    );

            String startPath =
                    intent.getStringExtra(
                            "start_path"
                    );

            if (containerId > 0 && startPath != null) {

                showFragment(
                        new ContainerFileManagerFragment(
                                containerId,
                                startPath
                        )
                );
            }
        }

        /*
         * ========================================================
         * DEBUG
         * ========================================================
         */

        if (DEBUG_MODE) {

            Log.d(
                    TAG,
                    "Winlator MainActivity iniciado"
            );

            logMemoryInformation();
        }
    }

    /*
     * ============================================================
     * MEMORY MANAGER
     * ============================================================
     */

    private void initializeMemoryManager() {

        try {

            memoryManager =
                    new MemoryManager(this);

            /*
             * Inicializa o backend Java/JNI.
             */

            boolean initialized =
                    memoryManager.initialize();

            if (DEBUG_MODE) {

                Log.d(
                        TAG,
                        "MemoryManager initialized: "
                                + initialized
                );
            }

        }
        catch (Throwable e) {

            /*
             * Não impedir o Winlator de iniciar caso o backend
             * de memória ainda não esteja disponível.
             */

            memoryManager = null;

            Log.e(
                    TAG,
                    "Falha ao inicializar MemoryManager",
                    e
            );
        }
    }

    /*
     * ============================================================
     * INFORMAÇÕES DE MEMÓRIA
     * ============================================================
     */

    private void logMemoryInformation() {

        if (memoryManager == null) {
            return;
        }

        try {

            long ram =
                    memoryManager.getRamSize();

            long swap =
                    memoryManager.getSwapSize();

            long available =
                    memoryManager.getAvailableMemory();

            Log.d(
                    TAG,
                    "RAM: " + ram
                            + " bytes"
            );

            Log.d(
                    TAG,
                    "SWAP: " + swap
                            + " bytes"
            );

            Log.d(
                    TAG,
                    "Available: " + available
                            + " bytes"
            );

        }
        catch (Throwable e) {

            Log.e(
                    TAG,
                    "Erro ao obter informações de memória",
                    e
            );
        }
    }

    /*
     * ============================================================
     * ACESSO AO MEMORY MANAGER
     * ============================================================
     */

    @Nullable
    public MemoryManager getMemoryManager() {

        return memoryManager;
    }

    /*
     * ============================================================
     * CONTEXT
     * ============================================================
     */

    @Override
    protected void attachBaseContext(Context newBase) {

        super.attachBaseContext(
                LocaleHelper.setSystemLocale(newBase)
        );
    }

    /*
     * ============================================================
     * PERMISSIONS
     * ============================================================
     */

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {

            if (grantResults.length > 0
                    && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {

                RootFSInstaller.installIfNeeded(this);

            }
            else {

                finish();
            }
        }
    }

    /*
     * ============================================================
     * FILE RESULT
     * ============================================================
     */

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode ==
                MainActivity.OPEN_FILE_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {

            if (openFileCallback != null
                    && data != null
                    && data.getData() != null) {

                openFileCallback.call(
                        data.getData()
                );

                openFileCallback = null;
            }
        }
    }

    /*
     * ============================================================
     * CONFIGURATION
     * ============================================================
     */

    @Override
    public void onConfigurationChanged(
            @NonNull Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        if ((newConfig.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
                ||
                newConfig.orientation ==
                Configuration.ORIENTATION_PORTRAIT)
                &&
                currentFragment instanceof
                        BaseFileManagerFragment) {

            ((BaseFileManagerFragment)
                    currentFragment)
                    .onOrientationChanged();
        }
    }

    /*
     * ============================================================
     * BACK
     * ============================================================
     */

    @Override
    public void onBackPressed() {

        if (currentFragment != null
                && currentFragment.isVisible()) {

            if (currentFragment instanceof
                    BaseFileManagerFragment) {

                BaseFileManagerFragment
                        fileManagerFragment =
                        (BaseFileManagerFragment)
                                currentFragment;

                if (fileManagerFragment.onBackPressed()) {
                    return;
                }
            }

            else if (currentFragment instanceof
                    ContainersFragment) {

                finish();

                return;
            }
        }

        showFragment(
                new ContainersFragment()
        );
    }

    /*
     * ============================================================
     * FILE CALLBACK
     * ============================================================
     */

    public void setOpenFileCallback(
            Callback<Uri> openFileCallback) {

        this.openFileCallback =
                openFileCallback;
    }

    /*
     * ============================================================
     * PERMISSION CHECK
     * ============================================================
     */

    private boolean requestAppPermissions() {

        boolean writePermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED;

        boolean readPermission =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED;

        if (writePermission && readPermission) {
            return false;
        }

        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE
        );

        return true;
    }

    /*
     * ============================================================
     * TOOLBAR
     * ============================================================
     */

    @Override
    public boolean onOptionsItemSelected(
            MenuItem menuItem) {

        int itemId =
                menuItem.getItemId();

        if (itemId == R.id.menu_item_add
                || itemId == R.id.menu_item_home
                || itemId == R.id.menu_item_view_style
                || itemId == R.id.menu_item_new_folder) {

            return super.onOptionsItemSelected(
                    menuItem
            );
        }

        /*
         * --------------------------------------------------------
         * Modo de edição
         * --------------------------------------------------------
         */

        if (editInputControls) {

            setResult(RESULT_OK);

            finish();

            return true;
        }

        /*
         * --------------------------------------------------------
         * File Manager
         * --------------------------------------------------------
         */

        if (currentFragment instanceof
                BaseFileManagerFragment) {

            BaseFileManagerFragment
                    fileManagerFragment =
                    (BaseFileManagerFragment)
                            currentFragment;

            if (fileManagerFragment
                    .onOptionsMenuClicked()) {

                return true;
            }
        }

        /*
         * --------------------------------------------------------
         * Drawer
         * --------------------------------------------------------
         */

        if (drawerLayout != null) {

            drawerLayout.openDrawer(
                    GravityCompat.START
            );
        }

        return true;
    }

    /*
     * ============================================================
     * NAVIGATION DRAWER
     * ============================================================
     */

    @Override
    public boolean onNavigationItemSelected(
            @NonNull MenuItem item) {

        FragmentManager fragmentManager =
                getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount()
                > 0) {

            fragmentManager.popBackStack(
                    null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
            );
        }

        int itemId =
                item.getItemId();

        switch (itemId) {

            /*
             * ----------------------------------------------------
             * SHORTCUTS
             * ----------------------------------------------------
             */

            case R.id.menu_item_shortcuts:

                preferences.edit()
                        .putBoolean(
                                "show_shortcuts_first",
                                true
                        )
                        .apply();

                showFragment(
                        new ShortcutsFragment()
                );

                break;

            /*
             * ----------------------------------------------------
             * CONTAINERS
             * ----------------------------------------------------
             */

            case R.id.menu_item_containers:

                preferences.edit()
                        .putBoolean(
                                "show_shortcuts_first",
                                false
                        )
                        .apply();

                showFragment(
                        new ContainersFragment()
                );

                break;

            /*
             * ----------------------------------------------------
             * INPUT CONTROLS
             * ----------------------------------------------------
             */

            case R.id.menu_item_input_controls:

                showFragment(
                        new InputControlsFragment(
                                selectedProfileId
                        )
                );

                break;

            /*
             * ----------------------------------------------------
             * SETTINGS
             * ----------------------------------------------------
             */

            case R.id.menu_item_settings:

                showFragment(
                        new SettingsFragment()
                );

                break;

            /*
             * ----------------------------------------------------
             * ABOUT
             * ----------------------------------------------------
             */

            case R.id.menu_item_about:

                new AboutDialog(this).show();

                break;
        }

        return true;
    }

    /*
     * ============================================================
     * FRAGMENT
     * ============================================================
     */

    public void showFragment(
            Fragment fragment) {

        if (fragment == null) {
            return;
        }

        FragmentManager fragmentManager =
                getSupportFragmentManager();

        fragmentManager.beginTransaction()
                .replace(
                        R.id.FLFragmentContainer,
                        fragment
                )
                .commit();

        if (drawerLayout != null) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }

        currentFragment =
                fragment;
    }

    /*
     * ============================================================
     * LIFECYCLE
     * ============================================================
     */

    @Override
    protected void onDestroy() {

        /*
         * Não destruir manualmente o JNI aqui.
         *
         * O MemoryManager deve cuidar do próprio ciclo de vida
         * caso seja necessário.
         */

        memoryManager = null;

        currentFragment = null;

        openFileCallback = null;

        super.onDestroy();
    }
                    }
