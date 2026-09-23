package dev.melodify.uranophilelab.utils.customview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import dev.melodify.uranophilelab.R

class BottomSheetItemView : LinearLayout {
    private var ID: String? = ""
    private var NAME: String? = ""
    private var IMAGE_URL = ""

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    constructor(
        context: Context?,
        string: String?,
        imageUrl: String,
        id: String?
    ) : super(context) {
        init(null, 0)
        this.titleTextView?.text = string
        if (!imageUrl.isBlank()) {
            this.iconImageView?.let { imgView ->
                imgView.colorFilter = null
                imgView.imageTintList = null
                imgView.setPadding(0, 0, 0, 0)
                val cleanUrl = if (imageUrl.startsWith("http:")) imageUrl.replace("http:", "https:") else imageUrl
                Glide.with(imgView.context)
                    .load(cleanUrl.toUri())
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(imgView)
            }
        } else {
            this.iconImageView?.let { imgView ->
                imgView.setImageResource(R.drawable.baseline_person_24)
                imgView.imageTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.text_light, context?.theme)
                )
            }
        }
        ID = id
        NAME = string
        IMAGE_URL = imageUrl
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.bottom_sheet_items_custom_view, this)

        setFocusable(true)
        isClickable = true

        setOnClickListener {
            Log.i("BottomSheetItemView", "init: Clicked!!")
        }

        if (attrs == null) return

        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetItemView, defStyle, 0)

        val title = a.getString(R.styleable.BottomSheetItemView_title)
        val mExampleDrawable = a.getDrawable(R.styleable.BottomSheetItemView_android_src)

        this.titleTextView?.text = title
        if (mExampleDrawable != null) {
            this.iconImageView?.let { imgView ->
                imgView.setImageDrawable(mExampleDrawable)
                imgView.imageTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.text_light, context.theme)
                )
            }
        }

        val padding = a.getDimensionPixelSize(R.styleable.BottomSheetItemView_srcPadding, 4)
        this.iconImageView?.setPadding(padding, padding, padding, padding)

        a.recycle()
    }

    val titleTextView: TextView?
        get() = findViewById(R.id.text)

    val iconImageView: ImageView?
        get() = findViewById(R.id.icon)
}
