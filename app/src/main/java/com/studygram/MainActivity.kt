package com.studygram

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.studygram.databinding.ActivityMainBinding
import com.studygram.utils.NetworkManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var networkManager: NetworkManager
    private var offlineSnackbar: Snackbar? = null

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
                R.id.registerFragment,
                R.id.feedFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        setupBottomNavigation()
        setupNetworkMonitoring()
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
                    // TODO: Navigate to My Content when fragment is created
                    true
                }
                R.id.navigation_create -> {
                    // TODO: Navigate to Create Post when fragment is created
                    true
                }
                R.id.navigation_profile -> {
                    // TODO: Navigate to Profile when fragment is created
                    true
                }
                else -> false
            }
        }

        // Show/hide bottom navigation and action bar based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.feedFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    supportActionBar?.show()
                    binding.bottomNavigation.selectedItemId = R.id.navigation_feed
                }
                R.id.loginFragment,
                R.id.registerFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    supportActionBar?.hide()
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                    supportActionBar?.show()
                }
            }
        }
    }

    private fun setupNetworkMonitoring() {
        networkManager = NetworkManager(this)
        networkManager.observe(this) { isConnected ->
            if (isConnected) {
                // Network is available
                offlineSnackbar?.dismiss()
                offlineSnackbar = null
            } else {
                // Network is unavailable
                showOfflineIndicator()
            }
        }
    }

    private fun showOfflineIndicator() {
        offlineSnackbar = Snackbar.make(
            binding.root,
            "No internet connection. Viewing cached content.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("OK") { dismiss() }
            show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
