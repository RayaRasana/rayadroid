package dts.rayafile.com.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import dts.rayafile.com.R;
import dts.rayafile.com.ui.base.fragment.BaseFragment;
import dts.rayafile.com.databinding.FragmentActivityBinding;
import dts.rayafile.com.ui.adapter.ViewPagerAdapter;

public class ActivityContainerFragment extends BaseFragment {

    private FragmentActivityBinding binding;

    public static ActivityContainerFragment newInstance() {

        Bundle args = new Bundle();

        ActivityContainerFragment fragment = new ActivityContainerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTabLayout();

        initViewPager();
    }

    private void initTabLayout() {
        binding.tabs.setTabIndicatorAnimationMode(TabLayout.INDICATOR_ANIMATION_MODE_ELASTIC);
        binding.tabs.setSelectedTabIndicator(R.drawable.cat_tabs_rounded_line_indicator);
        binding.tabs.setTabIndicatorFullWidth(false);
        binding.tabs.setTabGravity(TabLayout.GRAVITY_START);
    }

    private void initViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager(), getLifecycle());
        adapter.addFragment(AllActivitiesFragment.newInstance());
        adapter.addFragment(MineActivitiesFragment.newInstance());
        binding.viewPager.setAdapter(adapter);

        String[] tabArray = getResources().getStringArray(R.array.activity_fragment_titles);
        for (String s : tabArray) {
            new TabLayoutMediator(binding.tabs, binding.viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    tab.setText(s);
                }
            }).attach();
        }
    }
}
