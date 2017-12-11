/*
 * Copyright 2017 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.dragdismissbehavior

import android.content.Context
import android.content.res.Resources
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.util.Log
import android.view.View

/**
 * A coordinator layout behavior which enables drag dismiss functionality to the view
 */
class DragDismissBehavior(context: Context? = null, attrs: AttributeSet? = null
): CoordinatorLayout.Behavior<View>(context, attrs) {

    /**
     * The animator which snaps the view back after a drag
     */
    var animator = DefaultDragDismissAnimator()
    /**
     * The callback which will be invoked on events
     */
    var callback: DragDismissCallback? = null
    /**
     * Whether drag dismiss is enabled or not
     */
    var enabled = true
    /**
     * The min distance for a dismiss
     */
    var dragDismissDistance = (500 * Resources.getSystem().displayMetrics.density)
    /**
     * The scale will in a dismissing state
     */
    var dragDismissScale = 0.95f
    /**
     * Elasticity of the view
     */
    var dragElasticity = 0.5f
    /**
     * Whether we should scale or not
     */
    var shouldScale = true

    private var totalDrag = 0f
    private var draggingDown = false
    private var draggingUp = false
    private var currentDragFraction = 0f

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View,
                                     target: View, axes: Int, type: Int): Boolean {
        return if (enabled && type.isTouch()) {
            axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
        } else {
            super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
        }
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View,
                                   dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (enabled && type.isTouch()
                && (draggingDown && dy > 0 || draggingUp && dy < 0)) {
            // if we're in a drag gesture and the user reverses up the we should take those events
            dragScale(child, dy)
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View,
                                dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        if (enabled && type.isTouch()) {
            dragScale(child, dyUnconsumed)
        }
    }

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: View,
                                    target: View, type: Int) {
        if (enabled && type.isTouch()) {
            if (Math.abs(totalDrag) >= dragDismissDistance) {
                callback?.onDragDismissed()
            } else {
                // settle back to natural position
                animator.bringBackInPlace(child)
                totalDrag = 0f
                draggingUp = false
                draggingDown = false
                callback?.onDrag(0f)
            }
        }
    }

    private fun dragScale(view: View, scroll: Int) {
        if (scroll == 0) return

        totalDrag += scroll.toFloat()

        // track the direction & set the pivot point for scaling
        // don't double track i.e. if start dragging down and then reverse, keep tracking as
        // dragging down until they reach the 'natural' position
        if (scroll < 0 && !draggingUp && !draggingDown) {
            draggingDown = true
            if (shouldScale) view.pivotY = view.height.toFloat()
        } else if (scroll > 0 && !draggingDown && !draggingUp) {
            draggingUp = true
            if (shouldScale) view.pivotY = 0f
        }

        // how far have we dragged relative to the distance to perform a dismiss
        // (0–1 where 1 = dismiss distance). Decreasing logarithmically as we approach the limit
        var dragFraction = Math.log10((1 + Math.abs(totalDrag) / dragDismissDistance).toDouble()).toFloat()
        if (dragFraction > 1f) {
            dragFraction = 1f
        }

        // calculate the desired translation given the drag fraction
        var dragTo = dragFraction * dragDismissDistance * dragElasticity

        if (draggingUp) {
            // as we use the absolute magnitude when calculating the drag fraction, need to
            // re-apply the drag direction
            dragTo *= -1f
        }

        view.translationY = dragTo

        if (shouldScale) {
            val scale = 1 - (1 - dragDismissScale) * dragFraction
            view.scaleX = scale
            view.scaleY = scale
        }

        // if we've reversed direction and gone past the settle point then clear the flags to
        // allow the list to get the scroll events & reset any transforms
        if (draggingDown && totalDrag >= 0 || draggingUp && totalDrag <= 0) {
            dragFraction = 0f
            dragTo = dragFraction
            totalDrag = dragTo
            draggingUp = false
            draggingDown = draggingUp
            view.translationY = 0f
            view.scaleX = 1f
            view.scaleY = 1f
        }

        if (currentDragFraction != dragFraction) {
            currentDragFraction = dragFraction
            callback?.onDrag(dragFraction)
        }
    }

    private fun Int.isTouch() = this == ViewCompat.TYPE_TOUCH

    companion object {
        /**
         * Returns the drag dismiss behavior the view
         */
        @JvmStatic fun from(view: View): DragDismissBehavior? {
            val params = view.layoutParams as CoordinatorLayout.LayoutParams
            return params.behavior as DragDismissBehavior
        }
    }
}

interface DragDismissCallback {
    /**
     * Will be called on drags
     */
    fun onDrag(offset: Float)

    /**
     * Will be called when the view got dismissed
     */
    fun onDragDismissed()
}

interface DragDismissAnimator {
    /**
     * Animates the view back to the values
     */
    fun bringBackInPlace(view: View)
}

class DefaultDragDismissAnimator: DragDismissAnimator {
    override fun bringBackInPlace(view: View) {
        view.animate()
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300L)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(null)
                .start()
    }
}