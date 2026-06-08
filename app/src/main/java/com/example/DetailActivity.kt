package com.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.data.DrugDatabaseHelper
import com.example.databinding.ActivityDetailBinding
import com.example.helper.CalculationResult
import com.example.helper.CalculatorLogic
import com.example.model.Drug
import com.google.android.material.tabs.TabLayoutMediator

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: DrugDatabaseHelper
    private var drug: Drug? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Support Edge-to-Edge and window insets elegantly
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        dbHelper = DrugDatabaseHelper(this)

        // Read intent data
        val drugId = intent.getIntExtra("DRUG_ID", -1)
        val allDrugs = dbHelper.getAllDrugs("")
        drug = allDrugs.find { it.id == drugId }

        if (drug == null) {
            // Fallback by name if ID was not supplied or match not found
            val drugName = intent.getStringExtra("DRUG_NAME") ?: ""
            if (drugName.isNotBlank()) {
                drug = allDrugs.find { it.scientificName.equals(drugName, ignoreCase = true) }
            }
        }

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val activeDrug = drug
        if (activeDrug != null) {
            binding.toolbar.title = activeDrug.scientificName
            
            // Set up Adapter with calculations callback
            val pagerAdapter = DetailPagerAdapter(activeDrug) { weight, age, customDose, concentration, callback ->
                val result = CalculatorLogic.calculateDosage(
                    activeDrug,
                    weight,
                    age,
                    customDose,
                    concentration
                )
                when (result) {
                    is CalculationResult.Success -> {
                        val doseString = if (result.volumeMl != null) {
                            "${result.doseMg} mg (${result.volumeMl} mL)"
                        } else {
                            "${result.doseMg} mg"
                        }
                        callback(doseString, result.summary, result.isMaxCapped)
                    }
                    is CalculationResult.Error -> {
                        callback("⚠️ خطأ في المدخلات", result.message, false)
                    }
                }
            }

            binding.viewPager.adapter = pagerAdapter

            // Connect TabLayout with ViewPager2 using standard TabLayoutMediator
            val tabTitles = arrayOf("معلومات الدواء", "حاسبة الجرعة", "الأشكال الصيدلانية")
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = tabTitles[position]
            }.attach()
        } else {
            binding.toolbar.title = "المستحضر غير موجود"
        }
    }

    // ViewPager2 Adapter returning simple custom views for each tab
    class DetailPagerAdapter(
        private val drug: Drug,
        private val onCalculate: (Double?, Double?, Double?, Double?, (String, String, Boolean) -> Unit) -> Unit
    ) : RecyclerView.Adapter<DetailPagerAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int = 3

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutRes = when (viewType) {
                0 -> R.layout.layout_tab_info
                1 -> R.layout.layout_tab_calculator
                else -> R.layout.layout_tab_forms
            }
            val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val view = holder.itemView
            when (position) {
                0 -> {
                    view.findViewById<TextView>(R.id.tvCategoryValue)?.text = drug.category
                    view.findViewById<TextView>(R.id.tvDefinitionValue)?.text = drug.definition
                    view.findViewById<TextView>(R.id.tvMechanismValue)?.text = drug.mechanism
                    view.findViewById<TextView>(R.id.tvUsesValue)?.text = drug.uses
                }
                1 -> {
                    val etWeight = view.findViewById<EditText>(R.id.etWeight)
                    val etAge = view.findViewById<EditText>(R.id.etAge)
                    val etCustomDose = view.findViewById<EditText>(R.id.etCustomDose)
                    val etConcentration = view.findViewById<EditText>(R.id.etConcentration)
                    val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)
                    val cardResult = view.findViewById<View>(R.id.cardResult)
                    val tvCalculatedDoseValue = view.findViewById<TextView>(R.id.tvCalculatedDoseValue)
                    val tvCalculatedDoseDisclaimer = view.findViewById<TextView>(R.id.tvCalculatedDoseDisclaimer)
                    val tvMaxDoseWarning = view.findViewById<TextView>(R.id.tvMaxDoseWarning)

                    // Seed dynamic values from model
                    if (drug.weightBased == "نعم") {
                        val parsedDose = CalculatorLogic.parseNumber(drug.dosePerKg)
                        if (parsedDose != null) {
                            etCustomDose?.setText(parsedDose.toString())
                        }
                    } else {
                        etCustomDose?.setText("0.0")
                    }

                    btnCalculate?.setOnClickListener {
                        val w = etWeight?.text?.toString()?.toDoubleOrNull()
                        val a = etAge?.text?.toString()?.toDoubleOrNull()
                        val cd = etCustomDose?.text?.toString()?.toDoubleOrNull()
                        val c = etConcentration?.text?.toString()?.toDoubleOrNull()

                        onCalculate(w, a, cd, c) { resultDose, summary, isCapped ->
                            cardResult?.visibility = View.VISIBLE
                            tvCalculatedDoseValue?.text = resultDose
                            tvCalculatedDoseDisclaimer?.text = summary
                            tvMaxDoseWarning?.visibility = if (isCapped) View.VISIBLE else View.GONE
                        }
                    }
                }
                2 -> {
                    view.findViewById<TextView>(R.id.tvFormsValue)?.text = drug.dosageForms
                    view.findViewById<TextView>(R.id.tvAdministrationValue)?.text = drug.administration
                    view.findViewById<TextView>(R.id.tvPrecautionsValue)?.text = drug.precautions
                    view.findViewById<TextView>(R.id.tvSideEffectsValue)?.text = drug.sideEffects
                    view.findViewById<TextView>(R.id.tvInteractionsValue)?.text = drug.interactions
                }
            }
        }
    }
}
