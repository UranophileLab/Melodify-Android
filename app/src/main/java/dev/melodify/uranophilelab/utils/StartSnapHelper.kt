package dev.melodify.uranophilelab.utils

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * A SnapHelper that snaps items to the START edge of the RecyclerView
 * (left/right depending on RTL for horizontal, top for vertical) instead of the center.
 *
 * This prevents the first/last item from being clipped when they cannot
 * scroll far enough to reach the center of the viewport.
 */
class StartSnapHelper : LinearSnapHelper() {

    private var horizontalHelper: OrientationHelper? = null
    private var verticalHelper: OrientationHelper? = null

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        // Fix potential memory leak when detached from RecyclerView
        if (recyclerView == null) {
            horizontalHelper = null
            verticalHelper = null
        }
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        val out = IntArray(2)
        val isRtl = layoutManager.canScrollHorizontally() && 
                (layoutManager.layoutDirection == View.LAYOUT_DIRECTION_RTL)

        if (layoutManager.canScrollHorizontally()) {
            out[0] = distanceToStart(targetView, getHorizontalHelper(layoutManager), isRtl)
        } else {
            out[0] = 0
        }

        if (layoutManager.canScrollVertically()) {
            out[1] = distanceToStart(targetView, getVerticalHelper(layoutManager), false)
        } else {
            out[1] = 0
        }
        return out
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        // Fix bug: Avoid snapping at the end of the list to prevent jitter/bounce
        if (layoutManager is LinearLayoutManager) {
            val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()
            if (lastVisibleItem == layoutManager.itemCount - 1) {
                return null
            }
        }

        return if (layoutManager.canScrollHorizontally()) {
            findStartView(layoutManager, getHorizontalHelper(layoutManager))
        } else {
            findStartView(layoutManager, getVerticalHelper(layoutManager))
        }
    }

    private fun distanceToStart(targetView: View, helper: OrientationHelper, isRtl: Boolean): Int {
        return if (isRtl) {
            helper.getDecoratedEnd(targetView) - helper.endAfterPadding
        } else {
            helper.getDecoratedStart(targetView) - helper.startAfterPadding
        }
    }

    private fun findStartView(
        layoutManager: RecyclerView.LayoutManager,
        helper: OrientationHelper
    ): View? {
        val childCount = layoutManager.childCount
        if (childCount == 0) return null

        var closestChild: View? = null
        val isRtl = layoutManager.canScrollHorizontally() && 
                (layoutManager.layoutDirection == View.LAYOUT_DIRECTION_RTL)
        val start = if (isRtl) helper.endAfterPadding else helper.startAfterPadding
        var absClosest = Int.MAX_VALUE

        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val childStart = if (isRtl) helper.getDecoratedEnd(child) else helper.getDecoratedStart(child)
            val absDistance = abs(childStart - start)
            if (absDistance < absClosest) {
                absClosest = absDistance
                closestChild = child
            }
        }
        return closestChild
    }

    private fun getHorizontalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (horizontalHelper == null || (horizontalHelper!!.layoutManager !== layoutManager)) {
            horizontalHelper = OrientationHelper.createHorizontalHelper(layoutManager)
        }
        return horizontalHelper!!
    }

    private fun getVerticalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (verticalHelper == null || (verticalHelper!!.layoutManager !== layoutManager)) {
            verticalHelper = OrientationHelper.createVerticalHelper(layoutManager)
        }
        return verticalHelper!!
    }
}
