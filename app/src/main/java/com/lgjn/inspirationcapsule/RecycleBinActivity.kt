package com.lgjn.inspirationcapsule

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.lgjn.inspirationcapsule.adapter.RecycleBinAdapter
import com.lgjn.inspirationcapsule.data.Inspiration
import com.lgjn.inspirationcapsule.data.InspirationRepository
import com.lgjn.inspirationcapsule.databinding.ActivityRecycleBinBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecycleBinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecycleBinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecycleBinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.recycleBinPager.adapter = RecycleBinPagerAdapter(this)
        binding.recycleBinPager.isUserInputEnabled = true

        TabLayoutMediator(binding.tabLayout, binding.recycleBinPager) { tab, position ->
            tab.text = if (position == 0) "已完成" else "已丢弃"
        }.attach()
    }

    class RecycleBinPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment =
            RecycleBinListFragment.newInstance(
                if (position == 0) Inspiration.STATUS_COMPLETED else Inspiration.STATUS_DISCARDED
            )
    }
}

class RecycleBinListFragment : Fragment() {

    private lateinit var adapter: RecycleBinAdapter
    private lateinit var repository: InspirationRepository
    private var status = Inspiration.STATUS_COMPLETED

    companion object {
        fun newInstance(status: String) = RecycleBinListFragment().apply {
            arguments = Bundle().also { it.putString("status", status) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = arguments?.getString("status") ?: Inspiration.STATUS_COMPLETED
        repository = InspirationRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            setPadding(0, 8, 0, 24)
            clipToPadding = false
        }
        adapter = RecycleBinAdapter(
            onRestore = { inspiration -> restoreItem(inspiration) },
            onDelete = { inspiration -> confirmDelete(inspiration) }
        )
        rv.adapter = adapter
        return rv
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun loadItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { repository.getByStatus(status) }
            adapter.updateData(items)
        }
    }

    private fun restoreItem(inspiration: Inspiration) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.updateStatus(inspiration.id, Inspiration.STATUS_ACTIVE)
            loadItems()
            Toast.makeText(requireContext(), "已还原到主页", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(inspiration: Inspiration) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = "永久删除"
        view.findViewById<TextView>(R.id.dialogMessage).text = "此操作不可撤销，确定要永久删除这条灵感吗？"
        view.findViewById<TextView>(R.id.dialogBtnConfirm).text = "删除"

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.dialogBtnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<TextView>(R.id.dialogBtnConfirm).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                repository.delete(inspiration.id)
                loadItems()
            }
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.82).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
