package com.qadomy.eatitserver.ui.order

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.qadomy.eatitserver.common.MySwipeHelper
import com.qadomy.eatitserver.eventbus.ChangeMenuClick
import com.qadomy.eatitserver.eventbus.LoadOrderEvent
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
                adapter = MyOrderAdapter(requireContext(), orderList)
                recyclerOrder.adapter = adapter
                recyclerOrder.layoutAnimation = layoutAnimationController


                txt_order_filter.text = StringBuilder("Orders (")
                    .append(orderList.size)
                    .append(")")

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
                 * Delete Button
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
                 * Update Button
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
                 *  Size Button
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
                                // when click on size button after we swipe, we edit it in menu and database


                            }

                        }
                    )
                )

                /**
                 *  Addon Button
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
                                // when click on addon button after we swipe, we addon it in menu and database

                            }

                        }
                    )
                )
            }


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
        if (item.itemId == R.id.action_filter) {
            /**
             * when click on filter icon menu, display an dialog as bottom sheet "order filter fragment"
             */
            val bottomSheet = BottomSheetOrderFragment.instance
            bottomSheet!!.show(requireActivity().supportFragmentManager, "OrderList")
        }
        return true
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