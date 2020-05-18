package com.cnam.greta.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.cnam.greta.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setCustomLocationState();
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        setCustomLocationState();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void setCustomLocationState(){
        boolean isAutomatic = getPreferenceScreen().getSharedPreferences().getBoolean(
                requireActivity().getString(R.string.localisation_automatic_key),
                true
        );
        getPreferenceScreen().findPreference(requireActivity().getString(R.string.localisation_provider_key)).setEnabled(!isAutomatic);
        getPreferenceScreen().findPreference(requireActivity().getString(R.string.localisation_update_time_frequency_key)).setEnabled(!isAutomatic);
        getPreferenceScreen().findPreference(requireActivity().getString(R.string.localisation_update_distance_frequency_key)).setEnabled(!isAutomatic);
    }
}
