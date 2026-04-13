package com.lgjn.inspirationcapsule

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.lgjn.inspirationcapsule.adapter.RecycleBinAdapter
import com.lgjn.inspirationcapsule.data.Inspiration
import com.lgjn.inspirationcapsule.data.InspirationRepository
import com.lgjn.inspirationcapsule.databinding.FragmentPastInspirationsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PastInspirationsFragment : Fragment() {

    private var _binding: FragmentPastInspirationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: InspirationRepository
    private lateinit var adapter: RecycleBinAdapter

    /** Current tab: true = 已完成, false = 已丢弃 */
    private var showingCompleted = true

    /** Full lists loaded from DB (unfiltered) */
    private var completedList: List<Inspiration> = emptyList()
    private var discardedList: List<Inspiration> = emptyList()

    /** Current search query */
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPastInspirationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = InspirationRepository(requireContext())

        setupRecyclerView()
        setupSegmentedControl()
        setupSearchBar()
        setupCloseButton()
        loadAllData()
    }

    // ──────────────────── RecyclerView ────────────────────

    private fun setupRecyclerView() {
        adapter = RecycleBinAdapter(
            onRestore = { inspiration -> restoreItem(inspiration) },
            onDelete = { inspiration -> deleteItemWithUndo(inspiration) }
        )
        binding.rvPastInspirations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastInspirations.adapter = adapter
        // 滚动时自动收起展开的按钮行
        binding.rvPastInspirations.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                    adapter.collapseAll()
                }
            }
        })
    }

    // ──────────────────── iOS Segmented Control ────────────────────

    private fun setupSegmentedControl() {
        updateSegmentLabels()

        binding.segmentedControlContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.segmentedControlContainer.viewTreeObserver
                        .removeOnGlobalLayoutListener(this)
                    positionIndicator(showingCompleted, animate = false)
                }
            }
        )

        binding.segmentCompleted.setOnClickListener {
            if (!showingCompleted) {
                showingCompleted = true
                updateSegmentLabels()
                positionIndicator(showingCompleted, animate = true)
                updateList()
            }
        }

        binding.segmentDiscarded.setOnClickListener {
            if (showingCompleted) {
                showingCompleted = false
                updateSegmentLabels()
                positionIndicator(showingCompleted, animate = true)
                updateList()
            }
        }
    }

    private fun updateSegmentLabels() {
        binding.segmentCompleted.setTextColor(
            if (showingCompleted) 0xFF2D1F6E.toInt() else 0xCCFFFFFF.toInt()
        )
        binding.segmentDiscarded.setTextColor(
            if (!showingCompleted) 0xFF2D1F6E.toInt() else 0xCCFFFFFF.toInt()
        )
    }

    private fun positionIndicator(toCompleted: Boolean, animate: Boolean) {
        val container = binding.segmentedControlContainer
        val indicator = binding.segmentIndicator
        val containerWidth = container.width
        if (containerWidth == 0) return

        val margin = (3 * resources.displayMetrics.density + 0.5f).toInt()
        val halfWidth = (containerWidth - margin * 2) / 2
        val targetX = if (toCompleted) margin.toFloat() else (margin + halfWidth).toFloat()

        val lp = indicator.layoutParams
        lp.width = halfWidth
        indicator.layoutParams = lp

        if (animate) {
            ObjectAnimator.ofFloat(indicator, "translationX", indicator.translationX, targetX - indicator.left)
                .apply {
                    duration = 200
                    start()
                }
        } else {
            indicator.translationX = targetX - indicator.left
        }
    }

    // ──────────────────── Close Button ────────────────────

    private fun setupCloseButton() {
        binding.btnCloseDrawer.setOnClickListener {
            val drawerLayout = requireActivity().findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)
            drawerLayout?.closeDrawer(android.view.Gravity.END)
        }
    }

    // ──────────────────── Search Bar ────────────────────

    private fun setupSearchBar() {
        binding.btnSearch.setOnClickListener {
            showSearchBar(true)
        }

        binding.btnSearchClear.setOnClickListener {
            showSearchBar(false)
            searchQuery = ""
            updateList()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                updateList()
            }
        })
    }

    private fun showSearchBar(show: Boolean) {
        binding.headerRow.visibility = if (show) View.GONE else View.VISIBLE
        binding.searchBar.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            binding.etSearch.requestFocus()
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.etSearch.setText("")
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    // ──────────────────── Data Loading ────────────────────

    private fun loadAllData() {
        viewLifecycleOwner.lifecycleScope.launch {
            completedList = withContext(Dispatchers.IO) {
                repository.getByStatus(Inspiration.STATUS_COMPLETED)
            }
            discardedList = withContext(Dispatchers.IO) {
                repository.getByStatus(Inspiration.STATUS_DISCARDED)
            }
            updateList()
        }
    }

    private fun updateList() {
        val source = if (showingCompleted) completedList else discardedList
        val filtered = if (searchQuery.isEmpty()) {
            source
        } else {
            source.filter { it.content.contains(searchQuery, ignoreCase = true) }
        }
        adapter.updateData(filtered)
    }

    // ──────────────────── Actions ────────────────────

    private fun restoreItem(inspiration: Inspiration) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateStatus(inspiration.id, Inspiration.STATUS_ACTIVE)
            }
            Toast.makeText(requireContext(), "已还原到主页", Toast.LENGTH_SHORT).show()
            loadAllData()
        }
    }

    /** 删除 + Snackbar 撤销（3 秒内可恢复） */
    private fun deleteItemWithUndo(inspiration: Inspiration) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { repository.delete(inspiration.id) }
            loadAllData()
        }
        Snackbar.make(binding.root, "已删除", Snackbar.LENGTH_LONG)
            .setAction("撤销") {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.insert(inspiration.copy(id = 0))
                    }
                    loadAllData()
                }
            }
            .setActionTextColor(0xFFC9BAFF.toInt())
            .show()
    }

    // ──────────────────── Refresh ────────────────────

    /** Called by MainActivity when drawer opens to refresh data */
    fun refresh() {
        loadAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
