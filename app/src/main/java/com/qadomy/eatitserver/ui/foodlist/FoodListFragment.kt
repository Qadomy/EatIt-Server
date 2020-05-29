package com.qadomy.eatitserver.ui.foodlist

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.SizeAddonEditActivity
import com.qadomy.eatitserver.adapter.MyFoodListAdapter
import com.qadomy.eatitserver.callback.IMyButtonCallback
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.common.MySwipeHelper
import com.qadomy.eatitserver.eventbus.AddonSizeEditEvent
import com.qadomy.eatitserver.eventbus.ChangeMenuClick
import com.qadomy.eatitserver.eventbus.ToastEvent
import com.qadomy.eatitserver.model.FoodModel
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FoodListFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 1321
    private lateinit var foodListViewModel: FoodListViewModel
    private var recyclerFoodList: RecyclerView? = null
    private var layoutAnimationController: LayoutAnimationController? = null

    private var adapter: MyFoodListAdapter? = null
    private var foodModelList: List<FoodModel> = ArrayList<FoodModel>()


    // Variable
    private var imageFood: ImageView? = null
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var dialog: android.app.AlertDialog

    private var imageUri: Uri? = null


    // onDestroy, make event bus when destroy app
    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    // onCreate
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


    /**
     *
     * Init
     */
    private fun initView(root: View?) {

        // Enable options menu on Fragment
        setHasOptionsMenu(true)

        // init dialog
        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        // init firebase storage
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference


        recyclerFoodList = root!!.findViewById(R.id.recycler_food_list)
        recyclerFoodList!!.setHasFixedSize(true)
        recyclerFoodList!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)


        // change the bar title in food list fragment
        (activity as AppCompatActivity).supportActionBar!!.title = Common.CATEGORY_SELECTED!!.name


        /**
         *  DisplayMetrics: structure describing general information about a display,
         *  such as it size, density, and font scaling.
         */
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        /** windowManager: for showing custom windows */
        val width =
            displayMetrics.widthPixels // retrieve the absolute width of the available display size in pixels


        /** for attach swipe for DELETE recycler view items */
        val swipe = object : MySwipeHelper(requireContext(), recyclerFoodList!!, width / 6) {
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

                                        val foodModel = adapter!!.getItemAtPosition(pos)

                                        if (foodModel.positionInList == -1)
                                            Common.CATEGORY_SELECTED!!.foods!!.removeAt(pos)
                                        else
                                            Common.CATEGORY_SELECTED!!.foods!!.removeAt(foodModel.positionInList)

                                        updateFood(Common.CATEGORY_SELECTED!!.foods, true)
                                    }

                                // display dialog
                                val deleteDialog = builder.create()
                                deleteDialog.show()


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
                        "Update",
                        30,
                        0,
                        Color.parseColor("#520027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on update button after we swipe, we update it from menu and database

                                val foodModel = adapter!!.getItemAtPosition(pos)

                                if (foodModel.positionInList == -1)
                                    showUpdateDialog(pos, foodModel)
                                else
                                    showUpdateDialog(foodModel.positionInList, foodModel)

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
                        "Size",
                        30,
                        0,
                        Color.parseColor("#12005e"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on size button after we swipe, we edit it in menu and database
                                val foodModel = adapter!!.getItemAtPosition(pos)

                                if (foodModel.positionInList == -1)
                                    Common.FOOD_SELECTED = foodModelList!![pos]
                                else
                                    Common.FOOD_SELECTED = foodModel

                                startActivity(Intent(context, SizeAddonEditActivity::class.java))


                                // Use EventBus to send event to tell sizeAddonActivity receive our request
                                if (foodModel.positionInList == -1)
                                    EventBus.getDefault().postSticky(AddonSizeEditEvent(false, pos))
                                else
                                    EventBus.getDefault().postSticky(
                                        AddonSizeEditEvent(
                                            false,
                                            foodModel.positionInList
                                        )
                                    )


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
                        "Addon",
                        30,
                        0,
                        Color.parseColor("#333639"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on addon button after we swipe, we addon it in menu and database
                                val foodModel = adapter!!.getItemAtPosition(pos)

                                if (foodModel.positionInList == -1)
                                    Common.FOOD_SELECTED = foodModelList!![pos]
                                else
                                    Common.FOOD_SELECTED = foodModel

                                startActivity(Intent(context, SizeAddonEditActivity::class.java))


                                // Use EventBus to send event to tell sizeAddonActivity receive our request
                                if (foodModel.positionInList == -1)
                                    EventBus.getDefault().postSticky(AddonSizeEditEvent(true, pos))
                                else
                                    EventBus.getDefault().postSticky(
                                        AddonSizeEditEvent(
                                            true,
                                            foodModel.positionInList
                                        )
                                    )


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

    // function to show dialog for update item from food menu after click on it
    private fun showUpdateDialog(pos: Int, foodModel: FoodModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_food, null)

        val editFoodName = itemView.findViewById<View>(R.id.edt_food_name) as EditText
        val editFoodPrice = itemView.findViewById<View>(R.id.edt_food_price) as EditText
        val editFoodDescription = itemView.findViewById<View>(R.id.edt_food_description) as EditText

        imageFood = itemView.findViewById(R.id.img_food_image) as ImageView

        // set data
        editFoodName.setText(StringBuilder("").append(foodModel.name))
        editFoodPrice.setText(StringBuilder("").append(foodModel.price))
        editFoodDescription.setText(StringBuilder("").append(foodModel.description))

        Glide.with(requireContext()).load(foodModel.image).into(imageFood!!)

        // set event when click on image to choose a other image
        imageFood!!.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT

            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }


        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("UPDATE") { dialogInterface, _ ->
            val updateFood = foodModel

            updateFood.name = editFoodName.text.toString()
            updateFood.description = editFoodDescription.text.toString()
            updateFood.price = if (TextUtils.isEmpty(editFoodPrice.text)) {
                0
            } else {
                editFoodPrice.text.toString().toLong()
            }

            if (imageUri != null) {

                dialog.setMessage("Uploading...")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener { e ->
                        dialog.dismiss()
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener {
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->

                            // hide dialog
                            dialog!!.dismiss()
                            updateFood.image = uri.toString()
                            Common.CATEGORY_SELECTED!!.foods!![pos] = updateFood
                            updateFood(Common.CATEGORY_SELECTED!!.foods!!, false)

                        }
                    }

            } else {
                Common.CATEGORY_SELECTED!!.foods!![pos] = updateFood
                updateFood(Common.CATEGORY_SELECTED!!.foods!!, false)
            }

        }

        builder.setView(itemView)

        // create dialog
        val updateDialog = builder.create()
        updateDialog.show()

    }

    // function delete item from food menu and update items
    private fun updateFood(
        foods: MutableList<FoodModel>?,
        isDelete: Boolean
    ) {
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

                    EventBus.getDefault().postSticky(
                        ToastEvent(
                            !isDelete,
                            isBackFromFoodList = true
                        )
                    )
                }
            }
    }

    // onActivityResult for set data to imageUri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            if (data != null && data.data != null) {
                imageUri = data.data
                imageFood!!.setImageURI(imageUri)
            }
        }
    }


    /**
     *
     * Menu
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.food_list_menu, menu)

        // create search view
        val menuItem = menu.findItem(R.id.action_search)

        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager

        val searchView = menuItem.actionView as SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName!!))

        // Event, when click on search button
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(search: String?): Boolean {
                startSearchFoods(search!!)

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // clear text when click to clear button
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText
            // clear text
            ed.setText("")

            // clear query
            searchView.setQuery("", false)

            // collapse the action view
            searchView.onActionViewCollapsed()

            // collapse the search widget
            menuItem.collapseActionView()

            // restore result to original
            foodListViewModel.getMutableFoodListData().value = Common.CATEGORY_SELECTED!!.foods
        }
    }


    // method for make search
    private fun startSearchFoods(search: String?) {
        val resultFood: MutableList<FoodModel> = ArrayList()

        for (i in Common.CATEGORY_SELECTED!!.foods!!.indices) {

            val foodModel = Common.CATEGORY_SELECTED!!.foods!![i]
            if (foodModel.name!!.toLowerCase().contains(search!!.toLowerCase())) {
                // Here will save index of search result item
                foodModel.positionInList = i
                resultFood.add(foodModel)
            }
        }

        // update search result
        foodListViewModel!!.getMutableFoodListData().value = resultFood
    }
}
