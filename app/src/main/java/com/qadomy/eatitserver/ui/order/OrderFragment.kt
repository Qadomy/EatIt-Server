package com.qadomy.eatitserver.ui.order

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.adapter.MyOrderAdapter
import com.qadomy.eatitserver.callback.IMyButtonCallback
import com.qadomy.eatitserver.common.BottomSheetOrderFragment
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.common.MySwipeHelper
import com.qadomy.eatitserver.eventbus.ChangeMenuClick
import com.qadomy.eatitserver.eventbus.LoadOrderEvent
import com.qadomy.eatitserver.model.OrderModel
import kotlinx.android.synthetic.main.fragment_order.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class OrderFragment : Fragment() {

    private lateinit var orderViewModel: OrderViewModel
    lateinit var recyclerOrder: RecyclerView
    lateinit var layoutAnimationController: LayoutAnimationController

    private var adapter: MyOrderAdapter? = null


    // onStart, register event bus
    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    // onStop, unregister event bus
    override fun onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent::class.java)

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)

        super.onStop()
    }

    // onDestroy, check the click from any menu
    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))

        super.onDestroy()
    }

    // onCreate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_order, container, false)

        orderViewModel = ViewModelProviders.of(this).get(OrderViewModel::class.java)

        orderViewModel!!.messageError.observe(viewLifecycleOwner, Observer { s ->
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        })

        orderViewModel!!.getOrderModelList().observe(viewLifecycleOwner, Observer { orderList ->
            if (orderList != null) {
                adapter = MyOrderAdapter(requireContext(), orderList.toMutableList())
                recyclerOrder.adapter = adapter
                recyclerOrder.layoutAnimation = layoutAnimationController


                // update order text counter
                updateTextCounter()

            }
        })

        initView(root)


        return root
    }

    /**
     *
     * Init view
     */
    private fun initView(root: View) {

        // enable menu
        setHasOptionsMenu(true)

        recyclerOrder = root.findViewById(R.id.recycler_orders) as RecyclerView
        recyclerOrder.setHasFixedSize(true)
        recyclerOrder.layoutManager = LinearLayoutManager(context)

        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)


        /**
         *  DisplayMetrics: structure describing general information about a display,
         *  such as it size, density, and font scaling.
         */
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        /** windowManager: for showing custom windows */
        val width =
            displayMetrics.widthPixels // retrieve the absolute width of the available display size in pixels


        /** for attach swipe for Directions,call,remove,edit recycler view items */
        val swipe = object : MySwipeHelper(requireContext(), recyclerOrder!!, width / 6) {
            override fun instantisteMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {

                /**
                 * Directions Button
                 */
                buffer.add(
                    MyButton(
                        context!!,
                        "Directions",
                        30,
                        0,
                        Color.parseColor("#9b0000"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on delete button after we swipe, we delete it from menu and database

                            }

                        }
                    )
                )

                /**
                 * Call Button
                 */
                buffer.add(
                    MyButton(
                        context!!,
                        "Call",
                        30,
                        0,
                        Color.parseColor("#520027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on call button after we swipe, to call with customer

                                Dexter.withContext(activity)
                                    .withPermission(android.Manifest.permission.CALL_PHONE)
                                    .withListener(object : PermissionListener {
                                        override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                            val orderModel = adapter!!.getItemAtPosition(pos)
                                            val intent = Intent()
                                            intent.action = Intent.ACTION_DIAL
                                            intent.data = Uri.parse(
                                                StringBuilder("tel: ")
                                                    .append(orderModel.userPhone).toString()
                                            )
                                            startActivity(intent)
                                        }

                                        override fun onPermissionRationaleShouldBeShown(
                                            p0: PermissionRequest?,
                                            p1: PermissionToken?
                                        ) {

                                        }

                                        override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                            Toast.makeText(
                                                context,
                                                "You must accept this permission" + p0!!.permissionName,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }).check()
                            }

                        }
                    )
                )

                /**
                 *  Remove Button
                 */

                buffer.add(
                    MyButton(
                        context!!,
                        "Remove",
                        30,
                        0,
                        Color.parseColor("#12005e"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on remove button after we swipe, we remove order from list and database

                                val orderMode = adapter!!.getItemAtPosition(pos)

                                // make delete dialog
                                val builder = AlertDialog.Builder(context!!)
                                    .setTitle("Delete")
                                    .setMessage("Do you really want to remove this order?")
                                    .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                                    .setPositiveButton("DELETE") { dialogInterface, _ ->
                                        FirebaseDatabase.getInstance()
                                            .getReference(Common.ORDER_REF)
                                            .child(orderMode.key!!)
                                            .removeValue()
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    context!!,
                                                    "" + it.message,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .addOnSuccessListener {
                                                adapter!!.removeItem(pos)
                                                adapter!!.notifyItemRemoved(pos)

                                                // update order text counter
                                                updateTextCounter()

                                                dialogInterface.dismiss()

                                                Toast.makeText(
                                                    context!!,
                                                    "Order has been delete!",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                            }
                                    }

                                val dialog = builder.create()
                                dialog.show()


                                // change buttons colors for negative and positive buttons
                                val btnNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                                btnNegative.setTextColor(Color.LTGRAY)

                                val btnPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                                btnPositive.setTextColor(Color.RED)
                            }

                        }
                    )
                )

                /**
                 *  Edit Button
                 */

                buffer.add(
                    MyButton(
                        context!!,
                        "Edit",
                        30,
                        0,
                        Color.parseColor("#333639"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on Edit button after we swipe, we update it in list in firebase

                                showEditDialog(adapter!!.getItemAtPosition(pos), pos)
                            }

                        }
                    )
                )
            }


        }
    }


    /**
     *
     * Methods
     */

    // function for display dialog for edit
    private fun showEditDialog(orderModel: OrderModel, pos: Int) {
        var layoutDialog: View? = null
        var builder: AlertDialog.Builder? = null

        var rdiShipping: RadioButton? = null
        var rdiCancelled: RadioButton? = null
        var rdiShipped: RadioButton? = null
        var rdiRestorePlaced: RadioButton? = null
        var rdiDelete: RadioButton? = null

        when (orderModel.orderStatus) {
            -1 -> {
                /** -1 -> "Canceled"*/

                layoutDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_dialog_cancelled, null)

                builder = AlertDialog.Builder(requireContext()).setView(layoutDialog)

                rdiRestorePlaced =
                    layoutDialog.findViewById<View>(R.id.rdi_restore_placed) as RadioButton
                rdiDelete = layoutDialog.findViewById<View>(R.id.rdi_delete) as RadioButton

            }
            0 -> {
                /** 0 -> "Shipping"*/

                layoutDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_dialog_shipping, null)

                builder = AlertDialog.Builder(
                    requireContext(),
                    android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
                ).setView(layoutDialog)

                rdiShipping = layoutDialog.findViewById<View>(R.id.rdi_shipping) as RadioButton
                rdiCancelled = layoutDialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton

            }
            else -> {
                /** 2 -> "Shipped"*/

                layoutDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_dialog_shipped, null)

                builder = AlertDialog.Builder(requireContext()).setView(layoutDialog)

                rdiShipped = layoutDialog.findViewById<View>(R.id.rdi_shipped) as RadioButton
                rdiCancelled = layoutDialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton
            }
        }

        // views
        val btnOk = layoutDialog.findViewById<View>(R.id.btn_ok) as Button
        val btnCancel = layoutDialog.findViewById<View>(R.id.btn_cancel) as Button

        val textStatus = layoutDialog.findViewById<View>(R.id.txt_status) as TextView


        // set data
        textStatus.text = StringBuilder("Order Status(")
            .append(Common.convertStatusToString(orderModel.orderStatus))
            .append(")")

        // create dialog
        val dialog = builder.create()
        dialog.show()

        //custom dialog
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {
            dialog.dismiss()

            if (rdiCancelled != null && rdiCancelled.isChecked) {
                updateOrder(pos, orderModel, -1)
            } else if (rdiShipping != null && rdiShipping.isChecked) {
                updateOrder(pos, orderModel, 1)
            } else if (rdiShipped != null && rdiShipped.isChecked) {
                updateOrder(pos, orderModel, 2)
            } else if (rdiRestorePlaced != null && rdiRestorePlaced.isChecked) {
                updateOrder(pos, orderModel, 0)
            } else if (rdiDelete != null && rdiDelete.isChecked) {
                deleteOrder(pos, orderModel)
            }
        }

    }

    // function for update order
    private fun updateOrder(pos: Int, orderModel: OrderModel, status: Int) {
        if (!TextUtils.isEmpty(orderModel.key)) {
            val updateData = HashMap<String, Any>()
            updateData["orderStatus"] = status

            // update in firebase
            FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .child(orderModel.key!!)
                .updateChildren(updateData)
                .addOnFailureListener { throwable ->
                    Toast.makeText(requireContext(), "" + throwable.message, Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnSuccessListener {
                    // we remove item in list recycle view
                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)

                    updateTextCounter()

                    Toast.makeText(requireContext(), "Order updated success!", Toast.LENGTH_SHORT)
                        .show()
                }

        } else {

            Toast.makeText(
                requireContext(),
                "order number must not br null or empty",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // function for update order text counter
    private fun updateTextCounter() {
        txt_order_filter.text = StringBuilder("Orders (")
            .append(adapter!!.itemCount)
            .append(")")
    }

    // function for delete order
    private fun deleteOrder(pos: Int, orderModel: OrderModel) {
        if (!TextUtils.isEmpty(orderModel.key)) {

            // delete from firebase
            FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .child(orderModel.key!!)
                .removeValue()
                .addOnFailureListener { throwable ->
                    Toast.makeText(requireContext(), "" + throwable.message, Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnSuccessListener {
                    // we remove item in list recycle view
                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)

                    updateTextCounter()

                    Toast.makeText(requireContext(), "Order updated success!", Toast.LENGTH_SHORT)
                        .show()
                }

        } else {

            Toast.makeText(
                requireContext(),
                "order number must not br null or empty",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     *
     * Menu, init order menu
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_filter) {
            /**
             * when click on filter icon menu, display an dialog as bottom sheet "order filter fragment"
             */
            val bottomSheet = BottomSheetOrderFragment.instance
            bottomSheet!!.show(requireActivity().supportFragmentManager, "OrderList")
            true

        } else
            super.onOptionsItemSelected(item)

    }


    /**
     *
     * EventBus
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onLoadOrder(event: LoadOrderEvent) {
        orderViewModel.loadOrder(event.status)
    }

}