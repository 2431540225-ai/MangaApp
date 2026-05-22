package com.example.mangaapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mangaapp.ui.home.HomeFragment
import com.example.mangaapp.utils.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.mangaapp.ui.list.ListFragment
import com.example.mangaapp.ui.profile.ProfileFragment
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Áp dụng theme dark/light TRƯỚC khi setContentView
        ThemeManager.init(this)
        ThemeManager.applyTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        // Load Home Fragment mặc định
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_list -> {
                    loadFragment(ListFragment())
                    true
                }
                R.id.nav_search -> {
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    /** Dùng để chuyển tab từ bên trong Fragment */
    fun navigateTo(navItemId: Int) {
        bottomNav.selectedItemId = navItemId
    }
}
