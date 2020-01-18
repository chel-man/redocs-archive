package com.redocs.archive.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.R
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.localeManager
import com.redocs.archive.ui.events.ContextActionRequestEvent
import com.redocs.archive.ui.events.ContextActionStoppedEvent
import com.redocs.archive.ui.utils.ActivityResultSync
import com.redocs.archive.ui.utils.ContextActionSource
import com.redocs.archive.ui.utils.showError
import kotlinx.android.synthetic.main.main_activity.*
import java.util.*

class MainActivity : AppCompatActivity(), EventBusSubscriber, ActivityResultSync {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration : AppBarConfiguration

    init {
        subscribe(ContextActionRequestEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is ContextActionRequestEvent -> startActionMode(evt.data)
        }
    }

    override fun onStart() {
        super.onStart()
        if(ArchiveApplication.filesDir == null)
            ArchiveApplication.filesDir = filesDir.canonicalPath
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(localeManager.getLocalizedContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("#MA","CREATED")
        setContentView(R.layout.main_activity)

        val toolbar = toolbar
        setSupportActionBar(toolbar)

        val host: NavHostFragment = host_fragment as NavHostFragment? ?: return

        // Set up Action Bar
        navController = host.navController

        drawerLayout  = drawer_layout
        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.home_nav_dest,R.id.login_nav_dest),
                drawerLayout)

        setupActionBar(navController, appBarConfiguration)
        setupNavigationMenu(navController,nav_view)
        //setupBottomNavMenu(navController)
        if(!isServiceUrlSettedUp) {
            Handler().post {
                startSettingsActivity()
                Handler().post {
                    showError(this,"setup service url first")
                }
            }
        }
        else
            ArchiveApplication.baseUrl = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsFragment.SERVICE_URL_KEY, null)

        if(savedInstanceState == null) {
            ArchiveApplication.setup()
        }
    }

    /*private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNav?.setupWithNavController(navController)
    }*/

    private var isLoggedIn = false

    private fun setupNavigationMenu(navController: NavController, sideNavView:NavigationView) {
        /*In split screen mode, you can drag this view out from the left
            This does NOT modify the actionbar*/
        sideNavView.menu.findItem(R.id.settings_menu).setOnMenuItemClickListener {
            drawerLayout.closeDrawers()
            startSettingsActivity()
            true
        }

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if(destination.id != R.id.login_nav_dest){
                if(!isLoggedIn) {
                    isLoggedIn = true
                    for(mi in sideNavView.menu)
                        mi.isEnabled = true
                }
            }
        }
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

    private fun startSettingsActivity(){
        preferencesActivityRequest = Math.random().toInt() and 0xFF
        startActivityForResult(
            Intent(this,SettingsActivity::class.java),
            preferencesActivityRequest)
    }

    private val isServiceUrlSettedUp: Boolean
        get()=
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsFragment.SERVICE_URL_KEY, null) != null


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

    private var pressed = 0

    override fun onBackPressed() {

        var (backStackEntryCount,processed) = traverseChildFragments(supportFragmentManager)
        if(processed)
            return

        if(backStackEntryCount == 0 && pressed==0) {
            showError(this, resources.getString(R.string.exit_app_by_back_button))
            Handler().postDelayed({
                pressed = 0
            },2000)
            pressed++
        }
        else
            super.onBackPressed()
    }

    private var listener: (requestCode: Int, resultCode: Int, data: Intent?)->Unit = {_, _, _ ->  }

    override fun setActivityResultListener(listener: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit) {
        this.listener = listener
    }

    private var  preferencesActivityRequest = -1234321

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == preferencesActivityRequest) {
            if (resultCode == Activity.RESULT_OK)
                preferencesChanged(data)
        }
        else
            listener(requestCode, resultCode, data)
    }

    private fun preferencesChanged(data: Intent?) {
        if(data?.getBooleanExtra(SettingsActivity.RESTART_KEY,false) == true)
            recreate()
    }

}

private fun traverseChildFragments(fm: FragmentManager): Pair<Int,Boolean>{
    var bces = fm.backStackEntryCount
    for(f in fm.fragments){
        val (fbses,fproc) = traverseChildFragments(f.childFragmentManager)
        if(fproc)
            return 0 to true
        bces += fbses
    }
    return bces to false
}

/*interface ContextActionModeController {
    fun startActionMode(source: ContextActionSource)
}*/

interface BackButtonInterceptor {
    fun onBackPressed(): Boolean
}
