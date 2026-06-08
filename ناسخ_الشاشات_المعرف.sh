PROJECT_DIR="/storage/4403-B0CA/midapp"
echo "📋 سكربت النسخ المُعرف – ينسخ الملفات مع أسمائها ومساراتها"
echo "بعد كل ملف، الصق المحتوى هنا ثم اضغط Enter للمتابعة"

FILES=(
  # الشاشات الأساسية (مطلوبة لفهم هيكل التنقل الحالي)
  "app/src/main/java/com/example/ui/screens/DiplomaScreen.kt"
  "app/src/main/java/com/example/ui/screens/DirectoryScreen.kt"
  "app/src/main/java/com/example/ui/screens/ChapterScreen.kt"
  "app/src/main/java/com/example/ui/screens/DeviceScreen.kt"
  "app/src/main/java/com/example/ui/screens/SubjectContentScreen.kt"
  "app/src/main/java/com/example/ui/screens/ContentBooksScreen.kt"
  "app/src/main/java/com/example/ui/screens/BooksScreen.kt"
  "app/src/main/java/com/example/ui/screens/SkillsScreen.kt"
  "app/src/main/java/com/example/ui/screens/SearchScreen.kt"
  "app/src/main/java/com/example/ui/screens/CalculatorsScreen.kt"
  "app/src/main/java/com/example/ui/screens/InventoryDashboardScreen.kt"
  "app/src/main/java/com/example/ui/screens/SimulationCenterScreen.kt"
  "app/src/main/java/com/example/ui/screens/QrScannerScreen.kt"
  "app/src/main/java/com/example/ui/screens/ReportsScreen.kt"
  "app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt"
  "app/src/main/java/com/example/ui/screens/MilitaryLockScreen.kt"
  # المكونات المخصصة (لإعادة الاستخدام)
  "app/src/main/java/com/example/ui/components/Book3DCard.kt"
  "app/src/main/java/com/example/ui/components/IntubationSimulator.kt"
  "app/src/main/java/com/example/ui/components/LaserScanRipple.kt"
  "app/src/main/java/com/example/ui/components/Shelf.kt"
  "app/src/main/java/com/example/ui/components/StaggeredEntrance.kt"
  "app/src/main/java/com/example/ui/components/TerminalText.kt"
  # البيانات والنماذج
  "app/src/main/java/com/example/data/DocumentNotesManager.kt"
  "app/src/main/java/com/example/data/DrugDatabaseHelper.kt"
  "app/src/main/java/com/example/data/OfflineCacheManager.kt"
  "app/src/main/java/com/example/model/Models.kt"
  "app/src/main/java/com/example/model/Drug.kt"
  "app/src/main/java/com/example/model/RecentCalc.kt"
  # الأنشطة المساعدة
  "app/src/main/java/com/example/AbgActivity.kt"
  "app/src/main/java/com/example/CalculatorsActivity.kt"
  "app/src/main/java/com/example/DetailActivity.kt"
  "app/src/main/java/com/example/DosageActivity.kt"
  "app/src/main/java/com/example/EgfrActivity.kt"
  "app/src/main/java/com/example/GcsActivity.kt"
  "app/src/main/java/com/example/ReportsActivity.kt"
)

for FILE in "${FILES[@]}"; do
  if [ -f "$PROJECT_DIR/$FILE" ]; then
    echo ""
    echo "===== معالجة: ${FILE} ====="
    # تجهيز النص المراد نسخه: عنوان بداية، المحتوى، عنوان نهاية
    TEXT_TO_COPY="===== بداية الملف: ${FILE} =====
$(cat $PROJECT_DIR/$FILE)
===== نهاية الملف: ${FILE} ====="
    
    # نسخ النص الكامل إلى الحافظة
    termux-clipboard-set "$TEXT_TO_COPY"
    echo "✅ تم نسخ محتوى: $FILE إلى الحافظة مع تعريفه"
    echo "👆 الصق المحتوى الآن في المحادثة..."
    read -p "ثم اضغط Enter للمتابعة إلى الملف التالي"
  else
    echo "⚠️ غير موجود: $FILE"
  fi
done

echo ""
echo "🎉 انتهت جميع الملفات."
