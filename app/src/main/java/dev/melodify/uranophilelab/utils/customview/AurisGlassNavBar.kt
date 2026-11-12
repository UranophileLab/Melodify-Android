package dev.melodify.uranophilelab.utils.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.databinding.ViewAurisGlassNavBarBinding
import dev.melodify.uranophilelab.utils.HapticUtil

/**
 * Authentic Auris-spec Liquid Glass Floating Navigation Bar
 * Features:
 * - Full-width floating stadium glass capsule
 * - Dynamic traveling active tab pill indicator with coral-red gradient
 * - Smooth spring physics interpolation on tab switch
 * - Specular hairline highlight borders
 */
class AurisGlassNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewAurisGlassNavBarBinding

    var onTabSelectedListener: ((tabId: Int) -> Unit)? = null
    private var currentSelectedTabId: Int = R.id.nav_home

    private val tabViews = mutableMapOf<Int, View>()
    private val tabIcons = mutableMapOf<Int, ImageView>()
    private val tabLabels = mutableMapOf<Int, TextView>()

    private var pillAnimator: ValueAnimator? = null

    init {
        binding = ViewAurisGlassNavBarBinding.inflate(LayoutInflater.from(context), this)

        tabViews[R.id.nav_home] = binding.tabHome
        tabViews[R.id.nav_search] = binding.tabSearch
        tabViews[R.id.nav_library] = binding.tabLibrary

        tabIcons[R.id.nav_home] = binding.tabHomeIcon
        tabIcons[R.id.nav_search] = binding.tabSearchIcon
        tabIcons[R.id.nav_library] = binding.tabLibraryIcon

        tabLabels[R.id.nav_home] = binding.tabHomeLabel
        tabLabels[R.id.nav_search] = binding.tabSearchLabel
        tabLabels[R.id.nav_library] = binding.tabLibraryLabel

        setupClickListeners()

        post {
            movePillToTab(currentSelectedTabId, animate = false)
        }
    }

    private fun setupClickListeners() {
        tabViews.forEach { (tabId, view) ->
            view.setOnClickListener {
                if (currentSelectedTabId != tabId) {
                    HapticUtil.tap(this)
                    selectTab(tabId)
                    onTabSelectedListener?.invoke(tabId)
                }
            }
        }
    }

    fun selectTab(tabId: Int, animate: Boolean = true) {
        if (!tabViews.containsKey(tabId)) return
        currentSelectedTabId = tabId
        updateTabStyles()
        movePillToTab(tabId, animate)
    }

    private fun updateTabStyles() {
        tabViews.keys.forEach { tabId ->
            val isSelected = tabId == currentSelectedTabId
            val icon = tabIcons[tabId]
            val label = tabLabels[tabId]

            if (isSelected) {
                icon?.setColorFilter(Color.WHITE)
                label?.setTextColor(Color.WHITE)
                label?.alpha = 1.0f
                icon?.alpha = 1.0f
            } else {
                val unselectedColor = ContextCompat.getColor(context, R.color.textSec)
                icon?.setColorFilter(unselectedColor)
                label?.setTextColor(unselectedColor)
                label?.alpha = 0.7f
                icon?.alpha = 0.7f
            }
        }
    }

    private fun movePillToTab(tabId: Int, animate: Boolean) {
        val targetTab = tabViews[tabId] ?: return
        if (targetTab.width == 0) {
            targetTab.post { movePillToTab(tabId, animate) }
            return
        }

        val pill = binding.activeTabPill
        val targetLeft = targetTab.left
        val targetWidth = targetTab.width

        pillAnimator?.cancel()

        if (!animate) {
            val lp = pill.layoutParams as FrameLayout.LayoutParams
            lp.width = targetWidth
            lp.leftMargin = targetLeft
            pill.layoutParams = lp
            updateTabStyles()
            return
        }

        val initialLeft = (pill.layoutParams as FrameLayout.LayoutParams).leftMargin
        val initialWidth = pill.width

        pillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val currentLeft = (initialLeft + (targetLeft - initialLeft) * fraction).toInt()
                val currentWidth = (initialWidth + (targetWidth - initialWidth) * fraction).toInt()

                val lp = pill.layoutParams as FrameLayout.LayoutParams
                lp.width = currentWidth
                lp.leftMargin = currentLeft
                pill.layoutParams = lp
            }
            start()
        }
    }
}

