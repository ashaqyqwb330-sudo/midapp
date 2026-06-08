package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.model.Drug
import com.example.model.RecentCalc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrugDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "drug_guide.db"
        private const val DATABASE_VERSION = 4
        
        const val TABLE_DRUGS = "Drugs"
        const val KEY_ID = "id"
        const val KEY_SCIENTIFIC_NAME = "scientific_name"
        const val KEY_CATEGORY = "category"
        const val KEY_DEFINITION = "definition"
        const val KEY_MECHANISM = "mechanism"
        const val KEY_USES = "uses"
        const val KEY_DOSAGE_GENERAL = "dosage_general"
        const val KEY_DOSAGE_FORMS = "dosage_forms"
        const val KEY_SIDE_EFFECTS = "side_effects"
        const val KEY_CONTRAINDICATIONS = "contraindications"
        const val KEY_INTERACTIONS = "interactions"
        const val KEY_ADMINISTRATION = "administration"
        const val KEY_PRECAUTIONS = "precautions"
        
        const val KEY_WEIGHT_BASED = "weight_based"
        const val KEY_DOSE_PER_KG = "dose_per_kg"
        const val KEY_MAX_DAILY_DOSE = "max_daily_dose"
        const val KEY_AGE_DEPENDENT = "age_dependent"
        const val KEY_AGE_FORMULA = "age_formula"

        // History table
        const val TABLE_HISTORY = "CalculationHistory"
        const val KEY_HIST_ID = "id"
        const val KEY_HIST_TYPE = "calc_type"
        const val KEY_HIST_INPUTS = "inputs"
        const val KEY_HIST_RESULT = "result"
        const val KEY_HIST_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_DRUGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_SCIENTIFIC_NAME TEXT,
                $KEY_CATEGORY TEXT,
                $KEY_DEFINITION TEXT,
                $KEY_MECHANISM TEXT,
                $KEY_USES TEXT,
                $KEY_DOSAGE_GENERAL TEXT,
                $KEY_DOSAGE_FORMS TEXT,
                $KEY_SIDE_EFFECTS TEXT,
                $KEY_CONTRAINDICATIONS TEXT,
                $KEY_INTERACTIONS TEXT,
                $KEY_ADMINISTRATION TEXT,
                $KEY_PRECAUTIONS TEXT,
                $KEY_WEIGHT_BASED TEXT,
                $KEY_DOSE_PER_KG TEXT,
                $KEY_MAX_DAILY_DOSE TEXT,
                $KEY_AGE_DEPENDENT TEXT,
                $KEY_AGE_FORMULA TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)

        val createHistoryTable = """
            CREATE TABLE $TABLE_HISTORY (
                $KEY_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_HIST_TYPE TEXT,
                $KEY_HIST_INPUTS TEXT,
                $KEY_HIST_RESULT TEXT,
                $KEY_HIST_TIMESTAMP TEXT
            )
        """.trimIndent()
        db.execSQL(createHistoryTable)

        try {
            seedDrugs(db)
        } catch (e: Exception) {
            Log.e("DrugDB", "Error seeding database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DRUGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun getAllDrugs(searchQuery: String = ""): List<Drug> {
        val list = mutableListOf<Drug>()
        val db = this.readableDatabase
        val cursor = if (searchQuery.isBlank()) {
            db.query(TABLE_DRUGS, null, null, null, null, null, "$KEY_SCIENTIFIC_NAME ASC")
        } else {
            db.query(
                TABLE_DRUGS,
                null,
                "$KEY_SCIENTIFIC_NAME LIKE ? OR $KEY_USES LIKE ? OR $KEY_CATEGORY LIKE ? OR $KEY_DEFINITION LIKE ?",
                arrayOf("%$searchQuery%", "%$searchQuery%", "%$searchQuery%", "%$searchQuery%"),
                null, null, "$KEY_SCIENTIFIC_NAME ASC"
            )
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    val drug = Drug(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                        scientificName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SCIENTIFIC_NAME)) ?: "",
                        category = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY)) ?: "",
                        definition = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DEFINITION)) ?: "",
                        mechanism = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MECHANISM)) ?: "",
                        uses = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USES)) ?: "",
                        dosageGeneral = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSAGE_GENERAL)) ?: "",
                        dosageForms = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSAGE_FORMS)) ?: "",
                        sideEffects = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SIDE_EFFECTS)) ?: "",
                        contraindications = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTRAINDICATIONS)) ?: "",
                        interactions = cursor.getString(cursor.getColumnIndexOrThrow(KEY_INTERACTIONS)) ?: "",
                        administration = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ADMINISTRATION)) ?: "",
                        precautions = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRECAUTIONS)) ?: "",
                        weightBased = cursor.getString(cursor.getColumnIndexOrThrow(KEY_WEIGHT_BASED)) ?: "لا",
                        dosePerKg = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSE_PER_KG)) ?: "",
                        maxDailyDose = cursor.getString(cursor.getColumnIndexOrThrow(KEY_MAX_DAILY_DOSE)) ?: "",
                        ageDependent = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AGE_DEPENDENT)) ?: "لا",
                        ageFormula = cursor.getString(cursor.getColumnIndexOrThrow(KEY_AGE_FORMULA)) ?: ""
                    )
                    list.add(drug)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        return list
    }

    // Calculations History functions
    fun saveCalculation(type: String, inputs: String, result: String) {
        try {
            val db = this.writableDatabase
            val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date())
            
            val values = ContentValues().apply {
                put(KEY_HIST_TYPE, type)
                put(KEY_HIST_INPUTS, inputs)
                put(KEY_HIST_RESULT, result)
                put(KEY_HIST_TIMESTAMP, dateStr)
            }
            db.insert(TABLE_HISTORY, null, values)
        } catch (e: Exception) {
            Log.e("DrugDB", "Error saving calculation", e)
        }
    }

    fun getRecentCalculations(): List<RecentCalc> {
        val list = mutableListOf<RecentCalc>()
        val db = this.readableDatabase
        try {
            val cursor = db.query(TABLE_HISTORY, null, null, null, null, null, "$KEY_HIST_ID DESC", "30")
            if (cursor.moveToFirst()) {
                do {
                    val entry = RecentCalc(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HIST_ID)),
                        type = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HIST_TYPE)) ?: "",
                        inputs = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HIST_INPUTS)) ?: "",
                        result = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HIST_RESULT)) ?: "",
                        timestamp = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HIST_TIMESTAMP)) ?: ""
                    )
                    list.add(entry)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("DrugDB", "Error reading calculations", e)
        }
        return list
    }

    fun deleteCalculation(id: Int) {
        try {
            val db = this.writableDatabase
            db.delete(TABLE_HISTORY, "$KEY_HIST_ID = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            Log.e("DrugDB", "Error deleting calculation", e)
        }
    }

    fun clearCalculations() {
        try {
            val db = this.writableDatabase
            db.delete(TABLE_HISTORY, null, null)
        } catch (e: Exception) {
            Log.e("DrugDB", "Error clearing calculations", e)
        }
    }

    private fun seedDrugs(db: SQLiteDatabase) {
        val drugs = listOf(
            // 1. Adrenaline
            Drug(
                scientificName = "Adrenaline (Epinephrine)",
                category = "🚨 أدوية الطوارئ والإنعاش القلبي",
                definition = "هرمون ناقل عصبي ينتمي لعائلة الكاتيكولامينات، يفرز طبيعياً ويحجز للإنعاش السريع.",
                mechanism = "تحفيز قوي ومكثف لمستقبلات ألفا وبيتا الأدرينرجية لزيادة الانقباض القلبي، رفع النبض وتوسيع القصبات والمسالك الهوائية وقبض الأوردة.",
                uses = "الإنعاش القلبي الرئوي (CPR)، صدمة الحساسية المفرطة (Anaphylaxis)، نوبات الربو الحادة القاتلة وتمديد أثر المخدرات الموضعية.",
                dosageGeneral = "- CPR: 1 ملجم للبالغين عيار وريدي كل 3-5 دقائق.\n- حساسية مفرطة: 0.3-0.5 ملجم عضلياً أو تحت الجلد.\n- الربو: 0.3-0.5 ملجم عضلياً.",
                dosageForms = "أمبول 1mg/1ml (تخفيف 1:1000)، أمبول 1mg/10ml (تخفيف 1:10,000) جاهز للوريد.",
                sideEffects = "تسارع وخفقان قوي بالقلب، أرق حاد، ارتفاع ضغط الدم المفاجئ، نزف دماغي محتمل وصداع حاد ونبض مضطرب.",
                contraindications = "بطء النبض العضوي، الرجفان البطيني (في الحفظ العادي لغير الإنعاش)، ضغط العين المرتفع الشديد والحمل إلا للضرورة القصوى.",
                interactions = "يزداد مفعوله وسميته التنازلية بالتزامن مع مغلقات بيتا والديجوكسين والمهدئات المسببة لارتجاف القلب.",
                administration = "حقن وريدي (IV)، حقن عضلي (IM)، حقن تحت الجلد (SC)، استنشاق رذاذ، أنبوب رغامي.",
                precautions = "يراعى مراقبة مؤشرات النبض والضغط الشرياني عن كثب، الحذر التام لتفادي تسرب التركيز خارج الأوردة لئلا يحدث نخر بالأنسجة.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "حسب الحاجة الحيوية بالإنعاش",
                ageDependent = "لا",
                ageFormula = "للصدمات التحسسية: 0.01 ملجم/كجم للطفل بحد أقصى 0.3 ملجم للمرة الواحدة."
            ),
            // 2. Atropine
            Drug(
                scientificName = "Atropine",
                category = "🚨 أدوية الطوارئ والإنعاش القلبي",
                definition = "مضاد للكولين يعمل كمثبط للباراسمبثاوي لرفع سرعة النبض وتجفيف الإفرازات المخاطية.",
                mechanism = "حجب عكوس لمستقبلات الأستيل كولين الموسكارينية مما يمنع تأثيرات العصب الحائر الرافع لنبض الدم ومفرزات الخلايا.",
                uses = "علاج بطء نبض القلب المرضي، ترياق تسمم غاز الأعصاب العسكري والفسفور العضوي، تقليل الإفرازات الرئوية وتوسيع حدقة العين.",
                dosageGeneral = "- CPR: 1 ملجم وريدياً (تكرر كل 3-5 دقائق حتى سقف 3 ملجم).\n- بطء القلب: 0.5 إلى 1 ملجم وريدياً.\n- الترياق الفسفوري: 2 ملجم حقناً كل 15 دقيقة حتى جفاف الصدر.",
                dosageForms = "أمبول 0.5mg/1ml، أمبول 1mg/1ml، قطرات للعين 1% ومرهم تمديدي.",
                sideEffects = "جفاف حاد بالفم والحلق والجلد، تسارع نبض القلب الحاد، تشوش النظر، احتباس البول، الـهذيان الباراسمبثاوي السام.",
                contraindications = "الجلوكوما (ارتفاع ضغط العين)، تضخم البروستاتا الحاد، ضيق القولون التقرحي، الوهن العضلي الوبيل الشديد.",
                interactions = "يزيد من فاعلية مضادات الهستامين المركزة ومهدئات الفصام ويقلل امتصاص الكيتوكونازول المعوي.",
                administration = "حقن وريدي (IV)، حقن عضلي (IM)، تحت الجلد (SC)، عبر الأنبوب الرغامي.",
                precautions = "الحذر التام مع كبار السن لخطورة الاضطراب الذهني، ويجب التحقق من جاهزية وسائل التهوية والإنعاش الرئوي دائماً.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "3 ملجم (ما عدا التسمم بالغازات فحسب الجفاف الحنجري والأعرج)",
                ageDependent = "لا",
                ageFormula = "الأطفال بالتسمم: 0.05 ملجم/كجم مع تكراره المناسب."
            ),
            // 3. Sodium Bicarbonate
            Drug(
                scientificName = "Sodium Bicarbonate",
                category = "🚨 أدوية الطوارئ والإنعاش القلبي",
                definition = "قاعدة قاعدية كيميائية قوية ترشح بالوريد لمعادلة حموضة الخلايا والدم الفسيولوجية.",
                mechanism = "تحرير شق البيكربونات الفعال ليرتبط بالأيون الهيدروجيني الحامضي مشكلاً حمض كربونيك يتكسر لماء وغاز زفير مطهر.",
                uses = "تصحيح حمضية الدم الحادة (السكر الكيتوني أو الفشل الكلوي والميدان التكتيكي)، فرط البوتاسيوم الشرياني وترياق الجرعات المفرطة من الأسبرين.",
                dosageGeneral = "- وريدياً بطيئاً: 1.1 mmol/kg (ما يعادل 1 إلى 2 ملجم لكل كجم ببطء شديد).",
                dosageForms = "محلول تسريب وريدي بتركيزات (4.2% أو 8.4%) بقوارير عيار 50 مل و100 مل.",
                sideEffects = "القلاء الدموي الحاد، نقص مستوى الكالسيوم والبوتاسيوم الفسيولوجي بالبلازما، احتباس الصوديوم والمياه الرئوية.",
                contraindications = "مرضى القلاء الطبيعي أو القلوي بالدم، الحماض التنفسي النقي المتبوع بانسداد المسالك، فقدان الكلور بالقيء المفرط.",
                interactions = "يرسب محاليل الكالسيوم بالتالي يمنع حقنهما معاً بذات المغذي الوريدي، ويقصر تأثير الأسبرين البولي المتردد.",
                administration = "حقن وتسريب وريدي بطيء وحصري (IV only).",
                precautions = "احذر بشدة تسربه تحت الجلد مسبباً تخريشاً ونخراً للخلايا العضلية الطرفية، وراقب فحص غازات الدم pH بدقة.",
                weightBased = "نعم",
                dosePerKg = "1.1",
                maxDailyDose = "حسب نتائج غازات الدم والتحليل",
                ageDependent = "لا",
                ageFormula = "يخفف بالماء المعقم للرضع لتركيز: 0.5 mEq/ml قبل الحقن لمنع النزيف المعوي الدماغي الخلوي."
            ),
            // 4. Thiopental
            Drug(
                scientificName = "Thiopental Sodium",
                category = "💊 مسكنات الآلام والتخدير العسكري",
                definition = "مخدر عام سريع المفعول ينتمي لعائلة الباربيتورات المهدئة للجهاز المركزي.",
                mechanism = "تنشيط قنوات الكلور المسؤولة عن مستقبلات GABA في الدماغ مسبباً تثبيطاً ممتداً فاعلاً للوعي والتشنجات.",
                uses = "حث التخدير السريع قبل العمليات الجراحية الميدانية، السيطرة على حالات الصرع المستعصية وخفض الضغط داخل القحف.",
                dosageGeneral = "- البالغين: 3-5 ملجم/كجم وريدياً ببطء شديد لحث النوم.\n- الأطفال: 5-8 ملجم/كجم وريدياً.",
                dosageForms = "قوارير مسحوق جاف للحل عيار: 0.5 جم و 1 جم.",
                sideEffects = "هبوط ضغط الدم والنبض الحاد، تثبيط المركز التنفسي بالمخ، السعال الحنجري التشنجي الشديد والإدمان.",
                contraindications = "موانع التخدير الشائعة، الحساسية لعائلة الباربيتورات، مرض البورفيريا النادر والربو الصدري الشديد التصلب.",
                interactions = "يزيد تآزرياً من قوة المهدئات المورفينية والباراسامبثاوية المثبطة للروح العصبية المركزية.",
                administration = "حقن وريدي بطيء بفتحة مضمونة عريضة (IV Only).",
                precautions = "يمنع كلياً حقنه في العضل أو الشرايين لتجنب موت العضو والغنغرينا، وتأكد من ملائمة المحلول المحلول طازجاً (خلال 24 ساعة).",
                weightBased = "نعم",
                dosePerKg = "4.0",
                maxDailyDose = "1000 ملجم لتفادي غيبوبة التراكم الشحمي الجسدي",
                ageDependent = "نعم",
                ageFormula = "البالغين: 3-5 مجم/كجم. الأطفال: 5-8 مجم/كجم حقناً بالوريد التدريجي السريري."
            ),
            // 5. Ketamine HCL
            Drug(
                scientificName = "Ketamine HCL",
                category = "💊 مسكنات الآلام والتخدير العسكري",
                definition = "مخدر عام تفارقي فائق الفاعلية والتميز للميدان العسكري والجروح المباشرة لكونه يحفظ الضغط الرئوي والنبض.",
                mechanism = "حظر مستقبلات NMDA بالقشرة الدماغية والمهاد مما يعطل نقل الوعي والذاكرة والألم مع إبقاء المسالك التنفسية مفتوحة.",
                uses = "تخدير العمليات الصغرى وسحب المعرضين للكسور الرضحية بالميدان، وتخدير من يعانون من هبوط حاد بضغط الدم.",
                dosageGeneral = "- حث التخدير وريدياً: 1-2 ملجم/كجم ببطء.\n- حث التخدير عضلياً: 4-8 ملجم/كجم.\n- الصيانة والتمديد للتخدير: 0.5 ملجم/كجم كل ربع ساعة مبرمج.",
                dosageForms = "قوارير حقن معقمة عيار: 10mg/ml و50mg/ml بحجوم 10 مل و20 مل.",
                sideEffects = "هلوسات ما بعد اليقظة الحادة، أحلام مفزعة، تسارع نبض القلب الخفيف وارتفاع طفيف بضغط الدم، تشنج حنجري نادر.",
                contraindications = "التحسس، صدمات الرأس المصحوبة بضغط مخ داخلي مرتفع، وارتفاع ضغط الشرايين الحرج غير المتقاعد والمستقل.",
                interactions = "تطول فترته المنومة وتتزايد سميته بالتزامن مع الباربيتورات عالية الحركية ومثبطات الأعصاب المركزية المورفينية.",
                administration = "حقن وريدي بطيء (IV over 60 seconds) أو حقن عضلي عميق ومؤهل (IM).",
                precautions = "يتوجب حقنه ببرود وهدوء بالغ لتفادي صعوبة أو انقطاع التنفس العابر، ويرافق المريض في غرفته بالهدوء.",
                weightBased = "نعم",
                dosePerKg = "1.5",
                maxDailyDose = "500 ملجم للجرعة الواحدة",
                ageDependent = "لا",
                ageFormula = "الجرعة المعتمدة هي 1-2 ملجم لكل كجم وريدياً للكبار والأطفال لسهولة تطبيقها التكتيكي."
            ),
            // 6. Propofol
            Drug(
                scientificName = "Propofol",
                category = "💊 مسكنات الآلام والتخدير العسكري",
                definition = "مخدر عام منوم فائق السرعة وخفيف البقاء، مناسب للتجهيزات المحمولة العصرية.",
                mechanism = "تحفيز مباشر غامر لمستقبلات حمض الغاما-أمينوبيوتيريك (GABA-A) المانعة لإشارات التفكير العصبي العقلي بالدماغ.",
                uses = "الحث الأولي وتدبير تخدير وبقاء المرضى المصابين على وحدات التنفس الاصطناعي بغرف العناية والإصابات المركبة.",
                dosageGeneral = "- حث أولي: 2-5 ملجم/كجم وريدياً على مدة 60 ثانية للتأقلم الحسي والتأقلم العصبي.\n- صيانة تنفسية: 25-50 ميكروجرام/كجم/دقيقة.",
                dosageForms = "مستحلب حليبي دهني للحقن والرش الوريدي بتركيز: 10mg/ml لقوارير عيار 20 مل و50 مل.",
                sideEffects = "انخفاض شديد وحاد بالضغط الشرياني ونبضات القلب، ألم حارق بموقع الوريد قبل ذوبان المفعول، وانقطاع تنفسي مؤقت.",
                contraindications = "التحسس الخاص للبروبوفول أو لمكونات الدسم الصويا والبيض الغذائي، وللترشيح الذاتي غير المنتقى لعضلات الكبد.",
                interactions = "يتسبب بوقوع سكتة تنفسية ممتدة أو غيبوبة طويلة الأجل عند مشاركة الحاصرات المورفينية بالتناوب المباشر الدقيق.",
                administration = "حقن وريدي بطيء وتدريجي عبر أوردة عريضة (IV Only).",
                precautions = "الحذر التام من كفاءة التعقيم لكونه مستحلب دهني يقوي من نمو البكتريا السام، لذا تخلص من أي عبوة بعد 12 ساعة من حلها.",
                weightBased = "نعم",
                dosePerKg = "2.5",
                maxDailyDose = "500 ملجم",
                ageDependent = "لا",
                ageFormula = "يفضل تقييم الوزن بدقة وتجنيب إعطائه لمرضى الـصدمات غير المعوضة وريدياً لكونه يثبط القلب بشراسة دموية."
            ),
            // 7. Phenytoin
            Drug(
                scientificName = "Phenytoin",
                category = "🧠 أدوية الصرع والجهاز العصبي",
                definition = "مضاد تشنجات وصرع تركيبي موجه وعامل كابح لارتجافات وتشنجات المخ الشديدة.",
                mechanism = "حصر قنوات الصوديوم النشطة المعتمدة على الجهد العصبي بالخلايا العصبية مما يحد من انتشار التحفيز العشوائي.",
                uses = "علاج والوقاية من صرع الميدان وحالات السيطرة الشديدة بعد ارتجاج أو فتح جمجمة بسبب رصاص وإصابات القحف الشريكة.",
                dosageGeneral = "- الحالات الطارئة: 15 ملجم/كجم من الوزن وريدياً تدريجياً ببطء.\n- الجرعة اليومية المستقرة: 3-4 ملجم/كجم مقسمة على جرعات فموية.",
                dosageForms = "أمبول حقن عالي التموج 50mg/ml، شراب معلق 30 ملجم/5 مل، كبسولات فموية 100 ملجم.",
                sideEffects = "رعشة واحمرار ورؤية مزدوجة، انخفاض الضغط البطيني وتأثر القلب، تضخم ملموس باللثة وتدهور فقر الدم.",
                contraindications = "بطء النبض والارتجاف البطيني المزمن للقلب، حساسية عائلة الهيدانتوين والحمل لكونه مسبباً لتشوهات.",
                interactions = "ينبض ويتفاعل مع الأسبرين والكورتيزول المعوض المعوي ويسرع استقلاب موانع الطوارئ الهرمونية.",
                administration = "حقن وريدي بطيء جداً عيار (IV only) أو تعاطي ومضاد فموي (PO).",
                precautions = "يراعى عدم تخفيفه أبداً بغير النورمال سالين المعتمد طبيعياً (0.9% NaCl) لئلا يترسب المستحضر داخل الأنبوب التوصيلي المباشر.",
                weightBased = "نعم",
                dosePerKg = "15.0",
                maxDailyDose = "1000 ملجم للجرعة الوريدية الواحدة وتجنب السرعة الكبيرة بالحفظ",
                ageDependent = "لا",
                ageFormula = "الجرعة الميدانية للحقن الطارئ تحسب على أساس 15 مجم لكل كجم لتغطية السطح الكامل لجهد الفعل العصبي الطائر."
            ),
            // 8. Diazepam
            Drug(
                scientificName = "Diazepam",
                category = "🧠 أدوية الصرع والجهاز العصبي",
                definition = "مهدئ ومضاد تشنجات ومرخي عضلي فاعل وقوي ينتمي لفئة البنزوديازيبينات المقاومة للجهد التكتيكي.",
                mechanism = "تعزيز وتسهيل عمل مستقبلات حمض غاما-أمينوبيوتيريك (GABA) في الدماغ لتثبيط الحركية الزائدة للأعصاب.",
                uses = "النوبات التشنجية المتتالية القاتلة (Status Epilepticus)، القلق والخوف والتشنج العضلي الميداني والأرق الحاد التوتر.",
                dosageGeneral = "- وريدياً أو عضلياً للبالغين: 0.1 إلى 0.2 ملجم/كجم.\n- للأطفال: 0.15 ملجم/كجم ببطء ورورياً.",
                dosageForms = "أمبول زيتي 10mg/2ml، أقراص فموية: 5 ملجم و10 ملجم، تحميلات شرجية للأطفال عيار 10 ملجم.",
                sideEffects = "نعاس وتدهور بالانتباه، ارخاء حاد بالعضلات والمركز الرئوي، انخفاض ضغط الدم والادمان السلوكي والجسدي السريع.",
                contraindications = "الحساسية الخاصة، الصدمة الوعائية الرخوة، قصور وظائف التنفس أو الكبد الشديد، والحمل والإرضاع.",
                interactions = "يتسبب بالتوقف والوفاء المفاجئ للقلب والتنفس بالتزامن العضوي مع الكحولات ومثبطات المورفين العصبية الشرسة.",
                administration = "حقن وريدي بطيء (IV over 3 minutes) أو حقن عضلي عميق ومؤمن (IM) أو تحاميل.",
                precautions = "تجنب صبه أو خلطه بمحاقين محاليل السوائل الأخرى خشية ترسيب المركب الفعال الفاعل بمجرى المغذي الدوائي.",
                weightBased = "نعم",
                dosePerKg = "0.15",
                maxDailyDose = "40 ملجم يومياً كحد أقصى للمحافظ عصبياً",
                ageDependent = "نعم",
                ageFormula = "جرعة التشنجات الميدانية تحسب عند 0.15 ملجم/كجم للجرعة المفردة، مع إمكانية التكرار بحذر بعد ربع ساعة."
            ),
            // 9. Morphine Sulfate
            Drug(
                scientificName = "Morphine Sulfate",
                category = "💊 مسكنات الآلام والتخدير العسكري",
                definition = "مسكن مخدر أفيوني قوي فعال لعلاج الآلام العنيفة والرواسب الجراحية والرضحية الصعبة.",
                mechanism = "الارتباط الكامل الواهب بمستقبلات ميو الأفيونية (Mu-Opioid receptors) بالدماغ لوقف إشارات وجع الوعي الشوكي تماماً.",
                uses = "التحكم بالآلام المبرحة الحادة لدى مصابي الرصاص بالميادين والحروق البالغة الوعرة والكسور الكبرى واحتشاء القلب المتعب.",
                dosageGeneral = "- وريدياً بطيئاً: 0.1 ملجم/كجم (ما يقدر بـ 2 إلى 5 ملجم وتكرر للبالغ حسب الحاجة).\n- تسريب ممتد: 5 إلى 10 ملجم بالساعة.",
                dosageForms = "أمبولات عيار 5mg/ml و10mg/1ml وقوارير ممتدة الأجل وأقراص فموية بمقادير مختلفة.",
                sideEffects = "كبس للتنفس الشرياني، غثيان وقيء مفرط، تخميد الوعي، امساك شديد، انقباض حدقة العين (Miosis).",
                contraindications = "اضطرابات التنفس، انسداد وفتحات الرأس الرضحية، ضغط القحف العالي، الحساسية والـقصور الوعائي الحاد.",
                interactions = "مشاركتها مع الباربيتورات والمهدئات الأخرى تؤدي لغيبوبة صامتة وتوقف رئوي وعائي مباشر وصارم.",
                administration = "حقن وريدي بطيء (IV)، حقن عضلي (IM)، أو تحت الجلد (SC).",
                precautions = "يجب حفظ ترياق النالوكسون (Naloxone) جاهزاً دائماً لفك وقبض جرعات المورفين المفرطة السامة.",
                weightBased = "نعم",
                dosePerKg = "0.1",
                maxDailyDose = "حسب الحاجة وقوة الآلام مع مراعاة حماية مجرى الهواء",
                ageDependent = "لا",
                ageFormula = "يحسب بدقة للـأطفال الصغار لسرعة تثبيط التنفس لديهم (الجرعة النموذجية 0.1 مجم لكل كجم)."
            ),
            // 10. Calcium Gluconate
            Drug(
                scientificName = "Calcium Gluconate",
                category = "🚨 أدوية الطوارئ والإنعاش القلبي",
                definition = "مستحضر معدني أساسي يوصف لمعادلة وتثبيت عضلات وجدار القلب تحت حمى اضطرابات البوتاسيوم.",
                mechanism = "تعويض شارد الكالسيوم المنشط لجهد الفعل لعضلات الجسم والقلب ومقاومة الآثار الوهطية لزيادة البوتاسيوم بكفاءة.",
                uses = "القصور القلبي المتصل بارتفاع البوتاسيوم، تسمم مثبطات القنوات الكلسية، وعلاجات جفاف ونقصان الكالسيوم الحاد.",
                dosageGeneral = "- وريدياً عاجلاً للبالغين: 500 إلى 2000 ملجم ببطء شديد عبر الوريد (يعادل 10-20 مل من محلول 10%).",
                dosageForms = "أمبولات بتركيز 10% (100 ملجم/مل) بحجم 10 مل للامبول الواحد.",
                sideEffects = "انخفاض مفاجئ الشد لضغط الدم الوعائي، تباطؤ بضربات القلب، قياء شديد، إحساس بحرارة طاغية سريعة بالجسم.",
                contraindications = "الارتجاف البطيني غير المنظم، فرط الكالسيوم والـسّمية المرافقة بدواء الديجوكسين الدوائي الشرس.",
                interactions = "برسب المضاد الحيوي السفترياكسون داخل شرايين والوريد للرضع لذا يحظر دمجهما كلياً.",
                administration = "حقن وريدي بطيء جداً وحصري (IV over 10 minutes) عبر منفذ آمن.",
                precautions = "يراعى الحظر الشديد والكامل لتفادي تسربه خارج الجلد تحت النسيج لئلا يسبب تهتكاً ونخراً بالغاً بالطرف المعالج.",
                weightBased = "نعم",
                dosePerKg = "30.0",
                maxDailyDose = "2000 ملجم للحقنة الواحدة",
                ageDependent = "لا",
                ageFormula = "الجرعة الميدانية للأطفال: 30 ملجم لكل كجم من محلول عيار 10% ببطء بالغ لمقاومة الـصدمة وعوز المعدن الأساسي."
            ),
            // 11. Metoclopramide (Plasil)
            Drug(
                scientificName = "Metoclopramide (Plasil)",
                category = "🩺 أدوية الجهاز الهضمي والقيء",
                definition = "مضاد للتقيؤ والغثيان ومنظم حركة القناة الهضمية المعوية.",
                mechanism = "إعاقة وحجب جدار مستقبلات الدوبامين المركزية بالمخ والخلية الهضمية لتقصير فترات افراغ المعدة البطيئة.",
                uses = "علاج والوقاية من الغثيان والقياء المرافق للإصابات والجرحى ومرضى الجراحات والصدمات الميدانية الكبرى.",
                dosageGeneral = "10 ملجم للبالغين وريدياً أو عضلياً أو فموياً كل 8 ساعات بحد أقصى 3 مرات.",
                dosageForms = "أمبولات 10mg/2ml، وأقراص فموية عيار 10 ملجم.",
                sideEffects = "خمول طفيف ونعاس، اضطرابات الرعاش العضلية الميدانية، تشنج الرقبة، زيادة هرمون الحليب البرولاكتين.",
                contraindications = "انسداد ونزيف وثقاب القناة والمعدة الصامتة أو النشطة، صرع الهياج العقلي، التحسس وموانع الدوبامين.",
                interactions = "يقلل من الفاعلية العلاجية لأدوية الباركنسون ويزرع مستويات الامتصاص للباراسيتامول المعوي.",
                administration = "حقن وريدي (IV)، حقن عضلي (IM)، أو تناول فموي (PO).",
                precautions = "يراعى عدم إعطائه بجرعات سريعة وريدياً لئلا يورث اضطراب تململ وقلق حاد وعابر لدى المتلقي العسكري.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "30 ملجم يومياً",
                ageDependent = "لا",
                ageFormula = "يستخدم بحذر شديد مع تجنب تزويده للأطفال الصغار لعدم توليد تشنجات عضلية حركية خارج هرمية مفرطة لديهم."
            ),
            // 12. Tetanus Antitoxin
            Drug(
                scientificName = "Tetanus Antitoxin (ATS)",
                category = "⚔️ الترياقات والمصل الوقائي",
                definition = "ترياق ومصل مناعي واقي معدل لسموم بكتيريا الكزاز (التيتانوس) الناتجة عن الجروح الحادة الميدانية الملوثة بالتراب.",
                mechanism = "معادلة السم الفسيولوجي المترسب بمجرى الدم قبل ارتباطه بالنسيج الجلدي العصبي وحجز تقدم الكزاز بالجسم.",
                uses = "تأمين وحماية المصابين بجروح عميقة ملوثة برصاص وشظايا حربية وتراب، ومنع هجوم بكتيريا الكلاستريديوم قاتلة العضلات.",
                dosageGeneral = "- الجرعة الوقائية العاجلة: 1500 إلى 3000 وحدة دولية (IU) حقناً بالجلد/العضل بعد اختبار فرط الحساسية السريع.",
                dosageForms = "أمبولات جاهزة للحقن عيار: 1500 وحدة دولية (IU) / 1 مل.",
                sideEffects = "صدمة تحسس عاجلة حادة، حمى مرافقة، طفح جلدي ممتد وآلام مفاصل حادة متأخرة بعد أسبوع (داء المصل).",
                contraindications = "فرط التحسس المثبت السابق للمستحضر ومكونات البروتينات الحيوانية للخيول المأخوذ منها المصل.",
                interactions = "لا توجد تفاعلات سريرية تذكر غير منعه للآثار المناعية لبعض اللقاحات الفيروسية النشطة لمدة محدودة.",
                administration = "حقن تحت الجلد بطبقة واقية (SC) أو تزريق عضلي عميق (IM).",
                precautions = "يمنع كلياً حقن الجرعة دون إجراء فحص اختبار جلد الحساسية المسبق بمسحة قطن بسيطة، وحفز توفر دواء الأدرينالين لعلاج أي صدمة وعائية خاملة.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "3000 وحدة وقائياً (وتتضاعف علاجياً في المستشفيات لأرقام أكبر)",
                ageDependent = "لا",
                ageFormula = "الجرعة ثابتة للوقاية الميدانية من تيتانوس التراب بـ 1500 وحدة دولية للكبار والـصغار من المقاتلين والجرحى."
            ),
            // 13. Ampicillin
            Drug(
                scientificName = "Ampicillin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد حيوي واسع المدى ينتمي لعائلة البنسيلينات ذات الحلقة اللحمية الفعالة للبكتريا الموجبة والسالبة.",
                mechanism = "وقف وتثبيط تخليق جدار الخلية البكتيرية مسبباً تكسيرها وموتها الشامل تحت تأثير ضغط المياه الأسموزي الداخلي.",
                uses = "علاج التهابات الجهاز التنفسي السفلي من رئة، الشعب، مجاري البول، وجروح الحرائق والشظايا المعرضة للصديد.",
                dosageGeneral = "- الكبار: 500 ملجم إلى 1 جم فموياً أو حقناً بالوريد/العضل كل 6 ساعات بانتظام.\n- الصغار: 50-100 ملجم/كجم مقسمة.",
                dosageForms = "كبسولات فموية عيار: 250 ملجم و500 ملجم، فيال بودر حقن عينة: 500 ملجم و1000 ملجم (1 جم).",
                sideEffects = "طفح جلدي تحسسي حاد، إسهال معوي حاد، تشنجات عصبية عند تعجيل جرعات ضخمة وريدية لدى مرضى الكلى.",
                contraindications = "التاريخ المثبت لفرط التحسس من سائر عائلة البنسيلينات كالبنسيلين G، والتهاب كثرة وحيدات الخلايا العدواني.",
                interactions = "يقلل من الكفاءة الطبية لموانع الحمل التكتيكية فموياً، ويفرط بزيادة طفح دواء الألوبرينول الحامضي للكلى.",
                administration = "حقن وريدي مباشر (IV)، تزريق عضلي (IM)، أو تعاطي فموي (PO).",
                precautions = "يراعى خفض وتعديل سقف ومقادير الدواء لمرضى الفشل والقصور الكلوي المحتفظ، ومراقبة وظائف الدم الحيوية.",
                weightBased = "نعم",
                dosePerKg = "50.0",
                maxDailyDose = "4000 ملجم (4 جم) يومياً",
                ageDependent = "نعم",
                ageFormula = "الأطفال الرضع: 50 إلى 100 ملجم/كجم/يوم مقسمة بالتساوي على 4 فترات زمنية متساوية."
            ),
            // 14. Augmentin (Amoxicillin + Clavulanate)
            Drug(
                scientificName = "Augmentin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مستحضر دوائي مضاد للبكتريا مركب مدموج يمنع هدم ومقاومة البكتريا للأموكسيسيلين المعزز.",
                mechanism = "حمض الكلافولانيك يحظر عمل إنزيم البيتا-لاكتاماز البكتيري مما يتيح للأموكسيسيلين هدم وبناء جدار البكتيريا بأريحية تامة.",
                uses = "علاج التهاب الجيوب الرئوية، الأذن الوسطى المتقرحة، ذات الرئة الشديدة، عضلات العظم والأسنان والرضوض العميقة الشاطرة.",
                dosageGeneral = "- البالغين: 625 ملجم إلى 1000 ملجم (1 جم) مرتين أو 3 مرات يومياً فموياً لـمدة 7 إلى 10 أيام.\n- فيال وريدياً: 1.2 جم كل 8 ساعات للحالات الكبرى.",
                dosageForms = "أقراص فموية: 375، 625، 1000 ملجم. شراب معلق للأطفال وعاء 156، 312، 457 ملجم في 5 مل. فيال للمغذي 1.2 جم.",
                sideEffects = "اضطرابات هضمية قاسية تشمل إسهالاً وغثيان، يرقان جلدي عابر والتهاب الكبد الحاد وتراكم الفطريات المعوية.",
                contraindications = "التاريخ المثبت لفرط التحسس من دواء أو عائلة البنسيلينات واليرقان الكبدي الناجم من استعمال المستحضر المسبق.",
                interactions = "يزود من تركيز وخطورة وميض الوارفارين المميع للشرايين ويزداد مفعوله بالخلايا مع تفوق البروبينسيد بالكلى.",
                administration = "تناول فموي مع أول لقمة طعام (PO) للسهولة ومنع وجع وحرقة البطن، أو حقن وريدي بطيء بالمطهر الداخلي المعقم.",
                precautions = "تجنب الاستعمال الطويل المفرط لئلا تنشأ فطريات وراقب واضبط الجرعة بدقة للمسنين العسكريين وأصحاب علل الكلى الكبيد.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "3000 ملجم (3 جم) فموياً للكبار",
                ageDependent = "نعم",
                ageFormula = "الأطفال: يحسب على أساس مركب الأموكسيسيلين بمعدل 25 إلى 45 ملجم لكل كجم يومياً مقسمة سريرياً."
            ),
            // 15. Ceftriaxone
            Drug(
                scientificName = "Ceftriaxone",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد حيوي واسع النطاق من عائلة السيفالوسبورينات الجيل الثالث القاتل للعدوى الحادة بجرعة ممتدة ومريحة.",
                mechanism = "تثبيط ارتباط جدار البكتيريا من خلايا الموجب والسالب وقتلها تحت ظل ثباتها التام أمام إنزيمات التدمير البكتيري.",
                uses = "التهابات السحايا الدماغية الصعبة، تسمم الدم البكتيري بالجروح، التهاب المسالك، وذات الرئة بالميادين والتخييم العسكري.",
                dosageGeneral = "- البالغين: 1 إلى 2 جرام فموياً/حقناً مرة أو مرتين باليوم (تصل إلى 4 جرام بالعدوى الشديدة).\n- الأطفال: 50-80 ملجم/كجم حقناً.",
                dosageForms = "فيالات جافة للحل والحقن بـمذيب خاص عيار: 250 ملجم، 500 ملجم، 1 جم (1000 ملجم) و 2 جم.",
                sideEffects = "ألم حارق موضعي بمحل الحقن والمغذي، اضطراب إنزيمات كرت الكبد، نقص بسيط بكرات الدم البيضاء المؤقت، تشكل حصوات كلسية بالمرارة.",
                contraindications = "التحسس المفرط لعائلة السيفالوسبورينات والبنسيلينات، الأطفال الرضع المصابين باليرقان (الصفراء) لـتدميره ارتباط البيليروبين بالألبومن.",
                interactions = "يرسب ويتلف محاليل الكالسيوم بالتسريب الوريدي للرضع، ويزيد فرصة نزف الأوعية مع أدوية منع التخثر السليمة.",
                administration = "حقن وريدي متقارب بطيء (IV over 5 minutes) أو حقن عضلي عميق يضاف له مخفف ليدوكائين مخدر لمنع الألم الموضعي (IM).",
                precautions = "تحذير فرط التحسس والوقوف عند وجود أي تاريخ تحسسي مسبق، ولا يتوجب خفض الجرعة الشديد بوجود مرض كلي معافى الكبد.",
                weightBased = "نعم",
                dosePerKg = "50.0",
                maxDailyDose = "4000 ملجم (4 جم) بالعدوى القاسية كالسحايا والدماغ المصدوم",
                ageDependent = "نعم",
                ageFormula = "الأطفال: 50 إلى 80 ملجم/كجم من وزن البدن يومياً كجرعة وحيدة أو مقسمة بالتساوي على فترتين."
            ),
            // 16. Gentamicin
            Drug(
                scientificName = "Gentamicin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد بكتيري قوي هادم للخلايا ينتمي لعائلة الأمينوجليكوسيدات لمهاجمة البكتريا سالبة الغرام الشرسة.",
                mechanism = "الارتباط غير العكوس بوحدة ريبوسوم الخلايا 30S لمنع وافساد قراءة وتصنيع البروتين الأساسي لخلية العدوى البكتيرية.",
                uses = "العدوى الالتهابية المعقدة للمسالك، بطانة القلب، ذات الرئة الشديدة وإصابات البطن بالحروب والجروح المفتوحة صديدياً.",
                dosageGeneral = "- البالغين والأطفال: 3 إلى 5 ملجم/كجم من الوزن يومياً (تقسم على 3 فترات كل 8 ساعات أو جرعة واحدة مجمعة).",
                dosageForms = "أمبولات سائلة جاهزة عيار: 20mg/2ml، 40mg/2ml، 80mg/2ml ومرهم جلدي عيني بتركيز 0.3%.",
                sideEffects = "تسمم وفشل الكلى المتردد (Nephrotoxicity)، فقدان وتلف الأعصاب السمعية والتوازن بالأذن الداخلية المسمم (Ototoxicity)، ترهل عضلات حركي.",
                contraindications = "التحسس لمركبات الأمينوجليكوسيدات الشريكة، مرضى الوهن العضلي الوبيل لتفادي شلل حركة التنفس والحمل الكاذب.",
                interactions = "تتضاعف سمية الأذن والكلى والدم عند استخدامه بالتوازي مع مدر لاسيكس (Furosemide) والأسبرين والـمسكن المخدر ذو الحلقة المائية.",
                administration = "حقن وريدي متقطع (IV infusion over 30 minutes) أو تزريق عضلي عميق (IM) أو قطرات.",
                precautions = "يمنع استعماله لأيام طويلة تفوق 7-10 أيام إلا لمبررات، ويراعى تتبع ومراقبة وظائف البول وحجم الترشيح الكلوي وتصفية البدن.",
                weightBased = "نعم",
                dosePerKg = "4.0",
                maxDailyDose = "320 ملجم باليوم (لتجنب تراكم الدواء وسّمية الأذن الداخلية)",
                ageDependent = "لا",
                ageFormula = "الجرعة تحسب بجرأة ودقة بـ 3-5 ملجم/كجم لجميع الأعمار السنية بما يوافق التصفية الكلوية الفعالة الحقيقية للجريح."
            ),
            // 17. Tetracycline
            Drug(
                scientificName = "Tetracycline",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد حيوي كابح وموقف لنمو البكتيريا واسع الطيف الميداني التاريخي.",
                mechanism = "الارتباط بوحدة ريبوسوم البكتيريا 30S بكفاءة لمنع دخول حمض ريبونيك النقال tRNA وصناع البروتينات الحيوية.",
                uses = "علاج نوبات جروح التهابات الجلد والمقرات الرئوية، داء الأميبيا الخلوية بالكبد، وجراثيم الكوليرا المسببة لإسهال الجفاف الشديد.",
                dosageGeneral = "250 ملجم إلى 500 ملجم للبالغين فموياً كل 6 ساعات بانتظام على معدة فارغة بمياه كافية لتفادي قرحة الحنجرة.",
                dosageForms = "كبسولات فموية عيار: 250 ملجم و500 ملجم، مرهم للعين 1% لعلاج الرمد الميداني البكتيري.",
                sideEffects = "اضطرابات هضمية قوية، تلون وتآكل طبقة الأسنان والـعظام للأطفال بصبغ داكن لا يزول، وتحسس جلدي شديد للضوء الطبيعي الشمسي.",
                contraindications = "استخدامه للحوامل والمرضعات والرضع والأطفال بـعمر يقل عن 8 سنوات لمنع تلف وصبغ العظام الأسنان الشريكة.",
                interactions = "يتراجع كفاءة وامتصاص الدواء المعوي كلياً عند تناوله برفقة مشتقات الألبان، الكالسيوم، الحديد ومضادات الحموضة.",
                administration = "تناول فموي مع كوب ماء وفير قبل الطعام بساعة أو بعده بساعتين (PO).",
                precautions = "يراعى الحذر التام وتنبيه المصاب لتفادي السير تحت أشعة الشمس لشدة حساسية الجسد الجلدية المعالجة بالدواء للضوء.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "2000 ملجم (2g) فموياً للكبار بظروف الميدان",
                ageDependent = "نعم",
                ageFormula = "ممنوع كلياً وحظراً لجميع الأطفال تحت سن 8 سنوات لحفظ سلامة بنية العظام وألوان أسنانهم من التشوهات الدائمة."
            ),
            // 18. Erythromycin
            Drug(
                scientificName = "Erythromycin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد بكتيري واسع ينتمي لعائلة الماكروليدات كعلاج فاعل بديل لمن يعانون حساسية للبنسيلين المعهود.",
                mechanism = "الارتباط بمكون الوحدة الريبوسومية البكتيرية 50S المعطل لمجرى استطالة بروتينات خلايا المرض الميكروبية.",
                uses = "علاج التهابات القناة التنفسية العلوية والسفلية، داء السعال الديكي عند الأطفال الرضع، والدفتيريا، والوقاية من حمى الروماتيزم.",
                dosageGeneral = "- البالغين فموياً: 250 إلى 500 ملجم كل 6 ساعات (أو 1 جم كل 12 ساعة حسب شدة الالتهابات الميدانية).\n- الأطفال: 30-50 ملجم/كجم/يوم.",
                dosageForms = "أقراص فموية: 250 ملجم، 500 ملجم، شراب معلق مغلف عيار 125 ملجم و250 ملجم في 5 مل من العبوة السائلة.",
                sideEffects = "تقلصات وأوجاع عنيفة بالمعدة والبطن مسبباً قياء، زيادة سرعة خروج محتوى المعدة المعوية، تهيج نبضات القلب الطويل بالـ ECG.",
                contraindications = "الحساسية الخاصة لمجوعة الماكروليد، وأصحاب المرض الكبدي، والـعلاجات المتزامنة مع الترياق المعجل للرعاش السام.",
                interactions = "يثبط نشاط إنزيمات كرت الكبد CYP450 بـقوة مما يرفع مستويات وسموم التوفيلين، الديجوكسين والوارفارين بالدم.",
                administration = "تناول فموي ميسر بمياه وفيرة (PO) ويفضل تعاطيه بعيداً عن الطعام لضمان سرعة امتصاص الدليل.",
                precautions = "لمن يصابون باضطرابات الكبد لتفادي انسداد القنوات الصفراوية، وممنوع تعاطيه لمدد علاج تتخطى الأسبوعين المتواصلين.",
                weightBased = "نعم",
                dosePerKg = "40.0",
                maxDailyDose = "2000 ملجم (2 جرام) يومياً بالتساوي الحركي للكبار",
                ageDependent = "نعم",
                ageFormula = "الأطفال الصغار: 30 إلى 50 ملجم/كجم لقوام الوزن اليومي الكلي، وتقسم بالتساوي لتؤكل كل 6 ساعات."
            ),
            // 19. Clarithromycin
            Drug(
                scientificName = "Clarithromycin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد حيوي غامر شبه مصنع ماكروليدي قوي ذو تغلغل ممتاز ومستقر ومعدل للخلايا والأنسجة العميقة بالبدن.",
                mechanism = "إغلاق ميكانيكي حركي لوحدة 50S الريبوسومية البكتيرية المعطل لأطقم بناء البروتين الميكروبي المهاجم للصحة.",
                uses = "علاج الالتهاب الرئوي اللانموذجي الميداني، غزو جرثومة المعدة الحلزونية (قرحة البيلوري)، واللوزتين، وجروح الجلد الرغوية.",
                dosageGeneral = "250 إلى 500 ملجم للبالغين بجرعة منتظمة مريحة تعاد كل 12 ساعة لـمدة 7 إلى 14 يوماً مع الطعام.",
                dosageForms = "أقراص فموية مغلفة عيار: 250 ملجم و 500 ملجم، شراب سائل معلق معزز للأطفال بطعم الفاكهة.",
                sideEffects = "صداع ودوخة خفيفة، إسهال معوي وتغير حاد ومكثف بحاسة الذوق والطعم المعدني بالفم، قساوة خفيفة بالبطن.",
                contraindications = "الحساسية لماكروليدات، اضطرابات نبضات القلب المتأثر بالنبض الطويل QT، وقصور الكبد الكثيف المترافق بخلل كلو المترامي.",
                interactions = "يتسبب بسمية الديجوكسين والمهدئات الميدانية الميدازولام لكونه يحجز نشاط حرق الكبد المعوي.",
                administration = "فموي لسهولة التعاطي بالقاعدة العسكرية (PO) مع كوب ماء طاهر.",
                precautions = "يراعى خفض وتقليص سقف الأقراص لمرضى القصور الكلوية الحادة وتصفية الكرياتينين بحد يقل عن 30 مل/دقيقة.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "1000 ملجم (1 جم) يومياً كحد أقصى بالميادين السريرية المانحة",
                ageDependent = "لا",
                ageFormula = "للبالغين: 250 إلى 500 مجم كل 12 ساعة لمدة أسبوعين للقضاء على جرثومة المعدة المعوية بشكل فاعل حاسم."
            ),
            // 20. Ciprofloxacin
            Drug(
                scientificName = "Ciprofloxacin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد بكتيري فلوروكينولوني حاد وواسع الطيف التكاثري ذو استهداف نسيجي سريع.",
                mechanism = "تثبيط عمل إنزيم الحمض النووي التكاثري (DNA Gyrase) وهدم جين البكتيريا مما يمنع انقسامها نهائياً.",
                uses = "علاج الإسهال البكتيري المعوي الشديد (التيفوئيد وزحار الميادين العكر)، عدوى الرئة الشاطرة البالغة والجهاز البول والمسالك الحادة.",
                dosageGeneral = "- البالغ فموياً: 500 ملجم إلى 750 ملجم مرتين يومياً (كل 12 ساعة) لـمدة 5-10 أيام.\n- وريدياً للخط الحاد: 200 إلى 400 ملجم تسريباً.",
                dosageForms = "أقراص فموية مغلفة: 250، 500، 750 ملجم، محلول تسريب وريدي معقم بحجم 100 مل عيار 200 ملجم.",
                sideEffects = "ألم وتقلص بالبطن مع إسهال، غثيان طفيف، اضطراب عصبي بالأرق، والأجسام الغضروفية كالتهاب وتمزق أوتار العضلات (Tendonitis).",
                contraindications = "الحساسية لفلوروكينولونات، الأطفال الـمقاتلين تحت سن 18 عاماً لخطورة تلف الغضاريف والمفاصل والحمل والرضاعة.",
                interactions = "الحديد والزنك ومضادات الحموضة بالـألمونيوم تلغي امتصاص العلاج تماماً عند المزج السريع المعدي.",
                administration = "تناول فموي (PO) على معدة فارعة بمياه وافرة وجنب الحليب، أو تسريب وريدي متقطع معقم ومبطئ (IV infusion).",
                precautions = "يتوجب حث المصاب للشرب والتبول بالماء الغزير السليم لمنع ترسب وتثاقل بلورات الكريستالات بـالمجاري الكلوية البولية العسرة.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "1500 ملجم يومياً فموياً للكبار والميدانيين العسكريين",
                ageDependent = "نعم",
                ageFormula = "ممنوع وحظراً للأطفال واليافعين دون سن 18 عاماً خشية تلف الغضاريف النامية بالمفاصل (ما عدا حالات الجمرة الخبيثة الصعبة)."
            ),
            // 21. Levofloxacin
            Drug(
                scientificName = "Levofloxacin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد حيوي فلوروكينولوني مكثف أحادي الجرعة اليومية ذو نفاذية هائلة للمسالك الرؤوية والصدرية.",
                mechanism = "تعطيل وتثبيط إنزيمات الحمض البكتيري النووي الوراثي مما يقصي نمو البقع الجرثومية بهندسة حيوية صائبة.",
                uses = "ذات الرئة الميدانية والالتهاب الرئوي الفاعل، التهاب الشعب والجيوب الوعرة، عدوى الجلد وعضلات البدن للجرحى.",
                dosageGeneral = "250 إلى 500 ملجم للبالغين وريدياً أو فموياً مرة واحدة باليوم (كل 24 ساعة) لـمدة 5 إلى 14 يوماً حيوياً.",
                dosageForms = "أقراص فموية: 250 ملجم و500 ملجم و750 ملجم، ومحلول تسريب وريدي معقم جاهز للقطارة المغذية.",
                sideEffects = "صداع ودوخة خفيفة، صعوبات بالاستغراق بالنوم، التهابات بالأوتار العضلية وتمزق كاحل القدم (Achilles tendon rupture)، تحسس ضوئي.",
                contraindications = "فرط الحساسية، التاريخ المرضي للصرع ونوبات الكهرباء العصبية، والأطفال اليافعين أقل من 18 عاماً.",
                interactions = "تقل كفاءته بشدة بفعل الكالسيوم ومعدن المغنيسيوم، ويسرع ويبعث احتمالات هبوط السكر المفرط لمرضى السكري المتقاعد.",
                administration = "حقن وتسريب وريدي تدريجي هادئ (IV) أو تناول فموي منتظم (PO).",
                precautions = "الحظر الكامل والتقصي من حركة الكاحل، ويمنع الإعطاء والتناول لمستحضرات الألمونيوم قبل أو بعده بفاصل 4 ساعات.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "750 ملجم مرة باليوم للبالغين بالميدان",
                ageDependent = "نعم",
                ageFormula = "يمنع كلياً للأعمار دون 18 عاماً حماية لبنيتهم الغضروفية ونمو مفاصلهم الركيكة التكتيكية."
            ),
            // 22. Moxifloxacin
            Drug(
                scientificName = "Moxifloxacin",
                category = "🧫 المضادات الحيوية والمضادات البكتيرية",
                definition = "مضاد حيوي فلوروكينولوني فائق الطيف والتأثير لغزو البكتيريا اللاهوائية والأمراض الصدرية المتصلبة بالمخيم العسكري.",
                mechanism = "حظر وتطويق التماسك الجيني الجرثومي عبر وقف إنزيمات التضاعف مما يؤدي للتلاشي الخلوي الجداري والوراثي البكتيري.",
                uses = "التهابات التجويف الجيبي الحاد، التهاب القصبات، رئة الميدان، جراحات البطن والجروح المفتوحة بالمخيم الملوثة بالثرى.",
                dosageGeneral = "400 ملجم فموياً أو وريدياً بجرعة وحيدة ممتازة كل 24 ساعة (مرة باليوم) لـمدة 5-14 يوماً.",
                dosageForms = "أقراص مريحة عيار 400 ملجم، عبوات تسريب وريدي 400mg/250ml جاهزة، وقطرات عينية للرمد 0.5%.",
                sideEffects = "اضطرابات معوية وغثيان خفيف، قلق وتوتر نفسي عابر، دوار، وتطاول نسبي لحركة موجة القلب الـكهربائية QT.",
                contraindications = "التحسس المتقدم، أمراض تطاول النبض الـكهربي القلبي، أمراض الكبد اليرقانية المستعصية، ومن هم دون سن 18.",
                interactions = "يزود من تظليلات تميع الوارفارين ومستوى النزيف، ويتعارض مع الأدوية المضادة لاضطرابات النبض الصعبة بالقلب.",
                administration = "حقن وريدي بتسريب بطيء (IV over 60 minutes) أو تناول فموي في أي وقت (PO).",
                precautions = "يراعى عدم شرب مشتقات الحليب أو المعادن والحموضة بفاصل يقل عن ساعتين من تعاطي الحبة فموياً.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "400 ملجم باليوم (جرعة محددة ثابتة)",
                ageDependent = "نعم",
                ageFormula = "ممنوع حظراً لمن يقل سنهم عن 18 عاماً لمنع أي تلف للغضاريف أو العظام الركيكة السطحية الدقيقة."
            ),
            // 23. Fluconazole
            Drug(
                scientificName = "Fluconazole",
                category = "🍄 مضادات الفطريات الوعرة بالميادين",
                definition = "مضاد فطريات جهازي تريازولي فاعل وسريع الخروج للسيطرة على الفطريات الغازية لممرات البدن الفسيولوجية.",
                mechanism = "تثبيط كامل لإنتاج ستيرولات الغشاء الخلوي للفطريات مما يجعل جدار الفطريات قلقاً ومفرغاً للمحتوى المائي.",
                uses = "علاج والوقاية من الفطريات الجسدية بالميدان، الفطريات المهبلية، والتهابات القلاع البيضاء بالفم والمريء.",
                dosageGeneral = "150 ملجم فموياً كجرعة وحيدة قياسية مريحة، أو 50-100 ملجم للعدوى الجلدية اليومية لـمدة أسبوعين.",
                dosageForms = "أقراص فموية سريعة التحلل عيار: 150 ملجم وكبسولات قوية وعصير معلق للأطفال الرضع.",
                sideEffects = "صداع ودوخة خفيفة جداً، طفح جلدي طفيف، قساوة أو حرقة خفيفة بالمعدة والبطن عند التموضع الصباحي.",
                contraindications = "التحسس الشديد من مجموعة عائلة الآزول، التزامن مع الأدوية المعجلة لتباطؤ جدار عضلات المخ العصبية.",
                interactions = "يزيد تراكيز الفينيتوين ويهدد بزيادة تميع وميض الديدان الشرايين بالتزامن المورفيني الدقيق.",
                administration = "تناول فموي ميسر بمياه وفيرة (PO) مع أو في غمرة الغذاء الميداني السليم.",
                precautions = "يراعى الحذر والدقة الفعالة لأصحاب العلل الكبدية والفشل الكلوي وتتبع وظائف الدم الشريكة.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "150 ملجم (بالجرعات المفردة الشائعة)",
                ageDependent = "لا",
                ageFormula = "للبالغين: جرعة 150 مجم كبسولة واحدة تكفي لإنهاء الفطريات الفموية والسطحية بيسر تام."
            ),
            // 24. Prednisolone
            Drug(
                scientificName = "Prednisolone",
                category = "🧪 الهرمونات والكورتيزونات",
                definition = "كورتيكوستيرويد قوي مضاد لالتهابات وحساسية الأنسجة العميقة وكابت للمناعة الطارقة.",
                mechanism = "هدم وتطويق اصطناع الكيماويات والوسائط الالتهابية كالهستامين والبروستاجلاندين بالخلايا وحجر تقدمها الدفوع.",
                uses = "التحسس الميداني الحاد، نوبات الربو الصدري الشديد المتفاقم، التهاب الروماتيزم، وعلاجات الطفح والـجذام الجلدي الحاد.",
                dosageGeneral = "5 ملجم إلى 60 ملجم يومياً فموياً للبالغين حسب استجابة وشدة ومقادير الألم والالتهاب العضوي.",
                dosageForms = "أقراص فموية بمقادير: 5 ملجم و20 ملجم، وحقن مدموعة ومحلول شراب للأطفال 5mg/5ml وعيني.",
                sideEffects = "زيادة واندفاع لشهية الطعام والوزن، احتباس السوائل الشحوم بالوجه والبدن، قرحة هضمية حية، تدهور السكري والضغط.",
                contraindications = "الجهد الجرثومي البكتيري غير المعقم، الفطريات الجهازية، الحساسية، وتطعييم اللقاحات الفيروسية الحية المتزامنة.",
                interactions = "يقل فعاليته الطبية مع دواء الصرع الفينيتوين، ويزيد من مخاطر قرحة المعدة والنزف بالتزامن مع الأسبرين والبدائل.",
                administration = "ناول فموي ويفضل مع وجبة الصباح الميسرة (PO) لتقليل الحموضة وتجنيب اضطراب المعدة السريرية.",
                precautions = "يمنع كلياً تجميد أو إيقاف تناول الدواء فجأة دون تدريج هادئ (Tapering) لتجنب هبوط غدة الكظر الدموي القاتل بالميدان.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "60 ملجم يومياً فموياً",
                ageDependent = "لا",
                ageFormula = "الجرعة تحضر وتدرج بدقة، لـتخفيف نوبات الصدر: 1-2 ملجم لكل كجم يومياً كمدد لـمدة 3-5 أيام هادئة."
            ),
            // 25. Fansidar
            Drug(
                scientificName = "Fansidar (Sulfadoxine + Pyrimethamine)",
                category = "🦟 مضادات الملاريا الطفيلية العسكرية",
                definition = "مستحضر وقائي علاجي مدموج فاعل للقضاء على طفيل الملاريا بالبدن والميادين الاستوائية الدافئة.",
                mechanism = "حظر وتجميد عملية بناء واستقلاب حمض الفوليك الأساسي لطفيل الملاريا المنجلية والـبلازموديوم بالخلايا.",
                uses = "الوقاية المسبقة وعلاجات طفيل الملاريا المنجلية المقاوم بظروف التخييم والتدريب العسكري بالمناطق الموبوءة.",
                dosageGeneral = "- العلاج السريع: 3 أقراص فموية دفعة واحدة للبالغين المقاتلين بالميدان.\n- الوقاية: قرص واحد أسبوعياً قبل الدخول لربوع السهول.",
                dosageForms = "أقراص فموية مدموجة تحتوي عيار: 500 ملجم سلفادوكسين بالإضافة لـ 25 ملجم بيريميثامين.",
                sideEffects = "صداع ودوخة خفيفة، فقر دم حاد بنقص المغذيات الدموية، طفح تحسسي وتهيج حاد بالبشرة المرتجفة.",
                contraindications = "فقر الدم الشديد بنقص صفائح الكبد، الحساسية لعائلة السلفوناميدات الكبريتية، والرضع الصغار تحت عمر شهرين.",
                interactions = "يزداد مخاطر التسمم والـمفعول الكبدي عند تناوله برفقة مركبات السلفا الأخرى والأدوية المضادة للفطريات.",
                administration = "ناول فموي لسهولة التعاطي العسكري (PO) بعد الوجبات بمياه غزيرة للتسريب البولي الطاهر.",
                precautions = "يراعى الحذر الشديد لأصحاب الفشل والوهن الكبدي والكلوي المترافق بمراقبة نسبة تراكيز الهيموجلوبين بالدم البشري.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "3 أقراص يومياً كجرعة علاجية وحيدة",
                ageDependent = "لا",
                ageFormula = "توزع الجرعة بحرص للمقاتلين، 3 أقراص حاسمة دفعة واحدة تنهي دورة حياة الطفيل بالدم بكفاءة ممتازة."
            ),
            // 26. Artesunate
            Drug(
                scientificName = "Artesunate",
                category = "🦟 مضادات الملاريا الطفيلية العسكرية",
                definition = "مضاد طفيليات قوي وحديث من مشتقات الأرتيميسينين لعلاج حالات الملاريا الحادة الوعرة المنقذة للحياة.",
                mechanism = "تحرير شقوق حرة سامة كيميائياً تهاجم وتدمر بروتينات غشاء طفيل الملاريا وتجعله ينكمش ويتحلل بسرعة فائقة.",
                uses = "عاجل علاج حالات الملاريا المنجلية الشديدة والوقاية الكثيفة الطارئة بالمناطق المدارية الاستوائية الرطبة الحارة.",
                dosageGeneral = "- الجرعة الأولى: 2.4 ملجم/كجم حقناً وريدياً بطيئاً.\n- ثم 2.4 ملجم/كجم بعد 12 ساعة.\n- ثم 2.4 ملجم/كجم يومياً لـمدة 3 أيام بالتجانس.",
                dosageForms = "أمبولات فيال زجاجي بودرة جافة للحل تحتوي عينات: 60 ملجم و120 ملجم مع مذيب فسيولوجي خاص.",
                sideEffects = "غثيان وقيء عابر مع إسهال بسيط، صداع وتراجع طفيف لقوة النبض والضغط، وهبوط طفيف بكرات الدم الحمراء.",
                contraindications = "فرط الحساسية المثبت للدواء أو لعائلة الأرتيميسينين المركزة، والقصور الكبدي المتطور القاسي الـتموضع.",
                interactions = "يتجنب تناوله بالتزامن المباشر مع أدوية الصرع الفينيتوين المسرعة لتلاشي أثره العلاجي الفعال بالبدن.",
                administration = "حقن وريدي بطيء عبر الكانيولا (IV) أو حقن عضلي عميق ومؤهل (IM).",
                precautions = "يجب دائماً وبترتيب عسكري جازم دمجه وتليينه بضخ مضاد ملاريا آخر مكمل بالفم لتجنب نشوء سلالات مقاومة بالـمحيط.",
                weightBased = "yes",
                dosePerKg = "2.4",
                maxDailyDose = "240 ملجم للحقنة الواحدة",
                ageDependent = "لا",
                ageFormula = "الجرعة تحسب بصرامة بـ 2.4 ملجم/كجم لحماية الأطفال وكبار المقاتلين من غيبوبة دماغي الملاريا الخطير."
            ),
            // 27. Famotidine
            Drug(
                scientificName = "Famotidine",
                category = "🩺 أدوية الجهاز الهضمي والقيء",
                definition = "مضاد حموضة مهبط لإفرازات جدار المعدة يعوق عمل وحركة مستقبلات الهستامين.",
                mechanism = "وقف وحظر عكوس لمستقبلات الهستامين H2 بالخلايا الجدارية المبطنة للمعدة لمنع وخفض افراز الأيون الحامضي الكثيف.",
                uses = "الوقاية والعلاج الحثيث لقرحات المعدة والجرثومة، حرقة وسخونة المريء والصدر بفعل العصارة العكر والمعدة المتعبة.",
                dosageGeneral = "- البالغ فموياً: 20 إلى 40 ملجم يومياً مرة قبل النوم لمقاومة الحموضة الليلية.\n- وريدياً للخط الحرج: 20 ملجم.",
                dosageForms = "أقراص فموية: 20 ملجم و40 ملجم، وحقن أمبول عينة لـوريد عيار 20 ملجم/2 مل.",
                sideEffects = "صداع عابر متقطع، إمساك أو إسهال طفيف، تشوش وهياج ذهني بسيط نادر لدى المرضى والمسنين المقاتلين.",
                contraindications = "الحساسية المفرطة للمستحضر وعائلة مضادات مستقبلات H2 المتزامنة بالمركبات الكيميائية الشريكة.",
                interactions = "يقصر ويقلل امتصاص الكيتوكونازول والأدوية الصديقة للحموضة لكونه يقوم بازاحة وسط الهضم الـحامض.",
                administration = "حقن وريدي بطيء (IV over 2 minutes) أو تناول فموي مريح (PO) قبل العشاء والنوم.",
                precautions = "يراعى تقليص وضبط وتخفيض المقادير اليومية لأصحاب كسل وفشل وظيفة الكلى التصفية بشكل سليم وممنهج.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "40 ملجم يومياً (فموياً)",
                ageDependent = "لا",
                ageFormula = "للبالغين: 40 مجم حبة واحدة عند المساء تكفي لقهر وتخفيض حموضة الجوف والارتجاع بالمعدة بكفاءة وصمود متميز."
            ),
            // 28. Omeprazole
            Drug(
                scientificName = "Omeprazole",
                category = "🩺 أدوية الجهاز الهضمي والقيء",
                definition = "مثبط مضخة البروتون الفعال الخافض لأسيد المعدة عيار عالي القوة للميادين العسكرية.",
                mechanism = "عزل عكوس تام لعمل مضخات الهيدروجين-بوتاسيوم (H+/K+ ATPase) بالخلايا الجدارية لوقف دفق حامض المعدة بالكامل.",
                uses = "علاجات قرحة المعدة والـجوف المعوي الحادة، ارتجاع وعصارة المريء الحارقة، وحفظ المعدة عند تعاطي مسكنات ثقيلة لأيام.",
                dosageGeneral = "20 ملجم إلى 40 ملجم فموياً مرة واحدة باليوم صباحاً قبل الفطور بنصف ساعة لـمدة 4-8 أسابيع.",
                dosageForms = "كبسولات فموية ذات جدار معوي عيار: 20 ملجم و40 ملجم، وفيال حقن وريدية 40 ملجم.",
                sideEffects = "صداع طفيف، مغص بسيط بالبطن مع إسهال خفيف، ونقص بامتصاص مستويات فيتامين B12 والمغنيسيوم عند طول الأمد.",
                contraindications = "فرط التحسس للأوميبرازول وعائلة مثبطات مضخة البروتون (PPIs) المختلفة.",
                interactions = "يجمد امتصاص أدوية الحديد، كادواء الأمبيسيلين البنسيلين، والكيتوكونازول المعوي لزوال حامض الذوبان السليم.",
                administration = "تناول فموي وتبلع الكبسولة صحيحة كاملة (PO) أو حقن وريدي بطيء.",
                precautions = "يراعى اتخاذ الدقة وضم الكالسيوم وحمايته عند المرض السنوي لمنع كسر العظام المتصل بطول مد استخدام المستحضر.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "80 ملجم باليوم (كحد أقصى ممتد بالشدة الهضمية الحادة)",
                ageDependent = "لا",
                ageFormula = "للبالغين بالميدان: كبسولة ممتدة عيار 20 مجم صباحاً قادرة على كبت حامض وجرح الجوف بنجاح طوال اليوم الشاق."
            ),
            // 29. Mebendazole
            Drug(
                scientificName = "Mebendazole",
                category = "🐛 مضادات الجرب والقمل والعناية بالجلد",
                definition = "طارد ومضاد ديدان واسع الطيف وفاعل لتطهير الأمعاء الملوثة بالمخيمات.",
                mechanism = "حظر بناء الأنابيب الميكروية الدقيقة لدى الديدان مسبباً حرمانها الكامل من امتصاص الجلوكوز وموتها صدمة وطرحاً البشري.",
                uses = "تطهير وعلاج الديدان الدبوسية الحادة، ديدان الإسكارس الكبيرة، والديدان المعوية الشريكة المنتشرة ببيئة المخيم العسكري ومياه البراري.",
                dosageGeneral = "- ديدان دبوسية: 100 ملجم للبالغين جرعة واحدة فموية (وتكرر بعد أسبوعين تلافياً لرجوع البيض النشط).\n- إسكارس: 100 ملجم مرتين يومياً لـمدة 3 أيام متتالية.",
                dosageForms = "أقراص فموية قابلة للمضغ عيار 100 ملجم، شراب سائل معلق عذري للأطفال.",
                sideEffects = "مغص طفيف بالبطن والجلد، غازات، تحسس جلدي بسيط، ونقص طفيف عابر بمستوى خلايا الدم البيضاء.",
                contraindications = "فرط التحسس المثبت السابق للمادة الفعالة، والـسيدات الحوامل في ثلث الحمل الأول لسلامة تكون الجنين.",
                interactions = "يتعجل ويسرع استقلابه وتفريغه الكبدي عند تزامنه مع تعاطي دواء الصرع الفينيتوين الحركي فيتضاءل الأثر الدوائي.",
                administration = "فموي (يمضغ القرص جيداً قبل البلع لتعجيل الانتشار المعوي السليم أو يبلع بـالمياه) (PO).",
                precautions = "يراعى الحذر والتوجيه بضرورة غسيل وغلي الملابس والأغطية لكافة أفراد التخييم وقاية من عدوى عودة بيوض الديدان الملتصقة.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "500 ملجم كجرعة مفردة للإسكارس والترشيح الدوائي الممتد",
                ageDependent = "لا",
                ageFormula = "آمن للأطفال بـعمر يفوق عامين، ويعطى ذات جرعة البالغين بـ 100 ملجم للمضغ لسهولة قضاءه على ديدان البطن المنتشر الـمحيطي."
            ),
            // 30. Salbutamol (Ventolin)
            Drug(
                scientificName = "Salbutamol (Ventolin)",
                category = "🫁 أدوية الجهاز التنفسي والربو",
                definition = "موسع قصبات وممر شعب هوائية سريع المفعول ومنقذ لنوبات ضيق مجاري الهواء الصدري العاجل.",
                mechanism = "تحفيز قوي انتقائي لمستقبلات بيتا-2 (Beta-2 receptors) بالقصبات الصدرية مسبباً ارتخاء العضلات واتساع المسار الهوائي.",
                uses = "علاج والوقاية من نوبات ضيق الربو الصدري، التشنج الشعبي الرئوي المانع للتنفس بفعل الغبار ودخان الميادين والتخييم المناخي.",
                dosageGeneral = "- البخاخ: بخة أو بختين للبالغين (100-200 ميكروجرام) عند ضيق التنفس.\n- رذاذ النيبولايزر للتنفس: 2.5 إلى 5 ملجم مكرر عند اللزوم بقناع الأكسجين.",
                dosageForms = "عبوة بخاخ بضغط معقم الغاز (100 ميكروجرام للمرة)، محلول أزرق لجهاز النيبولايزر 5mg/ml، شراب فموي، كبسولات.",
                sideEffects = "رعشة اليدين الخفيفة الصادق العابر، صداع طفيف، تسارع بالنبض وخفقان القلب، تناقص بسيط لـنسبة البوتاسيوم بالدم.",
                contraindications = "التحسس المفرط، وعلاجات وقف الولادة المبكرة الصعبة لـتسببه بهبوط النبض الوالدي الحركي.",
                interactions = "أدوية مغلقات بيتا كدواء البروبرانولول تقيد وتلغي الفاعلية الطبية للمستحضر الهوائي بالكامل.",
                administration = "استنشاق بالفم عبر البخاخ الضغطي (Inhalation) أو عبر جهاز البخار المعقم (Nebulizer).",
                precautions = "الحذر التام مع مرضى القلب، فرط إفراز الغدة الدرقية، والسكري، ومراقبة تمدد نبضات القلب بالـ ECG للمتلقي الحاد.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "8 بخات يومياً كجرعة قصوى تكتيكية للمقاتل بالميدان",
                ageDependent = "لا",
                ageFormula = "يركب ويقاس الاستخدام للأطفال بجرعة رذاذ منخفضة (2.5 ملجم نيبولايزر بالماء المالح سالين 0.9%) بأمان."
            ),
            // 31. Aminophylline
            Drug(
                scientificName = "Aminophylline",
                category = "🫁 أدوية الجهاز التنفسي والربو",
                definition = "موسع مسارات تنفسیة وريدي ممتد الفاعلية لنوبات الربو المستعصية بالمخيم العسكري.",
                mechanism = "تثبيط إنزيم الفوسفودايستيراز (PDE) ورفع تركيز cAMP المحفز لارتخاء خلايا جدار القصبات الرئوية وتمدد المجرى الهوائي.",
                uses = "السيطرة كطوق أخير لعلاج نوبات تشنج القصبي الرئوي المستعصي (Severe Asthma Status) لمن لا يستجيب للبخاخات العادية.",
                dosageGeneral = "- جرعة تحميل بالوريد: 5 ملجم/كجم تسريباً هادئاً للغاية على مدى نصف ساعة في سكر سالين.\n- صيانة وريدية: 0.5 ملجم/كجم/ساعة.",
                dosageForms = "أمبولات وريدية معقمة للحقن عينة: 250mg/10ml المجهزة للـتخفيف في محاليل التسريب.",
                sideEffects = "اضطرابات وضربات قلب متسارعة خطرة، غثيان وقيء حاد، صداع وعصبية، أرق واندفاع للتشنجات الصرعية السام البالغ.",
                contraindications = "التحسس الشديد من مركب الثيوفيلين، نوبات الصرع غير المسيطر عليها عصبياً، وقصور عضل الجدار القلبي الحاد المرضي.",
                interactions = "يتراجع كفائته بالأعصاب مع استخدام البنزوديازيبينات والمهدئات، وتزداد سميته مع المضاد الحيوي إريثروميسين بالكبد.",
                administration = "تسريب وحقن وريدي مخفف ببطء شديد عبر قطارة محاليل الجلوكوز (IV infusion only).",
                precautions = "يمنع كلياً منعاً باتاً حقنه بالوريد عيار سريع لئلا يسبب هبوط ضغط قاتل وتوقف قلب مفاجئ للجريح الميداني الصارم.",
                weightBased = "yes",
                dosePerKg = "5.0",
                maxDailyDose = "500 ملجم للحقنة التحميلية المفرقة",
                ageDependent = "لا",
                ageFormula = "الجرعة النموذجية للتحميل تحسب بدقة بـ 5 ملجم لكل كجم من وزن المريض وتخلط جيداً بمحلول التسريب."
            ),
            // 32. Furosemide (Lasix)
            Drug(
                scientificName = "Furosemide (Lasix)",
                category = "🧪 المدرات الطبية والأسموزية",
                definition = "مدر بول قوي عالي التموج وفاعل لخفض الضغط وانسداد السوائل بالأطراف والرئة الصدرية.",
                mechanism = "تثبيط كامل لإعادة امتصاص الصوديوم والكلوريد والبوتاسيوم في العروة الصاعدة العلوية الكلوية مسبباً افراغ غزير للمياه بالبول.",
                uses = "علاج الوذمة الرئوية الميدانية الخانقة الناتجة عن قصور القلب الحاد، استسقاء السوائل بالأقدام، وعلاج ضغط الدم المرتفع الحرج.",
                dosageGeneral = "- وريدياً أو عضلياً عاجلاً للبالغين: 20 إلى 40 ملجم ببطء شديد وتكرر الجرعات بمقتضى التجاوب الفسيولوجي بالبول.",
                dosageForms = "أمبولات وريدية عازلة 20mg/2ml، أقراص فموية: 40 ملجم معقمة ومحمية.",
                sideEffects = "جفاف حاد ونقص شديد لنسب البوتاسيوم والصوديوم بالدم، هبوط ضغط دم مفاجئ وهط وعائي، فقدان وهن السمع السام المؤقت بالأذن الداخلي.",
                contraindications = "انقطاع التبول العضوي الكامل الكبدي الكلوى المتقدم (Anuria)، قلة البلازما والجفاف الحاد، وحالة التحسس لمركبات السلفا.",
                interactions = "يزوي ويضاعف سمي ومخاطر الأذن الداخلية الكلوية مع المضاد الحيوي جنتاميسين الشريك بالميادين الطبية.",
                administration = "حقن وريدي بطيء (IV over 2 minutes)، حقن عضلي عميق ومضمون (IM)، أو تناول فموي منتظم بمياه كافية (PO).",
                precautions = "تتبع النبض ومراقبة مستويات ضغط الدم الشرياني باستمرار، ويحظر حقنه كلياً عند تسجيل ضغط انقباضي يتدنى عن 90 مم زئبق.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "80 ملجم يومياً للحالات الميدانية غير السريرية",
                ageDependent = "لا",
                ageFormula = "الأطفال: يحسب على أساس 1 ملجم لكل كجم حقناً بالوريد السليم لتعجيل صرف واحتباس ماء البدن المصاب بأمان."
            ),
            // 33. Mannitol 20%
            Drug(
                scientificName = "Mannitol 20%",
                category = "🧪 المدرات الطبية والأسموزية",
                definition = "مدر بول أسموزي قوي يسحب السوائل من الأماكن المغلقة كالرأس والجمجمة والعين لحماية الدماغ.",
                mechanism = "رفع وتثقيل الضغط الأسموزي بالبلازما داخل شعيرات الدم مما يسحب مياه الأنسجة المضغوطة بالجمجمة للخارج والتفريغ البولي.",
                uses = "خفض مستويات ضغط الدماغ داخل القحف بعد اصابات وارتجامات الحروب ورصاص الرؤوس الشديد، خفض ضغط العين، وحفظ كفاءة الكلى.",
                dosageGeneral = "- تسريب وريدي منقذ للكبار: 0.25 إلى 2 جرام لكل كجم من وزن الجسم يسرب عبر منقى زجاج متبوع بالراحة وعاء ساعة.",
                dosageForms = "قوارير زجاجية معقمة من محلول مانيتول بتركيز: 20% بـحجوم 250 مل و 500 مل للحقنة المباشرة.",
                sideEffects = "صداع عابر وجاف بالفم، خفقان وتسرع بالقلب، زيادة مفاجئة بحجم البلازما، والوذمة الرئوية السائلة العابرة للقلب المتعب.",
                contraindications = "الفشل الفسيولوجي المطبق والكامل للكليتين، النزيف الباطني المفتوح غير المؤمم داخل الجمجمة، والجفاف الحاد المعمر.",
                interactions = "لا توجد تفاعلات ميدانية تذكر، ويراعى لضمان الفاعلية تفريغ مجاري السوائل البولية بالكامل تتبع الفيسيولوجيه.",
                administration = "تسريب مستمر وريدي عبر المغذي المجهز مصفاة عزل الزبرجد تفادياً للبلورات المتراكمة (IV infusion only).",
                precautions = "احذر بشدة تبلور وركود المانيتول بالبرد الشديد للميادين، ويجب لزاماً تدفئة القارورة بمياه فاتر دافئة لذوبان البلورات تماماً قبل الحقن.",
                weightBased = "yes",
                dosePerKg = "1.0",
                maxDailyDose = "200 جرام يومياً (كحد أقصى مسموح للبدن البشري)",
                ageDependent = "لا",
                ageFormula = "الجرعة تحضر بدقة بمعدل 1 جرام لكل كجم للحقن التدريجي لحجز وتصغير تورم الرأس والدماغ المصدوم عسكرياً."
            ),
            // 34. Lidocaine HCL
            Drug(
                scientificName = "Lidocaine HCL",
                category = "💊 مسكنات الآلام والتخدير العسكري",
                definition = "مخدر موضعي ومكافح لاضطرابات وعضلات نظم القلب البطيني الميداني العاجل.",
                mechanism = "غلق قنوات الصوديوم السريعة بالألياف العصبية الحسية مما يعوق ويمنع تكون ونقل إحساس الوجع وسرعة التخدير.",
                uses = "التخدير الموضعي المريح قبل مخاط وجراح خياطة الجروح العميقة وشظايا الحرب بالميادين، وعلاج حالات اللانظمية القلبية.",
                dosageGeneral = "- تخدير موضعي: يوضع بطبقة حاقنة حول أطراف الجرح بما يوافق سقف 3-4 ملجم/كجم من الوزن بدون ادرينالين.\n- مع ادرينالين: حتى 7 ملجم/كجم.",
                dosageForms = "فيالات زجاجية مخدر موضعي بتركيز 1% و 2% بحجم 50 مل، وكريم موضعي بـتركيزات 4% و 5% للتسكين الخارجي.",
                sideEffects = "طنين بالأذن والتشوش السمعي، ارتعاشات عصبية خفيفة، تباطؤ وانخفاض الضغط الشرياني، حساسية موضعية عابرة بالجلد.",
                contraindications = "فرط الحساسية المثبت للتخدير الموضعي الأميدي، بطء نبضات وجدران القلب الصامدة الشديد المتكتل بالـ ECG.",
                interactions = "يرتق ويمهد تزايد مفعول مغلقات قنوات الكالسيوم ومعدلات نبض القلب عند التداخل الجهازي بالأوردة.",
                administration = "حقن تحت جلدي موضعي كاشف للجرح (Infiltration/SC) أو مسحة وطلاء موضعي خارجي (Topical).",
                precautions = "يراعى الحظر بعدم حقنه بالأوردة بتموج عالي السرعة لغير اللانظميات، والتأكد من عدم سحب الدم بالمحقنة قبل التزريق الجلدي.",
                weightBased = "yes",
                dosePerKg = "3.0",
                maxDailyDose = "200 ملجم للبالغين المقاتلين بالميدان الموحد",
                ageDependent = "لا",
                ageFormula = "الجرعة تحسب كحد أمان قصوى بـ 3 ملجم لكل كجم من وزن الشخص حتى لا يتجاوز مستوى أمان الدورة الدموية."
            ),
            // 35. Etamsylate (Dicynone)
            Drug(
                scientificName = "Etamsylate (Dicynone)",
                category = "🩺 أدوية الجهاز الهضمي والقيء",
                definition = "مستحضر مضاد نزيف فعال لربط وتماسك الشعيرات الدموية التالفة المفتوحة.",
                mechanism = "تعزيز وتنشيط تلاصق الصفائح الدموية في مكان قطع الأوعية وتقليص زمن النزيف دون إحداث جلطات عارضة بالشريان.",
                uses = "الوقاية والحد من نزيف الأوعية الشعرية الدموية المترسخ عن الجروح والعمليات الصغرى والنزف الفجائي الميداني بالجسم.",
                dosageGeneral = "- البالغ وريدياً أو عضلياً: 250 إلى 500 ملجم قبل العمليات أو عند الإصابة مكرر كل 4 إلى 6 ساعات.",
                dosageForms = "أمبولات حقن عيار: 250 ملجم/2 مل، وأقراص فموية تماسك عيار 250 ملجم و 500 ملجم مريحة الاستخدام.",
                sideEffects = "صداع ودوخة خفيفة جداً، ألم خفيف بجدار عينات وعضلات المعدة، وهبوط طفيف سريع وضئيل لضغط الدم العابر.",
                contraindications = "التحسس المفرط للمستحضر ومكوناته، والـمرضى المصابين بأعراض البورفيريا الكبدية الشديدة الناتجة عن الصبغ.",
                interactions = "لا توجد تفاعلات ملموسة دقيقة، ويفضل عدم خلطه ودمجه بنفس الحقن مع مركبات ومضادات حيوية أخرى متبعة.",
                administration = "حقن وريدي معقم وهادئ بسوائل التغذية (IV)، حقن عضلي عميق ومضمون (IM)، أو تناول فموي مستمر (PO).",
                precautions = "يراعى الحذر الشديد لدى مرضى سوابق التجلط الرئوي الفاعل أو قصور وضيق وظائف الكلى التامة بالبدن البشري.",
                weightBased = "لا",
                dosePerKg = "0.0",
                maxDailyDose = "1500 ملجم يومياً للحفظ",
                ageDependent = "لا",
                ageFormula = "للبالغين: أمبول واحد 250 ملجم وريدياً أو عضلياً مكرر كاف تماماً لوقف نزيف الشعيرات والشظايا بيسر مميز."
            )
        )
        
        for (drug in drugs) {
            val cv = ContentValues().apply {
                put(KEY_SCIENTIFIC_NAME, drug.scientificName)
                put(KEY_CATEGORY, drug.category)
                put(KEY_DEFINITION, drug.definition)
                put(KEY_MECHANISM, drug.mechanism)
                put(KEY_USES, drug.uses)
                put(KEY_DOSAGE_GENERAL, drug.dosageGeneral)
                put(KEY_DOSAGE_FORMS, drug.dosageForms)
                put(KEY_SIDE_EFFECTS, drug.sideEffects)
                put(KEY_CONTRAINDICATIONS, drug.contraindications)
                put(KEY_INTERACTIONS, drug.interactions)
                put(KEY_ADMINISTRATION, drug.administration)
                put(KEY_PRECAUTIONS, drug.precautions)
                put(KEY_WEIGHT_BASED, drug.weightBased)
                put(KEY_DOSE_PER_KG, drug.dosePerKg)
                put(KEY_MAX_DAILY_DOSE, drug.maxDailyDose)
                put(KEY_AGE_DEPENDENT, drug.ageDependent)
                put(KEY_AGE_FORMULA, drug.ageFormula)
            }
            db.insert(TABLE_DRUGS, null, cv)
        }
    }
}
