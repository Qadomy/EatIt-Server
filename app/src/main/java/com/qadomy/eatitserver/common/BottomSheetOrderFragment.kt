package com.qadomy.eatitserver.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.eventbus.LoadOrderEvent
import kotlinx.android.synthetic.main.fragment_order_filter.*
import org.greenrobot.eventbus.EventBus

class BottomSheetOrderFragment : BottomSheetDialogFragment() {


    companion object {
        val instance: BottomSheetOrderFragment? = null
            get() = field ?: BottomSheetOrderFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemView = inflater.inflate(R.layout.fragment_order_filter, container, false)

        return itemView
    }

    // onViewCreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // init views
        initViews()
    }


    /**
     *
     * Init views
     */
    private fun initViews() {

        // make event bus for each item click in order filter fragment

        placed_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(0))
            dismiss()
        }

        shipping_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(1))
            dismiss()
        }

        shipped_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(2))
            dismiss()
        }

        cancelled_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(-1))
            dismiss()
        }
    }


}