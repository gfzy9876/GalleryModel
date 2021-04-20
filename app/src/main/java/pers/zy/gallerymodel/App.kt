package pers.zy.gallerymodel

import android.app.Application
import androidx.multidex.MultiDex
import pers.zy.gallarylib.gallery.tools.GallaryCommon

/**
 * date: 4/16/21   time: 6:41 PM
 * author zy
 * Have a nice day :)
 **/
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        GallaryCommon.init(this)
    }

}