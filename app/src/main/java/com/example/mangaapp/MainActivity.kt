package com.example.mangaapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mangaapp.ui.home.HomeFragment
import com.example.mangaapp.utils.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.mangaapp.ui.list.ListFragment
import com.example.mangaapp.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // ① Init ThemeManager trước tiên (đọc SharedPreferences)
        ThemeManager.init(this)
        // ② Áp dụng Night Mode — phải gọi TRƯỚC super.onCreate()
        ThemeManager.applyTheme()

        super.onCreate(savedInstanceState)

        // ③ Chọn đúng theme style theo dark/light
        if (ThemeManager.isDarkMode()) {
            setTheme(R.style.Theme_MangaApp_Dark)
        } else {
            setTheme(R.style.Theme_MangaApp_Light)
        }

        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_list -> { loadFragment(ListFragment()); true }
                R.id.nav_search -> { true }
                R.id.nav_profile -> { loadFragment(ProfileFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    fun navigateTo(navItemId: Int) {
        bottomNav.selectedItemId = navItemId
    }

    fun hideBottomNav() {
        bottomNav.visibility = android.view.View.GONE
    }

    fun showBottomNav() {
        bottomNav.visibility = android.view.View.VISIBLE
    }
}