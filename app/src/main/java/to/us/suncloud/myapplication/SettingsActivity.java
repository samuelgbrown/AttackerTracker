package to.us.suncloud.myapplication;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the theme here
        setTheme(PrefsHelper.getTheme(getApplicationContext()));

        setContentView(R.layout.activity_settings);
        Bundle args = getIntent().getExtras();

        // Instantiate the Settings Fragment
        SettingsFragment settingsFrag = new SettingsFragment();
        settingsFrag.setArguments(args);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, settingsFrag)
                .commit();
    }
}
