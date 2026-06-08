# سجل حالة المشروع – midapp

## 📅 آخر تحديث
2026-06-09

## 🎯 الهدف
إضافة شاشات "الخطة الدراسية" مبنية على البيانات الحقيقية من `app_assets_map.json`، مع دمجها بشكل صحيح مع الشاشات الموجودة (DiplomaScreen، DirectoryScreen، إلخ).

## ✅ ما تم إنجازه
1. قراءة كاملة لـ `app_assets_map.json` (البيانات الفعلية).
2. قراءة السكربتات المولدة (`final_complete.py`، `fix_covers.py`، `generate_all_data.py`).
3. قراءة الملفات الأساسية: `MainActivity.kt`، `NavGraph.kt`، `HomeScreen.kt`، `DataProvider.kt`، `Theme.kt`، `GoldButton.kt`، `GlassCard.kt`.
4. إنشاء نموذج `StudyPlanModels.kt` ومستودع `StudyPlanRepository.kt`.
5. إنشاء شاشات أولية: `StudyPlanScreen.kt`، `SemesterScreen.kt`، `CourseDetailScreen.kt`، `MedicalSystemsScreen.kt` (قيد المراجعة).

## ⚠️ ما ينقص
- **لم يتم قراءة الشاشات الموجودة**: `DiplomaScreen.kt`، `DirectoryScreen.kt`، `ChapterScreen.kt`، `DeviceScreen.kt`، `SubjectContentScreen.kt`، `ContentBooksScreen.kt`.
- يجب تعديل الشاشات الجديدة لتتكامل مع الموجود أو تعديل الموجود ليشمل الخطة الدراسية.

## 📌 المطلوب في الجلسة القادمة
1. استلام الشاشات الموجودة من المستخدم.
2. تحليلها وتحديد كيفية دمج الخطة الدراسية.
3. تعديل الشاشات الجديدة أو الموجودة وفقاً لرغبة المستخدم.
4. تسجيل الحالة بعد كل خطوة.
