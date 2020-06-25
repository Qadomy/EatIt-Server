package com.qadomy.eatitserver.ui.shipper

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.adapter.MyShipperAdapter
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.eventbus.ChangeMenuClick
import com.qadomy.eatitserver.eventbus.UpdateActiveEvent
import com.qadomy.eatitserver.model.ShipperModel
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ShipperFragment : Fragment() {


    // region variables
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyShipperAdapter? = null
    private var recyclerShipper: RecyclerView? = null
    private var shipperModel: List<ShipperModel> = ArrayList()
    private lateinit var shipperViewModel: ShipperViewModel
    // endregion


    // onDestroy
    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    // onStart
    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    // onStop
    override fun onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(UpdateActiveEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateActiveEvent::class.java)
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)

        super.onStop()
    }

    // onCreate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.shipper_fragment, container, false)
        shipperViewModel = ViewModelProviders.of(this).get(ShipperViewModel::class.java)

        initViews(root)

        shipperViewModel.getMessageError().observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        shipperViewModel.getShipperList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            //
            shipperModel = it
            adapter = MyShipperAdapter(requireContext(), it)
            recyclerShipper!!.adapter = adapter
            recyclerShipper!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initViews(root: View?) {

        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        // init recycler_menu
        recyclerShipper = root!!.findViewById(R.id.recycler_shipper)
        recyclerShipper!!.setHasFixedSize(true)

        // display items as linear layout
        val layoutManager = LinearLayoutManager(context)


        recyclerShipper!!.layoutManager = layoutManager
        recyclerShipper!!.addItemDecoration(
            DividerItemDecoration(
                context,
                layoutManager.orientation
            )
        )

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateActiveEvent(updateActiveEvent: UpdateActiveEvent) {
        val updateData = HashMap<String, Any>()
        updateData["active"] = updateActiveEvent.active
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPER_REF)
            .child(updateActiveEvent.shipperModel!!.key!!)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { aVoid ->
                Toast.makeText(context, "Update state to $updateActiveEvent", Toast.LENGTH_SHORT)
                    .show()
            }
    }

}