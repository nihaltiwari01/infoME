package com.mcafirst.infome;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mcafirst.infome.Fragments.FirstFragment;
import com.mcafirst.infome.Fragments.SecondFragment;
import com.mcafirst.infome.Fragments.ThirdFragment;

public class NewActivity extends AppCompatActivity {
    ViewPager2 viewPager;
    BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_nav);

        Fragment[] fragments = new Fragment[] {
                new FirstFragment(),
                new SecondFragment(),
                new ThirdFragment()
        };

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments[position];
            }
            @Override
            public int getItemCount() {
                return fragments.length;
            }
        });
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_first) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.nav_second) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (id == R.id.nav_third) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });


    }
}