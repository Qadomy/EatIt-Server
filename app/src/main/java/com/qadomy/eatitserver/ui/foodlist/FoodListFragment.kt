package com.qadomy.eatitserver.ui.foodlist

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private fun initView(root: View?) {

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
                                        Common.CATEGORY_SELECTED!!.foods!!.removeAt(pos)
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
                                showUpdateDialog(pos)

                            }

                        }
                    )
                )

                /**
                 *  Button
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
                                Common.FOOD_SELECTED = foodModelList!![pos]

                                startActivity(Intent(context, SizeAddonEditActivity::class.java))

                                // Use EventBus to send event to tell sizeAddonActivity receive our request
                                EventBus.getDefault().postSticky(AddonSizeEditEvent(false, pos))

                            }

                        }
                    )
                )
            }


        }

    }

    // function to show dialog for update item from food menu after click on it
    private fun showUpdateDialog(pos: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_food, null)

        val editFoodName = itemView.findViewById<View>(R.id.edt_food_name) as EditText
        val editFoodPrice = itemView.findViewById<View>(R.id.edt_food_price) as EditText
        val editFoodDescription = itemView.findViewById<View>(R.id.edt_food_description) as EditText

        imageFood = itemView.findViewById(R.id.img_food_image) as ImageView

        // set data
        editFoodName.setText(StringBuilder("").append(Common.CATEGORY_SELECTED!!.foods!![pos].name))
        editFoodPrice.setText(StringBuilder("").append(Common.CATEGORY_SELECTED!!.foods!![pos].price))
        editFoodDescription.setText(StringBuilder("").append(Common.CATEGORY_SELECTED!!.foods!![pos].description))

        Glide.with(requireContext()).load(Common.CATEGORY_SELECTED!!.foods!![pos].image)
            .into(imageFood!!)

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
            val updateFood = Common.CATEGORY_SELECTED!!.foods!![pos]

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
}
