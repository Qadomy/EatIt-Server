package com.qadomy.eatitserver

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.FirebaseDatabase
import com.qadomy.eatitserver.adapter.MyAddonAdapter
import com.qadomy.eatitserver.adapter.MySizeAdapter
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.eventbus.*
import com.qadomy.eatitserver.model.AddonModel
import com.qadomy.eatitserver.model.SizeModel
import kotlinx.android.synthetic.main.activity_size_addon_edit.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SizeAddonEditActivity : AppCompatActivity() {


    var sizeAdapter: MySizeAdapter? = null
    var addonAdapter: MyAddonAdapter? = null
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


    /**
     *
     * Init
     */

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

            if (!isAddon) { // mean change in size (create button when choose size)
                if (sizeAdapter != null) {
                    val sizeModel = SizeModel()

                    if (TextUtils.isEmpty(edt_name.text) && TextUtils.isEmpty(edt_price.text)) {
                        // if two edit text is empty
                        Toast.makeText(this, "You must edit some thing", Toast.LENGTH_SHORT).show()

                    } else {
                        // if any of edit text not empty
                        sizeModel.name = edt_name.text.toString()

                        if (TextUtils.isEmpty(edt_price.text))
                        // if we didn't write any price
                            sizeModel.price = 0
                        else
                            sizeModel.price = edt_price.text.toString().toLong()

                        sizeAdapter!!.addNewSize(sizeModel)
                    }
                }
            } else {    // mean change in addon (create button when choose addon)

                if (addonAdapter != null) {
                    val addonModel = AddonModel()

                    if (TextUtils.isEmpty(edt_name.text) && TextUtils.isEmpty(edt_price.text)) {
                        // if two edit text is empty
                        Toast.makeText(this, "You must add some thing", Toast.LENGTH_SHORT).show()

                    } else {
                        // if any of edit text not empty
                        addonModel.name = edt_name.text.toString()

                        if (TextUtils.isEmpty(edt_price.text))
                        // if we didn't write any price

                            addonModel.price = 0
                        else
                            addonModel.price = edt_price.text.toString().toLong()

                        addonAdapter!!.addNewAddon(addonModel)
                    }

                }
            }
        }

        // Event, when click on edit button
        btn_edit.setOnClickListener {
            if (!isAddon) { // mean size, (Edit button when choose size)
                if (sizeAdapter != null) {

                    val sizeModel = SizeModel()
                    sizeModel.name = edt_name.text.toString()
                    sizeModel.price = edt_price.text.toString().toLong()

                    sizeAdapter!!.editSize(sizeModel)

                }

            } else { // mean addon, (Edit button when choose addon)

                if (sizeAdapter != null) {

                    val addonModel = AddonModel()
                    addonModel.name = edt_name.text.toString()
                    addonModel.price = edt_price.text.toString().toLong()

                    addonAdapter!!.editAddon(addonModel)

                }
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

                if (needSave) { // mean if happened any changes
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
                } else {
                    // if back without make anything
                    closeActivity()
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
        if (!event.isAddon) { // mean size

            if (Common.FOOD_SELECTED!!.size != null) {

                sizeAdapter = MySizeAdapter(this, Common.FOOD_SELECTED!!.size.toMutableList())
                foodEditPosition = event.pos
                recycler_addon_size!!.adapter = sizeAdapter
                isAddon = event.isAddon

            }

        } else { // mean addon

            if (Common.FOOD_SELECTED!!.addon != null) {

                addonAdapter = MyAddonAdapter(this, Common.FOOD_SELECTED!!.addon.toMutableList())
                foodEditPosition = event.pos
                recycler_addon_size!!.adapter = addonAdapter
                isAddon = event.isAddon

            }

        }
    }

    // event for edit size item
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSizeModelUpdate(event: UpdateSizeModel) {
        if (event.sizeModelList != null) { // Size
            needSave = true
            Common.FOOD_SELECTED!!.size = event.sizeModelList!! // update
        }
    }


    // event when select item to edit size
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectSizeEvent(event: SelectSizeModel) {
        if (event.sizeModel != null) { // Size
            edt_name.setText(event.sizeModel!!.name)
            edt_price.setText(event.sizeModel!!.price!!.toString())
            btn_edit.isEnabled = true
        } else
            btn_edit.isEnabled = false
    }


    // event for edit addon item
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAddonModelUpdate(event: UpdateAddonModel) {
        if (event.addonModelList != null) { // Size
            needSave = true
            Common.FOOD_SELECTED!!.addon = event.addonModelList!! // update
        }
    }

    // event when select item to edit addon
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectAddonEvent(event: SelectAddonModel) {
        if (event.addonModel != null) { // Size
            edt_name.setText(event.addonModel!!.name)
            edt_price.setText(event.addonModel!!.price!!.toString())
            btn_edit.isEnabled = true
        } else
            btn_edit.isEnabled = false
    }
}
