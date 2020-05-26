package com.qadomy.eatitserver.ui.foodlist

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.adapter.MyFoodListAdapter
import com.qadomy.eatitserver.callback.IMyButtonCallback
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.common.MySwipeHelper
import com.qadomy.eatitserver.model.FoodModel

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel
    private var recyclerFoodList: RecyclerView? = null
    private var layoutAnimationController: LayoutAnimationController? = null

    private var adapter: MyFoodListAdapter? = null
    private var foodModelList: List<FoodModel> = ArrayList<FoodModel>()

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
            if (it != null) {
                // until fix any error happen if there any error

                foodModelList = it
                adapter = MyFoodListAdapter(requireContext(), it)
                recyclerFoodList!!.adapter = adapter
                recyclerFoodList!!.layoutAnimation = layoutAnimationController
            }
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


        /** for attach swipe for DELETE recycler view items */
        val swipe = object : MySwipeHelper(requireContext(), recyclerFoodList!!, 300) {
            override fun instantisteMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(
                        context!!,
                        "Delete",
                        30,
                        0,
                        Color.parseColor("#9b0000"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on delete button after we swipe, we delete it from menu and database
                                Common.FOOD_SELECTED = foodModelList[pos]

                                val builder = AlertDialog.Builder(context!!)

                                builder.setTitle("Delete")
                                    .setMessage("Do you really want to delete food ?")
                                    .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                                    .setPositiveButton("DELETE") { _, _ ->
                                        Common.CATEGORY_SELECTED!!.foods!!.removeAt(pos)
                                        updateFood(Common.CATEGORY_SELECTED!!.foods)
                                    }

                                // display dialog
                                val deleteDialog = builder.create()
                                deleteDialog.show()


                            }

                        }
                    )
                )

                buffer.add(
                    MyButton(
                        context!!,
                        "Update",
                        30,
                        0,
                        Color.parseColor("#520027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on update button after we swipe, we update it from menu and database


                            }

                        }
                    )
                )
            }


        }

    }

    private fun updateFood(foods: MutableList<FoodModel>?) {
        val updateData = HashMap<String, Any>()
        updateData["foods"] = foods!!

        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.CATEGORY_SELECTED!!.menuId!!)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "" + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    foodListViewModel.getMutableFoodListData()
                    Toast.makeText(requireContext(), "Delete success", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
