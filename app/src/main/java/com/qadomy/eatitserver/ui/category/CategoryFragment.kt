package com.qadomy.eatitserver.ui.category

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.qadomy.eatitserver.R
import com.qadomy.eatitserver.adapter.MyCategoriesAdapter
import com.qadomy.eatitserver.callback.IMyButtonCallback
import com.qadomy.eatitserver.common.Common
import com.qadomy.eatitserver.common.MySwipeHelper
import com.qadomy.eatitserver.model.CategoryModel
import dmax.dialog.SpotsDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CategoryFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 12345
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter? = null
    private var recyclerMenu: RecyclerView? = null

    internal var categoryModels: List<CategoryModel> = ArrayList()
    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUrl: Uri? = null
    internal lateinit var imageCategory: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        categoryViewModel =
            ViewModelProviders.of(this).get(CategoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_category, container, false)

        // init
        initView(root)

        categoryViewModel.getMessageError().observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        categoryViewModel.getCategoryList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            //
            categoryModels = it
            adapter = MyCategoriesAdapter(requireContext(), it)
            recyclerMenu!!.adapter = adapter
            recyclerMenu!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initView(root: View) {

        // init storage firebase
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        // init recycler_menu
        recyclerMenu = root.findViewById(R.id.recycler_menu)
        recyclerMenu!!.setHasFixedSize(true)

        // display items as linear layout
        val layoutManager = LinearLayoutManager(context)


        recyclerMenu!!.layoutManager = layoutManager
        recyclerMenu!!.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))


        /** for attach swipe for Update recycler view items */
        val swipe = object : MySwipeHelper(requireContext(), recyclerMenu!!, 200) {
            override fun instantisteMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(
                        context!!,
                        "Update",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on update button after we swipe, we updated it from menu and database
                                Common.CATEGORY_SELECTED = categoryModels[pos]
                                showUpdateDialog()
                            }

                        }
                    )
                )
            }

        }

    }

    private fun showUpdateDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Update Category")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category, null)

        val editCategoryName = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        imageCategory = itemView.findViewById(R.id.img_category) as ImageView

        // set data
        editCategoryName.setText(Common.CATEGORY_SELECTED!!.name)
        Glide.with(requireContext()).load(Common.CATEGORY_SELECTED!!.image).into(imageCategory)

        // set event, when click on image
        imageCategory.setOnClickListener { view ->
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
            val updateData = HashMap<String, Any>()
            updateData["name"] = editCategoryName.text.toString()

            if (imageUrl != null) {
                dialog.setMessage("Uploading...")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUrl!!)
                    .addOnFailureListener { e ->
                        dialog.dismiss()
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { taskSnapshot ->
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->
                            updateData["image"] = uri.toString()
                            updateCategory(updateData)
                        }
                    }
            } else {
                updateCategory(updateData)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()

    }

    private fun updateCategory(updateData: HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.CATEGORY_SELECTED!!.menuId!!)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { task ->
                categoryViewModel!!.loadCategory()
                Toast.makeText(context, "Update Success", Toast.LENGTH_SHORT).show()
            }
    }


    // onActivityResult for set data to imageUri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            if (data != null && data.data != null) {
                imageUrl = data.data
                imageCategory.setImageURI(imageUrl)
            }
        }
    }
}
