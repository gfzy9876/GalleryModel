package pers.zy.gallarylib.gallery.tools

import android.os.Build
import android.util.TypedValue
import android.widget.Toast
import pers.zy.gallarylib.gallery.GalleryApp

/**
 * date: 2020/6/7   time: 5:45 PM
 * author zy
 * Have a nice day :)
 **/

object Common {
    fun getStatsBarHeight(): Int {
        var height = 0
        val resId = GalleryApp.INSTANCE.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resId > 0) {
            height = GalleryApp.INSTANCE.resources.getDimensionPixelSize(resId)
        }
        return height
    }

    fun getScreenHeight(): Int = GalleryApp.INSTANCE.resources.displayMetrics.heightPixels

    fun lessThanAndroidQ(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun makeToast(msg: String) {
        Toast.makeText(GalleryApp.INSTANCE, msg, Toast.LENGTH_SHORT).show()
    }
}



val Float.dp: Int
    get() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, GalleryApp.INSTANCE.resources.displayMetrics).toInt()
    }

val Float.dpF: Float
    get() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, GalleryApp.INSTANCE.resources.displayMetrics)
    }
