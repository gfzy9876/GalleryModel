package pers.zy.gallarylib.gallery.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.MultiTypeAdapter
import pers.zy.gallarylib.R
import pers.zy.gallarylib.databinding.ActivityGallaryBinding
import pers.zy.gallarylib.gallery.commons.getScreenHeight
import pers.zy.gallarylib.gallery.commons.getStatsBarHeight
import pers.zy.gallarylib.gallery.engine.GalleryMediaLoader
import pers.zy.gallarylib.gallery.model.*

class GalleryMediaActivity : AppCompatActivity(), BaseMediaViewBinder.MediaItemClickListener {

    companion object {
        private const val EXTRA_MIME_TYPE = "extra_mime_type"

        fun startShowImage(context: Context) {
            context.startActivity(Intent(context, GalleryMediaActivity::class.java).apply {
                putExtra(EXTRA_MIME_TYPE, GalleryMediaLoader.MIME_TYPE_IMAGE)
            })
        }

        fun startShowVideo(context: Context) {
            context.startActivity(Intent(context, GalleryMediaActivity::class.java).apply {
                putExtra(EXTRA_MIME_TYPE, GalleryMediaLoader.MIME_TYPE_VIDEO)
            })
        }

        fun startShowAll(context: Context) {
            context.startActivity(Intent(context, GalleryMediaActivity::class.java).apply {
                putExtra(EXTRA_MIME_TYPE, GalleryMediaLoader.MIME_TYPE_ALL)
            })
        }
    }

    private var mimeType: Int = GalleryMediaLoader.MIME_TYPE_ALL
    private lateinit var galleryMediaLoader: GalleryMediaLoader
    private lateinit var binding: ActivityGallaryBinding

    private val mediaList = mutableListOf<LocalMediaInfo>()
    private val selectedMediaList = mutableListOf<LocalMediaInfo>()
    private val mediaAdapter = MultiTypeAdapter(mediaList)
    private lateinit var mediaLayoutManager: GridLayoutManager
    private val bucketList = mutableListOf<BucketInfo>()
    private val bucketAdapter = MultiTypeAdapter(bucketList)

