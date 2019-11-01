package com.redocs.archive.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import com.redocs.archive.R
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.ui.events.ContextActionRequestEvent
import com.redocs.archive.ui.events.ContextActionStoppedEvent
import com.redocs.archive.ui.utils.ActivityResultSync
import com.redocs.archive.ui.utils.ContextActionSource

class MainActivity : AppCompatActivity(), EventBusSubscriber, ActivityResultSync {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var sideNavView: NavigationView
    private lateinit var appBarConfiguration : AppBarConfiguration

    init {
        subscribe(ContextActionRequestEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is ContextActionRequestEvent -> startActionMode(evt.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val host: NavHostFragment = supportFragmentManager
                .findFragmentById(R.id.host_fragment) as NavHostFragment? ?: return

        // Set up Action Bar
        navController = host.navController

        drawerLayout  = findViewById(R.id.drawer_layout)
        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.home_nav_dest),
                drawerLayout)

        setupActionBar(navController, appBarConfiguration)

        sideNavView=findViewById(R.id.nav_view)
        setupNavigationMenu(navController,sideNavView)
        //setupBottomNavMenu(navController)

        /*navController.addOnDestinationChangedListener { _, destination, _ ->
            val dest: String = try {
                resources.getResourceName(destination.id)
            } catch (e: Resources.NotFoundException) {
                Integer.toString(destination.id)
            }

            Toast.makeText(this@MainActivity, "Navigated to $dest",
                    Toast.LENGTH_SHORT).show()
            Log.d("NavigationActivity", "Navigated to $dest")
        }*/
    }

    /*private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav?.setupWithNavController(navController)
    }*/

    private fun setupNavigationMenu(navController: NavController, sideNavView:NavigationView) {
        /*In split screen mode, you can drag this view out from the left
            This does NOT modify the actionbar*/
        sideNavView.setupWithNavController(navController)
    }

    private fun setupActionBar(navController: NavController,
                               appBarConfig : AppBarConfiguration) {

        /*This allows NavigationUI to decide what label to show in the action bar
        By using appBarConfig, it will also determine whether to
            show the up arrow or drawer menu icon*/
        setupActionBarWithNavController(navController, appBarConfig)

    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val retValue = super.onCreateOptionsMenu(menu)
        //val navigationView = findViewById<NavigationView>(R.id.nav_view)
        //The NavigationView already has these same navigation items, so we only addChildren
        //    navigation items to the menu here if there isn't a NavigationView
        //if (navigationView == null) {
            menuInflater.inflate(R.menu.overflow_menu, menu)
            return true
        //}
        //return retValue
    }*/

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /*Have the NavigationUI look for an action or destination matching the menu
            item id and navigate there if found.
        Otherwise, bubble up to the parent.*/
        return item.onNavDestinationSelected(navController)
                || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        /*Allows NavigationUI to support proper up navigation or the drawer layout
            drawer menu, depending on the situation*/
        return navController.navigateUp(appBarConfiguration)
    }

    private var isNavMenuLocked: Boolean = false
    set(value){
        drawerLayout.setDrawerLockMode(
            if(value)
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            else
                DrawerLayout.LOCK_MODE_UNLOCKED)
        field = value
    }

    private fun startActionMode(source: ContextActionSource) {

        isNavMenuLocked = true

        startSupportActionMode(
            object : ActionMode.Callback {

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem?): Boolean =
                    source.onContextMenuItemClick(mode, item)

                override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {

                    if (menu != null) {
                        source.createContextActionMenu(mode,mode.menuInflater,menu)
                        return true
                    }
                    return false
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean = false

                override fun onDestroyActionMode(mode: ActionMode) {
                    isNavMenuLocked = false
                    source.onDestroyContextAction()
                    EventBus.publish(ContextActionStoppedEvent(source))
                }
            })
    }

    override fun onBackPressed() {

        if((supportFragmentManager
                .fragments[0]
                    .childFragmentManager
                        .fragments[0]as? BackButtonInterceptor)?.
                            onBackPressed() != true)
            super.onBackPressed()
    }

    private var listener: (requestCode: Int, resultCode: Int, data: Intent?)->Unit = {_, _, _ ->  }
    override fun listen(listener: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit) {
        this.listener = listener
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        listener(requestCode, resultCode, data)
    }
}

/*interface ContextActionModeController {
    fun startActionMode(source: ContextActionSource)
}*/

interface BackButtonInterceptor {
    fun onBackPressed(): Boolean
}