package com.qadomy.eatitserver

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.eventbus.CategoryClick
import com.qadomy.eatitserver.eventbus.ChangeMenuClick
import com.qadomy.eatitserver.eventbus.ToastEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView

    private var menuClick: Int = -1

    // onStart register event bust when start app
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    // onStop unregister event bus when stop app
    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // subscribe To Topic
        subscribeToTopic(Common.getNewOrderTopic())

        // init drawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)

        // init navView
        navView = findViewById(R.id.nav_view)

        // init mavController
        navController = findNavController(R.id.nav_host_fragment)


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_category, R.id.nav_food_list, R.id.nav_order, R.id.nav_shipper
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        // when click on items in menu
        navView.run {

            // when click on items in menu
            setNavigationItemSelectedListener { item ->
                item.isChecked = true
                drawerLayout!!.closeDrawers() // Close all currently open drawer views by animating them out of view.


                when (item.itemId) {
                    R.id.nav_sign_out -> {
                        // when click on sign out menu
                        signOut()
                    }
                    R.id.nav_category -> {
                        if (menuClick != item.itemId) {
                            navController.popBackStack() // clear back stack
                            navController.navigate(R.id.nav_category)
                        }
                    }
                    R.id.nav_order -> {
                        if (menuClick != item.itemId) {
                            navController.popBackStack() // clear back stack
                            navController.navigate(R.id.nav_order)
                        }
                    }
                    R.id.nav_shipper -> {
                        if (menuClick != item.itemId) {
                            navController.popBackStack() // clear back stack
                            navController.navigate(R.id.nav_shipper)
                        }
                    }

                }

                /** we save item id if user click on menu */
                menuClick = item!!.itemId

                true
            }
        }

        // view
        val headerView = navView.getHeaderView(0)
        val textUser = headerView.findViewById<View>(R.id.txt_user) as TextView
        Common.setSpanString("Hey Mr.", Common.currentServerUser!!.name, textUser)


        menuClick = R.id.nav_category // default

    }// end onCreate


    // method to subscribe to topic because in client app we send notification to topic
    private fun subscribeToTopic(newOrderTopic: String) {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(newOrderTopic)
            .addOnFailureListener { message ->
                Toast.makeText(
                    this@HomeActivity,
                    "" + message.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task ->
                if (!task.isSuccessful)
                    Toast.makeText(this@HomeActivity, "Subscribe topic failed", Toast.LENGTH_SHORT)
                        .show()
            }
    }

    // function when click on sign out menu
    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Sign out")
            .setMessage("Are You Sure Want Exit?")
            .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton("OK") { _, _ ->
                // when click on ok for sign out

                Common.FOOD_SELECTED = null
                Common.CATEGORY_SELECTED = null
                Common.currentServerUser = null

                // sign out form firebase
                FirebaseAuth.getInstance().signOut()

                // make intent to main activity "Register"
                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()


            }

        val dialog = builder.create()
        dialog.show()
    }


    /**
     *
     * for Menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }


    /**
     *
     * For fragment navigation
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    /**
     *
     * Event Bus
     */

    // event when click on category item to navigate to food list screen
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategoryClick(event: CategoryClick) {
        if (event.isSuccess) {
            if (menuClick != R.id.nav_food_list) {
                navController!!.navigate(R.id.nav_food_list)
                menuClick = R.id.nav_food_list
            }
        }
    }

    // event to change menu of items "check if come from food list fragment "
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun oonChangeMenu(event: ChangeMenuClick) {
        if (!event.isFromFoodList) {

            //clear
            navController!!.popBackStack(R.id.nav_category, true)
            navController!!.navigate(R.id.nav_category)
        }

        menuClick = -1
    }

    // event to check Toast message come from (update in category fragment) or from food list fragment
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onToastEvent(event: ToastEvent) {
        if (event.isUpdate) {
            Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Delete success", Toast.LENGTH_SHORT).show()
        }

        EventBus.getDefault().postSticky(ChangeMenuClick(event.isBackFromFoodList))
    }

}
