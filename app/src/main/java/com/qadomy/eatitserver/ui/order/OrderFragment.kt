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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.adapter.MyOrderAdapter
import com.qadomy.eatitserver.adapter.MyShipperSelectedAdapter
import com.qadomy.eatitserver.callback.IMyButtonCallback
import com.qadomy.eatitserver.callback.IShipperLoadCallbackListener
import com.qadomy.eatitserver.common.BottomSheetOrderFragment
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.common.Common.SHIPPER_REF
import com.qadomy.eatitserver.common.MySwipeHelper
import com.qadomy.eatitserver.eventbus.ChangeMenuClick
import com.qadomy.eatitserver.eventbus.LoadOrderEvent
import com.qadomy.eatitserver.model.*
import com.qadomy.eatitserver.remote.IFCMService
import com.qadomy.eatitserver.remote.RetrofitFCMClient
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_order.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class OrderFragment : Fragment(), IShipperLoadCallbackListener {

    private val compositeDisposable = CompositeDisposable()
    lateinit var ifcmService: IFCMService

    private lateinit var orderViewModel: OrderViewModel
    lateinit var recyclerOrder: RecyclerView
    lateinit var layoutAnimationController: LayoutAnimationController

    private var adapter: MyOrderAdapter? = null

    var myShipperSelectedAdapter: MyShipperSelectedAdapter? = null
    lateinit var shipperLoadCallbaListener: IShipperLoadCallbackListener
    var recyclerShipper: RecyclerView? = null

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

        // clear the composite Disposable
        compositeDisposable.clear()
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

        // init shipperLoadCallbaListener
        shipperLoadCallbaListener = this

        // init IFCMService
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

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

                recyclerShipper = layoutDialog.findViewById(R.id.recycler_shipper) as RecyclerView

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

        if (orderModel.orderStatus == 0) // shipping
            loadShipperList(
                pos,
                orderModel,
                dialog,
                btnOk,
                btnCancel,
                rdiShipping,
                rdiShipped,
                rdiCancelled,
                rdiDelete,
                rdiRestorePlaced
            )
        else
            showDialog(
                pos,
                orderModel,
                dialog,
                btnOk,
                btnCancel,
                rdiShipping,
                rdiShipped,
                rdiCancelled,
                rdiDelete,
                rdiRestorePlaced
            )

    }

    private fun loadShipperList(
        pos: Int,
        orderModel: OrderModel,
        dialog: AlertDialog?,
        btnOk: Button,
        btnCancel: Button,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestorePlaced: RadioButton?
    ) {
        val tempList: MutableList<ShipperModel> = ArrayList()
        val shipperRef = FirebaseDatabase.getInstance().getReference(SHIPPER_REF)
        val shipperActive = shipperRef.orderByChild("active").equalTo(true)
        shipperActive.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                shipperLoadCallbaListener.onShipperLoadFailed(error!!.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (shipperSnapshot in snapshot.children) {
                    val shipperModel = shipperSnapshot.getValue(ShipperModel::class.java)
                    shipperModel!!.key = shipperSnapshot.key
                    tempList.add(shipperModel)
                }


                shipperLoadCallbaListener.onShipperLoadSuccess(
                    pos,
                    orderModel,
                    tempList,
                    dialog,
                    btnOk,
                    btnCancel,
                    rdiShipping,
                    rdiShipped,
                    rdiCancelled,
                    rdiDelete,
                    rdiRestorePlaced
                )
            }
        })
    }

    private fun showDialog(
        pos: Int,
        orderModel: OrderModel,
        dialog: AlertDialog?,
        btnOk: Button,
        btnCancel: Button,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestorePlaced: RadioButton?
    ) {

        dialog!!.show()

        //custom dialog
        btnCancel.setOnClickListener { dialog!!.dismiss() }
        btnOk.setOnClickListener {

            when {
                rdiCancelled != null && rdiCancelled.isChecked -> {
                    updateOrder(pos, orderModel, -1)
                    dialog!!.dismiss()
                }
                rdiShipping != null && rdiShipping.isChecked -> {
//                    updateOrder(pos, orderModel, 1)
                    var shipperModel: ShipperModel? = null
                    if (myShipperSelectedAdapter != null) {
                        shipperModel = myShipperSelectedAdapter!!.selectedShipper
                        if (shipperModel != null) {
                            Toast.makeText(
                                requireContext(),
                                "${shipperModel.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog!!.dismiss()
                        } else
                            Toast.makeText(
                                requireContext(),
                                "Please choose Shipper",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }
                rdiShipped != null && rdiShipped.isChecked -> {
                    updateOrder(pos, orderModel, 2)
                    dialog!!.dismiss()
                }
                rdiRestorePlaced != null && rdiRestorePlaced.isChecked -> {
                    updateOrder(pos, orderModel, 0)
                    dialog!!.dismiss()
                }
                rdiDelete != null && rdiDelete.isChecked -> {
                    deleteOrder(pos, orderModel)
                    dialog!!.dismiss()
                }
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

                    // create dialog
                    val dialog =
                        SpotsDialog.Builder().setContext(requireContext()).setCancelable(false)
                            .build()
                    dialog.show()

                    // load token
                    FirebaseDatabase.getInstance()
                        .getReference(Common.TOKEN_REF)
                        .child(orderModel.userId!!)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                                dialog.dismiss()
                                Toast.makeText(context, "" + p0.message, Toast.LENGTH_SHORT).show()
                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if (p0.exists()) {
                                    val tokenModel = p0.getValue(TokenModel::class.java)
                                    val notiData = HashMap<String, String>()
                                    notiData.put(
                                        Common.NOTIFICATION_TITLE,
                                        "Your order was updated"
                                    )

                                    notiData.put(
                                        Common.NOTIFICATION_CONTENT, StringBuilder("Your order ")
                                            .append(orderModel.key)
                                            .append(" was update to ")
                                            .append(Common.convertStatusToString(status)).toString()
                                    )

                                    val sendData = FCMSendData(tokenModel!!.token!!, notiData)
                                    compositeDisposable.add(
                                        ifcmService.sendNotification(sendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ t: FCMResponse? ->
                                                dialog.dismiss()
                                                if (t!!.success == 1) {
                                                    Toast.makeText(
                                                        context,
                                                        "Update order successfully",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                        .show()

                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to send notification",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                        .show()
                                                }
                                            }, { t: Throwable? ->
                                                dialog.dismiss()
                                                Toast.makeText(
                                                    context,
                                                    "" + t!!.message,
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            })
                                    )

                                } else {
                                    dialog.dismiss()
                                    Toast.makeText(context, "Token not found", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        })


                    // we remove item in list recycle view
                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)

                    updateTextCounter()

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


    /**
     * Methods implement from IShipperLoadCallbackListener interface
     */
    // region IShipperLoadCallbackListener
    override fun onShipperLoadSuccess(shipperList: List<ShipperModel>) {
        // go nothing
    }

    override fun onShipperLoadSuccess(
        pos: Int,
        orderModel: OrderModel?,
        shipperList: List<ShipperModel>?,
        dialog: AlertDialog?,
        ok: Button?,
        cancel: Button?,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestore: RadioButton?
    ) {
        if (recyclerShipper != null) {
            recyclerShipper!!.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(context)
            recyclerShipper!!.layoutManager = layoutManager
            recyclerShipper!!.addItemDecoration(
                DividerItemDecoration(
                    context,
                    layoutManager.orientation
                )
            )
            myShipperSelectedAdapter = MyShipperSelectedAdapter(requireContext(), shipperList!!)
            recyclerShipper!!.adapter = myShipperSelectedAdapter
        }

        showDialog(
            pos,
            orderModel!!,
            dialog,
            ok!!,
            cancel!!,
            rdiShipping,
            rdiShipped,
            rdiCancelled,
            rdiDelete,
            rdiRestore
        )
    }

    override fun onShipperLoadFailed(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    // endregion

}