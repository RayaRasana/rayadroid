package dts.rayafile.com.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.BuildCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.blankj.utilcode.util.FragmentUtils;
import dts.rayafile.com.R;
import dts.rayafile.com.databinding.SettingsActivityLayoutBinding;
import dts.rayafile.com.ui.base.BaseActivity;

@Deprecated
public class SettingsActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private SettingsActivityLayoutBinding binding;
    private SettingsActivityViewModel viewModel;

    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = SettingsActivityLayoutBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        FragmentUtils.add(getSupportFragmentManager(), SettingsFragment.newInstance(), R.id.settings_fragment_container);

        Toolbar toolbar = getActionBarToolbar();
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        binding.swipeRefreshLayout.setEnabled(false);

        viewModel = new ViewModelProvider(this).get(SettingsActivityViewModel.class);
        viewModel.getRefreshLiveData().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                binding.swipeRefreshLayout.setRefreshing(aBoolean);
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        final Bundle args = pref.getExtras();
        String f = pref.getFragment();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), f);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_fragment_container, fragment).addToBackStack(null).commit();
        return true;
    }
}