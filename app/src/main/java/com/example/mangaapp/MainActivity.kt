package com.example.mangaapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mangaapp.ui.home.HomeFragment
import com.example.mangaapp.utils.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.mangaapp.ui.favorites.FavoritesFragment
import com.example.mangaapp.ui.profile.ProfileFragment

import com.example.mangaapp.ui.list.ListFragment
import com.example.mangaapp.ui.search.SearchFragment

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import androidx.coordinatorlayout.widget.CoordinatorLayout

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // ① Init ThemeManager trước tiên (đọc SharedPreferences)
        ThemeManager.init(this)
        com.example.mangaapp.utils.EventTracker.init(this) // Init EventTracker 4.1
        // ② Áp dụng Night Mode — phải gọi TRƯỚC super.onCreate()
        ThemeManager.applyTheme()

        super.onCreate(savedInstanceState)

        // ③ Chọn đúng theme style theo dark/light
        if (ThemeManager.isDarkMode()) {
            setTheme(R.style.Theme_MangaApp_Dark)
        } else {
            setTheme(R.style.Theme_MangaApp_Light)
        }

        // ④ Khôi phục session user từ SharedPreferences (instant)
        //    Sau đó tự động sync Firestore ở background nếu còn token
        com.example.mangaapp.utils.UserSession.init(this)

        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_list -> { loadFragment(ListFragment()); true }
                R.id.nav_favorites -> { loadFragment(FavoritesFragment()); true }
                R.id.nav_search -> { loadFragment(SearchFragment()); true }
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
        val params = bottomNav.layoutParams as? CoordinatorLayout.LayoutParams
        val behavior = params?.behavior as? HideBottomViewOnScrollBehavior
        behavior?.slideDown(bottomNav)
    }

    // Dùng behavior để slide up (hiện) thay vì VISIBLE
    fun showBottomNav() {
        val params = bottomNav.layoutParams as? CoordinatorLayout.LayoutParams
        val behavior = params?.behavior as? HideBottomViewOnScrollBehavior
        behavior?.slideUp(bottomNav)
    }
}