/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nooblol.smartnoob.core.common.overlays.menu

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton

import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.core.view.forEach
import androidx.lifecycle.Lifecycle

import com.nooblol.smartnoob.core.base.addDumpTabulationLvl
import com.nooblol.smartnoob.core.base.extensions.disableMoveAnimations
import com.nooblol.smartnoob.core.base.extensions.doWhenMeasured
import com.nooblol.smartnoob.core.base.extensions.safeAddView
import com.nooblol.smartnoob.core.base.extensions.safeUpdateViewLayout
import com.nooblol.smartnoob.core.common.overlays.R
import com.nooblol.smartnoob.core.common.overlays.base.BaseOverlay
import com.nooblol.smartnoob.core.common.overlays.di.OverlaysEntryPoint
import com.nooblol.smartnoob.core.common.overlays.manager.OverlayManager
import com.nooblol.smartnoob.core.common.overlays.menu.implementation.common.OverlayMenuAnimations
import com.nooblol.smartnoob.core.common.overlays.menu.implementation.common.OverlayMenuMoveTouchEventHandler
import com.nooblol.smartnoob.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource
import com.nooblol.smartnoob.core.common.overlays.menu.implementation.common.OverlayMenuResizeController

import dagger.hilt.EntryPoints
import java.io.PrintWriter

/**
 * Controller for a menu displayed as an overlay shown from a service.
 *
 * This class ensure that all overlay menu opened from a service will have the same behaviour. It provides basic
 * lifecycle alike methods to ease the view initialization/cleaning, as well as a menu item enabling/disabling
 * management and the moving of the menu by pressing the move item. It also provides the management of an overlay view,
 * a view that can be shown/hide as an overlay over the currently displayed activity.
 *
 * Using this class impose some restrictions on the provided views:
 * - The root layout must be a FrameLayout with the size set to wrap content.
 * - The root layout must have only one child. This child should show the background of the overlay window and should
 * have the view id [R.id.menu_background].
 * - The layout containing all menu buttons should have the view id [R.id.menu_items].
 *
 * Two menu items are supported by default and are not mandatory (if you don't need it, don't declare it in your layout).
 * Those items must be a direct child of [R.id.menu_items]:
 * - [R.id.btn_move]: the button allowing the move the overlay menu when drag and drop by the user.
 * - [R.id.btn_hide_overlay]: the button allowing to show/hide the overlay view on the screen. When hidden, the user can
 * click on the activity overlaid.
 *
 * The overlay view is created by the abstract method [onCreateOverlayView]. This view can be shown/hidden on a press by
 * the user on the [R.id.btn_hide_overlay] button.
 *
 * The position of the menu is saved in the [android.content.SharedPreferences] for each orientation.
 */
