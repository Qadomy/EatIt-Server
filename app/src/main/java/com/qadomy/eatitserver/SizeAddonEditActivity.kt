package com.qadomy.eatitserver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.qadomy.eatitserver.adapter.MySizeAdapter
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.eventbus.AddonSizeEditEvent
import com.qadomy.eatitserver.eventbus.SelectSizeModel
import com.qadomy.eatitserver.eventbus.UpdateSizeModel
import com.qadomy.eatitserver.model.SizeModel
import kotlinx.android.synthetic.main.activity_size_addon_edit.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SizeAddonEditActivity : AppCompatActivity() {


    var adapter: MySizeAdapter? = null
    private var foodEditPosition = -1
    private var needSave = false
    private var isAddon = false

    // onStart, register event bus
    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    // onStop, remove event
    override fun onStop() {
        EventBus.getDefault().removeStickyEvent(UpdateSizeModel::class.java)
        super.onStop()
    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_size_addon_edit)

        // init
        init()
    }

    private fun init() {
        setSupportActionBar(tool_bar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) // will make the icon clickable and add the < at the left of the icon.
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        recycler_addon_size.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        recycler_addon_size!!.layoutManager = layoutManager
        recycler_addon_size.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )

        // Event, when click on create button
        btn_create.setOnClickListener {

            if (!isAddon) { // mean change in size
                if (adapter != null) {
                    val sizeModel = SizeModel()

                    sizeModel.name = edt_name.text.toString()
                    sizeModel.price = edt_price.text.toString().toLong()

                    adapter!!.addNewSize(sizeModel)
                }
            } else {    // mean change in addon

                //todo: late
            }
        }

        // Event, when click on edit button
        btn_edit.setOnClickListener {
            if (!isAddon) { // mean size
                if (adapter != null) {

                    val sizeModel = SizeModel()
                    sizeModel.name = edt_name.text.toString()
                    sizeModel.price = edt_price.text.toString().toLong()

                    adapter!!.editSize(sizeModel)

                }

            } else { // mean addon

            }
        }

    }


    /**
     *
     *  Save menu
     */

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_size_addon, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_save -> saveData() // save data
            android.R.id.home -> {

                if (needSave) {
                    val builder = AlertDialog.Builder(this)
                        .setTitle("Cancel?")
                        .setMessage("Do you really want to close without saving?")
                        .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .setPositiveButton("OK") { _, _ ->

                            needSave = false

                            // after click ok, it will close activity
                            closeActivity()

                        }

                    val dialog = builder.create() // create dialog
                    dialog.show() // display dialog
                }
            }


        }

        return true
    }


    // function for close activity after click ok
    private fun closeActivity() {
        edt_name.setText("")
        edt_price.setText("0")

        finish() // activity is done and should be closed
    }

    // function for save data in database firebase when click save button
    private fun saveData() {
        if (foodEditPosition != -1) {

            Common.CATEGORY_SELECTED!!.foods?.set(foodEditPosition, Common.FOOD_SELECTED!!)
            val updateData: MutableMap<String, Any> = HashMap()

            updateData["foods"] = Common.CATEGORY_SELECTED!!.foods!!

            FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
                .child(Common.CATEGORY_SELECTED!!.menuId!!)
                .updateChildren(updateData)
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(this@SizeAddonEditActivity, "" + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@SizeAddonEditActivity,
                            "Reload success",
                            Toast.LENGTH_SHORT
                        )
                            .show()

                        needSave = false
                        edt_name.setText("")
                        edt_price.setText("0")

                    }
                }
        }
    }

    /**
     * because we have send event from fragment Food List, so in this activity we will
     * listen this event to display data
     */

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAddSizeReceive(event: AddonSizeEditEvent) {
        if (!event.isAddon) { // Size
            if (Common.FOOD_SELECTED!!.size != null) {

                adapter = MySizeAdapter(this, Common.FOOD_SELECTED!!.size.toMutableList())
                foodEditPosition = event.pos
                recycler_addon_size!!.adapter = adapter
                isAddon = event.isAddon

            }
        }
    }

    //
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSizeModelUpdate(event: UpdateSizeModel) {
        if (event.sizeModelList != null) { // Size
            needSave = true
            Common.FOOD_SELECTED!!.size = event.sizeModelList!! // update
        }
    }

    //
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectSizeEvent(event: SelectSizeModel) {
        if (event.sizeModel != null) { // Size
            edt_name.setText(event.sizeModel!!.name)
            edt_price.setText(event.sizeModel!!.price!!.toString())
            btn_edit.isEnabled = true
        } else
            btn_edit.isEnabled = false
    }
}
