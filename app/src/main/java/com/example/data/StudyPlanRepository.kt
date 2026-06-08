package com.example.data

import com.example.model.*

class StudyPlanRepository(private val dataProvider: DataProvider) {

    fun getStages(): List<StudyStage> {
        val allBooks = dataProvider.allBooks
        val semesterMap = allBooks.groupBy { it.chapter }

        val stage1 = StudyStage(
            id = 1,
            name = "المرحلة الأولى: التأسيس في العلوم الأساسية للميدان",
            description = "أساسيات التمريض، الأدوية، التشريح، الطوارئ",
            semesters = listOf(1, 2).mapNotNull { buildSemester(it, semesterMap) }
        )
        val stage2 = StudyStage(
            id = 2,
            name = "المرحلة الثانية: التأسيس في العلوم الأساسية للطب",
            description = "الأنسجة، الكيمياء، الأحياء الدقيقة، علم الأمراض",
            semesters = listOf(3, 4).mapNotNull { buildSemester(it, semesterMap) }
        )
        val stage3 = StudyStage(
            id = 3,
            name = "المرحلة الثالثة: التأسيسية بنظام الأجهزة",
            description = "دراسة أجهزة الجسم بتخصصاتها المتعددة",
            semesters = listOf(5, 6, 7).mapNotNull { buildSemester(it, semesterMap) }
        )
        val stage4 = StudyStage(
            id = 4,
            name = "المرحلة الرابعة: السريرية",
            description = "التدريب السريري والاختصاصات الدقيقة",
            semesters = (8..13).mapNotNull { buildSemester(it, semesterMap) }
        )
        return listOf(stage1, stage2, stage3, stage4)
    }

    private fun buildSemester(chapter: Int, map: Map<Int, List<BookEntry>>): StudySemester? {
        val books = map[chapter] ?: return null
        if (books.isEmpty()) return null
        val courses = books.map { book ->
            StudyCourse(
                name = book.title,
                chapter = book.chapter,
                type = book.type,
                file = book.file,
                coverPath = book.cover_path
            )
        }
        return StudySemester(
            id = chapter,
            name = "الفصل الدراسي $chapter",
            courses = courses
        )
    }

    fun getMedicalSystems(): List<MedSystem> {
        val subjects = dataProvider.allBooks.filter { it.type == "subject" }
        val grouped = subjects.groupBy { book ->
            book.title.substringAfterLast(" - ", "").trim()
        }.filterKeys { it.isNotEmpty() }

        return grouped.map { (deviceName, books) ->
            MedSystem(
                name = deviceName,
                subjects = books.map { it.title.substringBefore(" (") }.distinct()
            )
        }
    }
}
