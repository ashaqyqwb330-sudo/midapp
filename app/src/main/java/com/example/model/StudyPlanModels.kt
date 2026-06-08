package com.example.model

data class StudyStage(
    val id: Int,
    val name: String,
    val description: String,
    val semesters: List<StudySemester>
)

data class StudySemester(
    val id: Int,
    val name: String,
    val courses: List<StudyCourse>
)

data class StudyCourse(
    val name: String,
    val chapter: Int,
    val type: String,
    val file: String,
    val coverPath: String,
    val contents: List<CourseContent> = defaultContents()
)

data class CourseContent(
    val title: String,
    val icon: String
)

fun defaultContents(): List<CourseContent> = listOf(
    CourseContent("المحاضرات النظرية", "📖"),
    CourseContent("المحاضرات العملية", "🔬"),
    CourseContent("الدليل المرئي المصور", "🖼️"),
    CourseContent("دليل الطالب", "📘"),
    CourseContent("دليل المدرب", "📙"),
    CourseContent("المراجع الرئيسية", "📚"),
    CourseContent("الفيديوهات", "🎥"),
    CourseContent("الأنشطة", "🎯"),
    CourseContent("بنك الأسئلة", "❓"),
    CourseContent("العروض التقديمية", "📊"),
    CourseContent("المحاكاة والواقع الافتراضي", "🥽"),
    CourseContent("دليل المهارات والتدريب السريري", "🏥")
)

data class MedSystem(
    val name: String,
    val subjects: List<String>
)
