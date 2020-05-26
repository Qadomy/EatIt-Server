package com.qadomy.eatitserver.ui.foodlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.adapter.MyFoodListAdapter
import com.qadomy.eatitserver.common.Common

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel
    private var recyclerFoodList: RecyclerView? = null
    private var layoutAnimationController: LayoutAnimationController? = null

    private var adapter: MyFoodListAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel =
            ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)

        // init view
        initView(root)

        foodListViewModel.getMutableFoodListData().observe(viewLifecycleOwner, Observer {
            adapter = MyFoodListAdapter(requireContext(), it)
            recyclerFoodList!!.adapter = adapter
            recyclerFoodList!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initView(root: View?) {

        recyclerFoodList = root!!.findViewById(R.id.recycler_food_list)
        recyclerFoodList!!.setHasFixedSize(true)
        recyclerFoodList!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)


        // change the bar title in food list fragemt
        (activity as AppCompatActivity).supportActionBar!!.title = Common.CATEGORY_SELECTED!!.name


    }
}
