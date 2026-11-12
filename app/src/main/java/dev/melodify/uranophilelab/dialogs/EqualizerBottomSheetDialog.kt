package dev.melodify.uranophilelab.dialogs

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import dev.melodify.uranophilelab.R
import dev.melodify.uranophilelab.model.EqPreset
import dev.melodify.uranophilelab.model.EqPresets
import dev.melodify.uranophilelab.utils.EqualizerManager
import dev.melodify.uranophilelab.utils.customview.EqBandSliderView
import dev.melodify.uranophilelab.utils.customview.EqCurveView
import kotlin.math.abs

class EqualizerBottomSheetDialog(context: Context) :
    BottomSheetDialog(context, R.style.MyBottomSheetDialogTheme),
    EqualizerManager.EqualizerChangeListener {

    private val sliders = mutableListOf<EqBandSliderView>()
    private var masterSwitch: MaterialSwitch? = null
    private var btnReset: TextView? = null
    private var eqSubtitle: TextView? = null
    private var presetDescriptionText: TextView? = null
    private var liveCurveOverlay: EqCurveView? = null
    private var categoriesContainer: LinearLayout? = null

    private val presetAdapters = mutableListOf<PresetAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = layoutInflater.inflate(R.layout.dialog_equalizer_bottom_sheet, null)
        setContentView(view)

        masterSwitch = view.findViewById(R.id.masterSwitch)
        btnReset = view.findViewById(R.id.btnReset)
        eqSubtitle = view.findViewById(R.id.eqSubtitle)
        presetDescriptionText = view.findViewById(R.id.presetDescriptionText)
        liveCurveOverlay = view.findViewById(R.id.liveCurveOverlay)
        categoriesContainer = view.findViewById(R.id.presetCategoriesContainer)

        sliders.clear()
        val sliderIds = intArrayOf(
            R.id.bandSlider0, R.id.bandSlider1, R.id.bandSlider2, R.id.bandSlider3, R.id.bandSlider4,
            R.id.bandSlider5, R.id.bandSlider6, R.id.bandSlider7, R.id.bandSlider8, R.id.bandSlider9
        )

        for (i in sliderIds.indices) {
            val slider = view.findViewById<EqBandSliderView>(sliderIds[i])
            slider.bandIndex = i
            slider.onGainChangedListener = { bandIdx, gainDb ->
                EqualizerManager.setBandGain(bandIdx, gainDb)
            }
            sliders.add(slider)
        }

        masterSwitch?.isChecked = EqualizerManager.isEnabled
        masterSwitch?.setOnCheckedChangeListener { _, isChecked ->
            EqualizerManager.setEnabled(isChecked)
            updateSlidersEnabled(isChecked)
        }

        btnReset?.setOnClickListener {
            EqualizerManager.reset()
        }

        buildPresetCategories()
        updateUI(EqualizerManager.isEnabled, EqualizerManager.currentPresetId, EqualizerManager.gains)

        EqualizerManager.addListener(this)
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EqualizerManager.removeListener(this)
    }

    override fun onEqualizerStateChanged(enabled: Boolean, presetId: String?, gains: FloatArray) {
        updateUI(enabled, presetId, gains)
    }

    private fun updateSlidersEnabled(enabled: Boolean) {
        for (slider in sliders) {
            slider.isEnabled = enabled
            slider.alpha = if (enabled) 1.0f else 0.4f
        }
        liveCurveOverlay?.alpha = if (enabled) 1.0f else 0.3f
    }

    private fun updateUI(enabled: Boolean, presetId: String?, gains: FloatArray) {
        if (masterSwitch?.isChecked != enabled) {
            masterSwitch?.isChecked = enabled
        }
        updateSlidersEnabled(enabled)

        for (i in 0 until 10) {
            if (i < gains.size && i < sliders.size) {
                sliders[i].setGain(gains[i], notify = false)
            }
        }

        // Live curve update
        liveCurveOverlay?.setGains(gains, isSelected = enabled, accentColor = Color.parseColor("#F43F5E"))

        // Reset button state (dimmed when flat)
        val isFlat = gains.all { abs(it) < 0.05f }
        btnReset?.isEnabled = !isFlat
        btnReset?.alpha = if (isFlat) 0.4f else 1.0f

        val preset = EqPresets.getPresetById(presetId)
        val activeName = preset?.name ?: "Custom"
        val statusText = if (enabled) "Active: $activeName" else "Disabled"
        eqSubtitle?.text = statusText
        eqSubtitle?.setTextColor(if (enabled) Color.parseColor("#F43F5E") else Color.parseColor("#888888"))

        if (preset != null) {
            presetDescriptionText?.text = preset.description
        } else {
            presetDescriptionText?.text = "Custom curve — no preset selected."
        }

        for (adapter in presetAdapters) {
            adapter.setSelectedPresetId(presetId)
        }
    }

    private fun buildPresetCategories() {
        categoriesContainer?.removeAllViews()
        presetAdapters.clear()

        val padding20Px = (20 * context.resources.displayMetrics.density).toInt()

        for ((category, presetIds) in EqPresets.kEqPresetCategories) {
            val presets = presetIds.mapNotNull { EqPresets.getPresetById(it) }
            if (presets.isEmpty()) continue

            // Header
            val headerTv = TextView(context).apply {
                text = category.uppercase()
                setTextColor(Color.parseColor("#888888"))
                textSize = 10.5f
                paint.isFakeBoldText = true
                letterSpacing = 0.12f
                setPadding(padding20Px, 16, padding20Px, 8)
            }
            categoriesContainer?.addView(headerTv)

            // Horizontal RecyclerView with Edge-to-Edge Bleed
            val recyclerView = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
                clipToPadding = false
                setPadding(padding20Px, 0, padding20Px, 0)
            }

            val adapter = PresetAdapter(presets, EqualizerManager.currentPresetId, recyclerView) { selectedPreset ->
                EqualizerManager.applyPreset(selectedPreset)
            }
            presetAdapters.add(adapter)
            recyclerView.adapter = adapter

            categoriesContainer?.addView(
                recyclerView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private class PresetAdapter(
        private val presets: List<EqPreset>,
        private var selectedPresetId: String?,
        private val recyclerView: RecyclerView,
        private val onPresetSelected: (EqPreset) -> Unit
    ) : RecyclerView.Adapter<PresetAdapter.ViewHolder>() {

        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val cardContainer: MaterialCardView = view.findViewById(R.id.cardContainer)
            val eqCurveView: EqCurveView = view.findViewById(R.id.eqCurveView)
            val presetNameText: TextView = view.findViewById(R.id.presetNameText)
        }

        fun setSelectedPresetId(id: String?) {
            if (selectedPresetId != id) {
                selectedPresetId = id
                notifyDataSetChanged()

                val index = presets.indexOfFirst { it.id == id }
                if (index >= 0) {
                    recyclerView.smoothScrollToPosition(index)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_eq_preset_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val preset = presets[position]
            val isSelected = preset.id == selectedPresetId

            holder.presetNameText.text = preset.name
            holder.presetNameText.setTextColor(
                if (isSelected) Color.parseColor("#F43F5E") else Color.parseColor("#E0E0E0")
            )

            holder.eqCurveView.setGains(
                preset.gains,
                isSelected = isSelected,
                accentColor = Color.parseColor("#F43F5E")
            )

            val surfaceColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurface)
            holder.cardContainer.setCardBackgroundColor(
                if (isSelected) Color.argb(38, 244, 63, 94) else surfaceColor
            )

            holder.cardContainer.strokeColor =
                if (isSelected) Color.parseColor("#F43F5E") else Color.parseColor("#33888888")
            holder.cardContainer.strokeWidth = if (isSelected) 4 else 2

            holder.cardContainer.setOnClickListener {
                onPresetSelected(preset)
            }
        }

        override fun getItemCount(): Int = presets.size
    }
}