abstract class OverlayMenu(
    @StyleRes theme: Int? = null,
    private val recreateOverlayViewOnRotation: Boolean = false,
) : BaseOverlay(theme = theme, recreateOnRotation = false) {

    /** The base layout parameters of the menu layout & overlay view. */
    private val baseLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        OverlayManager.OVERLAY_WINDOW_TYPE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        disableMoveAnimations()
    }

    /** The layout parameters of the menu layout. */
    private val menuLayoutParams: WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply { copyFrom(baseLayoutParams) }

    private val animations: OverlayMenuAnimations = OverlayMenuAnimations()

    internal var resumeOnceShown: Boolean = false
        private set
    internal var destroyOnceHidden: Boolean = false
        private set

    /** The Android window manager. Used to add/remove the overlay menu and view. */
    private lateinit var windowManager: WindowManager

    /** The root view of the menu overlay. Retrieved from [onCreateMenu] implementation. */
    private lateinit var menuLayout: ViewGroup
    /** The view displaying the background of the overlay. */
    private lateinit var menuBackground: ViewGroup
    /** The view containing the buttons as direct children. */
    private lateinit var buttonsContainer: ViewGroup
    /** Handles the window size computing when animating a resize of the overlay. */
    private lateinit var resizeController: OverlayMenuResizeController
    /** Handles the touch events on the move button. */
    private lateinit var moveTouchEventHandler: OverlayMenuMoveTouchEventHandler

    /** Handles the save/load of the position of the menus. */
    private val positionDataSource: OverlayMenuPositionDataSource by lazy {
        EntryPoints.get(context.applicationContext, OverlaysEntryPoint::class.java)
            .overlayMenuPositionDataSource()
    }

    /** Value of the alpha for a disabled item view in the menu. */
    private var disabledItemAlpha: Float = 1f

    /** The hide overlay button, if provided. */
    private var hideOverlayButton: ImageButton? = null
    /** The move button, if provided. */
    private var moveButton: View? = null

    /**
     * The view to be displayed between the current activity and the overlay menu.
     * It can be shown/hidden by pressing on the menu item with the id [R.id.btn_hide_overlay]. If null, pressing this
     * button will have no effect.
     */
    protected var screenOverlayView: View? = null
    /** The layout parameters of the overlay view. */
    private lateinit var overlayLayoutParams: WindowManager.LayoutParams

    private val onLockedPositionChangedListener: (Point?) -> Unit = ::onLockedPositionChanged

    /**
     * Creates the root view of the menu overlay.
     *
     * @param layoutInflater the Android layout inflater.
     *
     * @return the menu root view. It MUST contains a view group within a depth of 2 that contains all menu items in
     *         order for move and hide to work as expected.
     */
    protected abstract fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup

    /**
     * Creates the view to be displayed between the current activity and the overlay menu.
     * It can be shown/hidden by pressing on the menu item with the id [R.id.btn_hide_overlay]. If null, pressing this
     * button will have no effect.
     *
     * @return the overlay view, or null if none is required.
     */
    protected open fun onCreateOverlayView(): View? = null

    /** Tells if the overlay view should be animated when shown/hidden. True by default. */
    protected open fun animateOverlayView(): Boolean = true

    /**
     * Creates the layout parameters for the [screenOverlayView].
     * Default implementation uses the same parameters as the floating menu, but in fullscreen.
     *
     * @return the layout parameters to apply to the overlay view.
     */
    protected open fun onCreateOverlayViewLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        copyFrom(baseLayoutParams)
        displayConfigManager.displayConfig.sizePx.let { size ->
            width = size.x
            height = size.y
        }
    }

    @CallSuper
    @SuppressLint("ResourceType")
    override fun onCreate() {
        windowManager = context.getSystemService(WindowManager::class.java)!!
        disabledItemAlpha = context.resources.getFraction(R.dimen.alpha_menu_item_disabled, 1, 1)

        // First, call implementation methods to check what we should display
        menuLayout = onCreateMenu(context.getSystemService(LayoutInflater::class.java))
        screenOverlayView = onCreateOverlayView()
        overlayLayoutParams = onCreateOverlayViewLayoutParams()

        // Set the clicks listener on the menu items
        menuBackground = menuLayout.findViewById(R.id.menu_background)
        buttonsContainer = menuLayout.findViewById(R.id.menu_items)
        setupButtons(buttonsContainer)

        // Setup the touch event handler for the move button
        moveTouchEventHandler = OverlayMenuMoveTouchEventHandler(::updateMenuPosition)

        // Restore the last menu position, if any.
        menuLayoutParams.gravity = Gravity.TOP or Gravity.START
        overlayLayoutParams.gravity = Gravity.TOP or Gravity.START
        positionDataSource.addOnLockedPositionChangedListener(onLockedPositionChangedListener)
        loadMenuPosition(displayConfigManager.displayConfig.orientation)
        moveButton?.visibility = if (positionDataSource.isPositionLocked()) View.GONE else View.VISIBLE

        // Handle window resize animations
        resizeController = OverlayMenuResizeController(
            backgroundViewGroup = menuBackground,
            resizedContainer = buttonsContainer,
            maximumSize = getWindowMaximumSize(menuBackground),
            windowResizer = ::onNewWindowSize,
        )

        // Add the overlay, if any. It needs to be below the menu or user won't be able to click on the menu.
        screenOverlayView?.let {
            if (animateOverlayView()) it.visibility = View.GONE
            if (!windowManager.safeAddView(it, overlayLayoutParams)) {
                finish()
                return
            }
        }

        // Add the menu view to the window manager, but hidden
        if (animateOverlayView()) menuBackground.visibility = View.GONE
        if (!windowManager.safeAddView(menuLayout, menuLayoutParams)) {
            finish()
            return
        }
    }

    private fun setupButtons(buttonsContainer: ViewGroup) {
        buttonsContainer.forEach { view ->
            @SuppressLint("ClickableViewAccessibility") // View is only drag and drop, no click
            when (view.id) {
                R.id.btn_move -> {
                    moveButton = view
                    view.setOnTouchListener { _: View, event: MotionEvent -> onMoveTouched(event) }
                }
                R.id.btn_hide_overlay -> {
                    hideOverlayButton = (view as ImageButton)
                    setOverlayViewVisibility(true)
                    view.setOnClickListener { onToggleOverlayVisibilityClicked() }
                }
                else -> view.setDebouncedOnClickListener { v ->
                    if (resizeController.isAnimating) return@setDebouncedOnClickListener
                    onMenuItemClicked(v.id)
                }
            }
        }
    }

    final override fun start() {
        if (lifecycle.currentState != Lifecycle.State.CREATED) return
        if (animations.showAnimationIsRunning) return

        super.start()
        loadMenuPosition(displayConfigManager.displayConfig.orientation)

        // Start the show animation for the menu
        Log.d(TAG, "Start show overlay ${hashCode()} animation...")

        val animatedOverlayView = if (animateOverlayView()) screenOverlayView else null
        menuLayout.visibility = View.VISIBLE
        menuBackground.visibility = View.VISIBLE
        animatedOverlayView?.visibility = View.VISIBLE
        animations.startShowAnimation(menuBackground, animatedOverlayView) {
            Log.d(TAG, "Show overlay ${hashCode()} animation ended")

            if (resumeOnceShown) {
                resumeOnceShown = false
                resume()
            }
        }
    }

    final override fun resume() {
        if (lifecycle.currentState == Lifecycle.State.CREATED) start()
        if (lifecycle.currentState != Lifecycle.State.STARTED) return

        if (animations.showAnimationIsRunning) {
            Log.d(TAG, "Show overlay ${hashCode()} animation is running, delaying resume...")
            resumeOnceShown = true
            return
        }

        forceWindowResize()

        super.resume()
    }

    final override fun stop() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (animations.hideAnimationIsRunning) return
        if (lifecycle.currentState == Lifecycle.State.RESUMED) pause()

        saveMenuPosition(displayConfigManager.displayConfig.orientation)

        // Start the hide animation for the menu
        Log.d(TAG, "Start overlay ${hashCode()} hide animation...")
        val animatedOverlayView = if (animateOverlayView()) screenOverlayView else null
        animations.startHideAnimation(menuBackground, animatedOverlayView) {
            Log.d(TAG, "Hide overlay ${hashCode()} animation ended")

            menuLayout.visibility = View.GONE
            menuBackground.visibility = View.GONE
            screenOverlayView?.visibility = View.GONE

            super.stop()

            if (destroyOnceHidden) {
                destroyOnceHidden = false
                destroy()
            }
        }
    }

    final override fun destroy() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) stop()

        if (animations.hideAnimationIsRunning) {
            Log.d(TAG, "Hide overlay ${hashCode()} animation is running, delaying destroy...")
            destroyOnceHidden = true
            return
        }

        // Save last user position
        positionDataSource.removeOnLockedPositionChangedListener(onLockedPositionChangedListener)
        saveMenuPosition(displayConfigManager.displayConfig.orientation)

        windowManager.removeView(menuLayout)
        screenOverlayView?.let { windowManager.removeView(it) }
        screenOverlayView = null

        resizeController.release()
        super@OverlayMenu.destroy()
    }

    /**
     * Handles the screen orientation changes.
     * It will save the menu position for the previous orientation and load and apply the correct position for the new
     * orientation.
     */
    override fun onOrientationChanged() {
        saveMenuPosition(
            if (displayConfigManager.displayConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) Configuration.ORIENTATION_PORTRAIT
            else Configuration.ORIENTATION_LANDSCAPE
        )
        loadMenuPosition(displayConfigManager.displayConfig.orientation)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)

            val overlayView = screenOverlayView ?: return
            if (recreateOverlayViewOnRotation) {
                recreateOverlayViewForRotation(overlayView)
                return
            }

            displayConfigManager.displayConfig.sizePx.let { size ->
                overlayLayoutParams.width = size.x
                overlayLayoutParams.height = size.y
            }
            windowManager.safeUpdateViewLayout(overlayView, overlayLayoutParams)
        }
    }

    /**
     * Recreates the overlay view after a screen rotation.
     * As the Z order is dependant to the addition index in the WindowManager, we need to remove
     * the menu and add it AFTER the new overlay view.
     *
     * @param oldOverlayView the overlay view before the rotation.
     */
    private fun recreateOverlayViewForRotation(oldOverlayView: View) {
        screenOverlayView = onCreateOverlayView()
        overlayLayoutParams = onCreateOverlayViewLayoutParams().apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val previousState = lifecycle.currentState
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager.apply {
            removeView(oldOverlayView)
            removeView(menuLayout)
            screenOverlayView?.let { overlayView ->
                if (!safeAddView(overlayView, overlayLayoutParams)) {
                    finish()
                    return
                }
            }

            if (!safeAddView(menuLayout, menuLayoutParams)) {
                finish()
                return
            }
        }

        lifecycleRegistry.currentState = previousState

        setOverlayViewVisibility(oldOverlayView.visibility == View.VISIBLE)
    }

    /**
     * Called when an item (other than move/hide) in the menu have been pressed.
     * @param viewId the pressed view identifier.
     */
    protected open fun onMenuItemClicked(@IdRes viewId: Int): Unit = Unit

    /**
     * Called when the visibility of the screen overlay have changed.
     * @param isVisible true if it has became visible, false if it became invisible.
     */
    protected open fun onScreenOverlayVisibilityChanged(isVisible: Boolean): Unit = Unit

    /**
     * Get the maximum size the window can take.
     * @param backgroundView the background view.
     */
    protected open fun getWindowMaximumSize(backgroundView: ViewGroup): Size {
        backgroundView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        return Size(backgroundView.measuredWidth, backgroundView.measuredHeight)
    }

    /**
     * Change the menu view visibility.
     * @param visibility the new visibility to apply.
     */
    protected fun setMenuVisibility(visibility: Int) {
        menuLayout.visibility = visibility
    }

    /**
     * Set the enabled state of a menu item.
     *
     * @param view the view of the menu item to change the state of.
     * @param enabled true to enable the view, false to disable it.
     * @param clickable true to keep the view clickable, false to ignore all clicks on the view. False by default.
     */
    protected fun setMenuItemViewEnabled(view: View, enabled: Boolean, clickable: Boolean = false) {
        view.apply {
            isEnabled = enabled || clickable
            alpha = if (enabled) 1.0f else disabledItemAlpha
        }
    }

    /**
     * Set the visibility of a menu item.
     *
     * @param view the view of the menu item to change the visibility of.
     * @param visible true for visible, false for gone.
     */
    protected fun setMenuItemVisibility(view: View, visible: Boolean) {
        Log.d(TAG, "setMenuItemVisibility for ${hashCode()}, $view to $visible")
        view.visibility = if (visible) View.VISIBLE else View.GONE

        if (!resizeController.isAnimating) forceWindowResize()
    }

    /**
     * Animates the provided layout changes.
     * Allow to use the xml property animateLayoutChanges. All changes triggering a window resize should be made using
     * this method.
     *
     * @param layoutChanges the changes triggering a resize.
     */
    protected fun animateLayoutChanges(layoutChanges: () -> Unit) {
        resizeController.animateLayoutChanges(layoutChanges)
    }

    private fun forceWindowResize() {
        Log.d(TAG, "Force window resize")
        onNewWindowSize(resizeController.measureMenuSize())
    }

    private fun onNewWindowSize(size: Size) {
        menuLayoutParams.width = size.width
        menuLayoutParams.height = size.height

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.d(TAG, "Updating menu window size: ${size.width}/${size.height}")
            windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
        }
    }

    /**
     * Handle the click on the hide overlay button.
     * Toggle the visible state of the overlay view.
     */
    private fun onToggleOverlayVisibilityClicked() {
        if (resizeController.isAnimating) return

        screenOverlayView?.let { view ->
            setOverlayViewVisibility(view.visibility != View.VISIBLE)
        }
    }

    /**
     * Change the overlay view visibility, allowing the user the click on the Activity bellow the overlays.
     * Updates the hide button state, if any.
     *
     * @param isOverlayVisible the new visibility to apply.
     */
    protected fun setOverlayViewVisibility(isOverlayVisible: Boolean) {
        screenOverlayView?.apply {

            Log.d(TAG, "setOverlayViewVisibility for ${this@OverlayMenu.hashCode()} with visibility $isOverlayVisible")

            if (isOverlayVisible) {
                visibility = View.VISIBLE
                hideOverlayButton?.setImageResource(R.drawable.ic_visible_on)
            } else {
                visibility = View.GONE
                hideOverlayButton?.setImageResource(R.drawable.ic_visible_off)
            }

            onScreenOverlayVisibilityChanged(isOverlayVisible)
        }
    }

    /**
     * Called when the user touch the [R.id.btn_move] menu item.
     * Handle the long press and move on this button in order to drag and drop the overlay menu on the screen.
     *
     * @param event the touch event occurring on the menu item.
     *
     * @return true if the event is handled, false if not.
     */
    private fun onMoveTouched(event: MotionEvent) : Boolean {
        if (resizeController.isAnimating) return false

        return moveTouchEventHandler.onTouchEvent(menuLayout, event)
    }


    /** Safe setter for the position of the overlay menu ensuring it will not be displayed outside the screen. */
    private fun updateMenuPosition(position: Point) {
        val displaySize = displayConfigManager.displayConfig.sizePx
        if (displaySize.x < menuLayout.width || displaySize.y < menuLayout.height) return

        menuLayoutParams.x = position.x.coerceIn(0, displaySize.x - menuLayout.width)
        menuLayoutParams.y = position.y.coerceIn(0, displaySize.y - menuLayout.height)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            Log.d(TAG, "Updating menu window position: ${menuLayoutParams.x}/${menuLayoutParams.y}")
            windowManager.safeUpdateViewLayout(menuLayout, menuLayoutParams)
        }
    }

    private fun loadMenuPosition(orientation: Int) {
        val savedPosition = positionDataSource.loadMenuPosition(orientation)
        if (savedPosition != null && savedPosition.x != 0 && savedPosition.y != 0) {
            updateMenuPosition(savedPosition)
        } else {
            menuLayout.doWhenMeasured {
                updateMenuPosition(
                    Point(
                        (displayConfigManager.displayConfig.sizePx.x - menuLayout.width) / 2,
                        (displayConfigManager.displayConfig.sizePx.y / 2) - menuLayout.height,
                    )
                )
            }
        }
    }

    private fun saveMenuPosition(orientation: Int) {
        positionDataSource.saveMenuPosition(
            position = Point(menuLayoutParams.x, menuLayoutParams.y),
            orientation = orientation,
        )
    }

    private fun onLockedPositionChanged(lockedPosition: Point?) {
        if (lockedPosition != null) {
            Log.d(TAG, "Locking menu position of overlay ${hashCode()}")
            moveButton?.let { setMenuItemVisibility(it, false) }
            saveMenuPosition(displayConfigManager.displayConfig.orientation)
            updateMenuPosition(lockedPosition)
        } else {
            Log.d(TAG, "Unlocking menu position of overlay ${hashCode()}")
            moveButton?.let { setMenuItemVisibility(it, true) }
            loadMenuPosition(displayConfigManager.displayConfig.orientation)
        }
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(contentPrefix)
                .append("resumeOnceShown=$resumeOnceShown; ")
                .append("destroyOnceHidden=$destroyOnceHidden; ")
                .println()

            animations.dump(writer, contentPrefix)
            positionDataSource.dump(writer, contentPrefix)
        }
    }
}

/** Tag for logs */
private const val TAG = "OverlayMenu"