    private val bucketEnterAnim: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(binding.flBucket, View.TRANSLATION_Y, -binding.flBucket.height.toFloat(), 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    binding.flBucket.visibility = View.VISIBLE
                }
            })
        }
    }

    private val bucketExitAnim: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(binding.flBucket, View.TRANSLATION_Y, 0f, -binding.flBucket.height.toFloat()).apply {
            duration = 300
            interpolator = AccelerateInterpolator()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.trans_from_bottom_enter_anim, 0)
        binding = ActivityGallaryBinding.inflate(layoutInflater)
        mimeType = intent.getIntExtra(EXTRA_MIME_TYPE, GalleryMediaLoader.MIME_TYPE_ALL)
        setContentView(binding.root)
        initView()
        initMediaLoader()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.trans_from_bottom_exit_anim)
    }

    override fun onDestroy() {
        super.onDestroy()
        bucketEnterAnim.removeAllListeners()
        bucketEnterAnim.end()
        bucketExitAnim.end()
    }

    override fun mediaItemClick(localMediaInfo: LocalMediaInfo) {
        if (selectedMediaList.contains(localMediaInfo)) {
            val removeIndex = mediaList.indexOf(localMediaInfo)
            selectedMediaList.remove(localMediaInfo)
            mediaAdapter.notifyItemChanged(removeIndex, BaseMediaViewBinder.PAYLOADS_UPDATE_SELECTED_INDEX)

            val firstVisibleItemPosition = mediaLayoutManager.findFirstVisibleItemPosition()
            val lastVisibleItemPosition = mediaLayoutManager.findLastVisibleItemPosition()
            selectedMediaList.forEach {
                val position = mediaList.indexOf(it)
                if (position in firstVisibleItemPosition..lastVisibleItemPosition) {
                    mediaAdapter.notifyItemChanged(position, BaseMediaViewBinder.PAYLOADS_UPDATE_SELECTED_INDEX)
                }
            }
        } else {
            selectedMediaList.add(localMediaInfo)
            val addIndex = mediaList.indexOf(localMediaInfo)
            mediaAdapter.notifyItemChanged(addIndex, BaseMediaViewBinder.PAYLOADS_UPDATE_SELECTED_INDEX)
        }
    }

    private fun initView() {
        binding.titleBar.layoutParams = (binding.titleBar.layoutParams as LinearLayout.LayoutParams).apply {
            height += getStatsBarHeight()
        }
        binding.titleBar.setPadding(0, getStatsBarHeight(), 0, 0)
        binding.titleBar.setIconClickListener(View.OnClickListener { finish() })

        mediaAdapter.register(LocalMediaImageInfo::class, MediaImageViewBinder(selectedMediaList, this))
        mediaAdapter.register(LocalMediaVideoInfo::class, MediaVideoViewBinder(selectedMediaList, this))
        mediaLayoutManager = GridLayoutManager(this@GalleryMediaActivity, 4)
        binding.rvMedia.apply {
            adapter = mediaAdapter
            layoutManager = mediaLayoutManager
            addOnScrollListener(object : EndlessRecyclerViewScrollListener(mediaLayoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    loadMoreMedia(page)
                }
            })
        }
        bucketAdapter.register(BucketInfo::class.java, BucketBinder { bucketInfo ->
            refreshBucketSelectorState(false)
            if (galleryMediaLoader.selectBucketId != bucketInfo.id) {
                binding.root.postDelayed({
                    requestMediaWithBucketId(bucketInfo)
                }, 300)
                bucketExitAnim.start()
            }
        })
        binding.rvBucket.apply {
            adapter = this@GalleryMediaActivity.bucketAdapter
            layoutManager = LinearLayoutManager(this@GalleryMediaActivity, LinearLayoutManager.VERTICAL, false)
        }

        binding.llBucketSelector.setOnClickListener {
            if (binding.llBucketSelector.tag == true) {
                refreshBucketSelectorState(false)
            } else {
                refreshBucketSelectorState(true)
            }
        }
        binding.flBucket.setOnClickListener {
            binding.llBucketSelector.tag = false
            binding.flBucketArrow.animate().rotation(180f).setDuration(300).start()
            bucketExitAnim.start()
        }

        binding.rvBucket.maxRecyclerViewHeight = (getScreenHeight() * 0.5f).toInt()
    }

    private fun initMediaLoader() {
        galleryMediaLoader = GalleryMediaLoader(this)
        galleryMediaLoader.loadMedia(mimeType, successCall = {
            refreshMedia(it)
        })
        galleryMediaLoader.loadBucket(mimeType, {
            if (it.isNotEmpty()) {
                bucketList.addAll(it)
                bucketAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun refreshBucketSelectorState(show: Boolean) {
        if (binding.llBucketSelector.tag != show) {
            binding.llBucketSelector.tag = show
            if (show) {
                binding.flBucketArrow.animate().rotation(360f).setDuration(300).start()
                bucketEnterAnim.start()
            } else {
                binding.flBucketArrow.animate().rotation(180f).setDuration(300).start()
                bucketExitAnim.start()
            }
        }
    }

    private fun requestMediaWithBucketId(bucketInfo: BucketInfo) {
        binding.tvBucketSelector.text = bucketInfo.displayName
        galleryMediaLoader.selectBucketId = bucketInfo.id
        galleryMediaLoader.loadMedia(mimeType, successCall = {
            refreshMedia(it)
        })
    }

    private fun refreshMedia(result: List<LocalMediaInfo>) {
        mediaList.clear()
        addResult(result)
        mediaAdapter.notifyDataSetChanged()
    }

    private fun addResult(result: List<LocalMediaInfo>) {
        if (result.isEmpty()) return
        mediaList.addAll(result)
    }

    private fun loadMoreMedia(page: Int) {
        galleryMediaLoader.loadMedia(mimeType, page, successCall = {
            val orgSize = mediaList.size
            addResult(it)
            if (mediaList.size != orgSize) {
                mediaAdapter.notifyItemRangeInserted(orgSize, mediaList.size - orgSize)
            }
        })
    }
}