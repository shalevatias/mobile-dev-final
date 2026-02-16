package com.studygram

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
<<<<<<< Updated upstream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
=======
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.studygram.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.loginFragment,
                R.id.feedFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_feed -> {
                    if (navController.currentDestination?.id != R.id.feedFragment) {
                        navController.navigate(R.id.feedFragment)
                    }
                    true
                }
                R.id.navigation_my_content -> {
                    if (navController.currentDestination?.id != R.id.myContentFragment) {
                        navController.navigate(R.id.myContentFragment)
                    }
                    true
                }
                R.id.navigation_create -> {
                    navController.navigate(R.id.createPostFragment)
                    true
                }
                R.id.navigation_profile -> {
                    if (navController.currentDestination?.id != R.id.profileFragment) {
                        navController.navigate(R.id.profileFragment)
                    }
                    true
                }
                else -> false
            }
        }

        // Show/hide bottom navigation based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.feedFragment,
                R.id.myContentFragment,
                R.id.profileFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }

            // Update selected item in bottom navigation
            when (destination.id) {
                R.id.feedFragment -> binding.bottomNavigation.selectedItemId = R.id.navigation_feed
                R.id.myContentFragment -> binding.bottomNavigation.selectedItemId = R.id.navigation_my_content
                R.id.profileFragment -> binding.bottomNavigation.selectedItemId = R.id.navigation_profile
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
>>>>>>> Stashed changes
    }
}
