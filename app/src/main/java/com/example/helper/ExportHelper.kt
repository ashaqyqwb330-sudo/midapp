package com.example.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.screens.ClinicalReport
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Data bundle representing all exports.
     */
    data class ExportDataPackage(
        val appName: String = "Medical Library Tactical",
        val exportTimestamp: String,
        val documentNotes: List<DocumentNoteExport>,
        val clinicalReports: List<ClinicalReport>
    )

    data class DocumentNoteExport(
        val documentFile: String,
        val noteContent: String
    )

    /**
     * Retrieves all saved notes and clinical reports.
     */
    fun collectDataForExport(context: Context): ExportDataPackage {
        // Collect Document Notes
        val documentNotesPrefs = context.getSharedPreferences("document_notes_storage", Context.MODE_PRIVATE)
        val documentNotesList = mutableListOf<DocumentNoteExport>()
        
        try {
            val allEntries = documentNotesPrefs.all
            for ((key, value) in allEntries) {
                if (value is String && value.isNotEmpty()) {
                    documentNotesList.add(DocumentNoteExport(documentFile = key, noteContent = value))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Collect Clinical Reports
        val clinicalReportsPrefs = context.getSharedPreferences("clinical_reports_prefs", Context.MODE_PRIVATE)
        var reportsList = emptyList<ClinicalReport>()
        val json = clinicalReportsPrefs.getString("reports_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<ClinicalReport>>() {}.type
                reportsList = gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val df = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return ExportDataPackage(
            exportTimestamp = df.format(Date()),
            documentNotes = documentNotesList,
            clinicalReports = reportsList
        )
    }

    /**
     * Generates a JSON string of all medical security notes and clinical datasets.
     */
    fun generateJsonString(data: ExportDataPackage): String {
        return gson.toJson(data)
    }

    /**
     * Generates a beautiful tactical text representation in Arabic and English.
     */
    fun generateTextString(data: ExportDataPackage): String {
        val sb = StringBuilder()
        sb.append("========================================================\n")
        sb.append("🛡️ جدار الحماية الطبي الميداني - تقرير التصدير الموحد 🧬\n")
        sb.append("========================================================\n")
        sb.append("تاريخ التصدير التكتيكي: ${data.exportTimestamp}\n")
        sb.append("الجهة المخولة: وحدة الإسناد الطبي والتعليم العسكري المستمر\n")
        sb.append("نظرة عامة: تجميع المخرجات العلمية والتعليقات والتقرير السريري لحامل الجهاز\n")
        sb.append("========================================================\n\n")

        sb.append("📁 أولاً: تعليقات وملاحظات المناهج الطبية المقررة (${data.documentNotes.size})\n")
        sb.append("--------------------------------------------------------\n")
        if (data.documentNotes.isEmpty()) {
            sb.append("• لا توجد ملاحظات أو هوامش مكتوبة على الكشاف والوثائق حالياً.\n")
        } else {
            data.documentNotes.forEachIndexed { index, item ->
                sb.append("${index + 1}. [الوثيقة/الملف الدراسي]: ${item.documentFile}\n")
                sb.append("   [نص الملاحظة المدونة]:\n")
                sb.append("   \"${item.noteContent}\"\n")
                sb.append("   -------------------------------------------------\n")
            }
        }
        sb.append("\n\n")

        sb.append("📋 ثانياً: التقارير الطبية والتقييمات السريرية المحفوظة (${data.clinicalReports.size})\n")
        sb.append("--------------------------------------------------------\n")
        if (data.clinicalReports.isEmpty()) {
            sb.append("• لا توجد سجلات مرضية أو تقارير تشخيصية مسجلة حالياً.\n")
        } else {
            data.clinicalReports.forEachIndexed { index, report ->
                sb.append("${index + 1}. [تقرير مريض]: ${report.patientName} (${report.patientId})\n")
                sb.append("   [التاريخ]: ${report.date}\n")
                sb.append("   [الجهاز المستخدم / الوحدة]: ${report.deviceName}\n")
                if (report.gcsScore > 0) {
                    sb.append("   • مقياس غلاسكو للوعي (GCS): ${report.gcsScore} (${report.gcsSeverity})\n")
                }
                if (report.egfrVal > 0) {
                    sb.append("   • ترشيح كبيبي (eGFR): ${report.egfrVal} مل/دقيقة (${report.egfrInterpretation})\n")
                }
                if (report.abgDiagnosis.isNotEmpty()) {
                    sb.append("   • تشخيص غازات الدم (ABG): ${report.abgDiagnosis}\n")
                }
                if (report.details.isNotEmpty()) {
                    sb.append("   • ملاحظات سريرية إضافية:\n")
                    sb.append("     \"${report.details}\"\n")
                }
                sb.append("   -------------------------------------------------\n")
            }
        }
        sb.append("\n========================================================\n")
        sb.append("نهاية سجل التصدير الآمن. مخصص للاستخدام الخارجي المعتمد.\n")
        return sb.toString()
    }

    /**
     * Exports the gathered data as a file to the cache directory and triggers the Share intent.
     */
    fun triggerExportShare(context: Context, asJson: Boolean) {
        val dataPackage = collectDataForExport(context)
        
        if (dataPackage.documentNotes.isEmpty() && dataPackage.clinicalReports.isEmpty()) {
            Toast.makeText(context, "⚠️ خطأ: لا توجد هوامش أو وثائق أو تقارير محفوظة لتصديرها!", Toast.LENGTH_LONG).show()
            return
        }

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        
        val fileName = if (asJson) "Tactical_Medical_Export_$timestamp.json" else "Tactical_Medical_Export_$timestamp.txt"
        val mimeType = if (asJson) "application/json" else "text/plain"
        val content = if (asJson) generateJsonString(dataPackage) else generateTextString(dataPackage)

        try {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(content)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تصدير البيانات الطبية والعلمية الميدانية 🧬")
                putExtra(Intent.EXTRA_TEXT, "مرفق طيه تصدير البيانات والملاحظات والتقارير الطبية الميدانية.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "تصدير الملف عبر:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            Toast.makeText(context, "✅ تم توليد ملف التصدير بنجاح وزرع تصاريح المشاركة!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to direct share text if file generation fails
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "تصدير البيانات الطبية 🧬")
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                val chooser = Intent.createChooser(fallbackIntent, "مشاركة المحتوى المكتوب:")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "فشل تصدير الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
