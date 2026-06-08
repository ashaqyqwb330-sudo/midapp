PROJECT_DIR="/storage/4403-B0CA/midapp"
echo "📋 سكربت النسخ المُصحح – سينسخ كل ملف إلى الحافظة"
echo "بعد كل ملف، الصق المحتوى هنا ثم اضغط Enter للمتابعة"

FILES=(
  "app/build.gradle.kts"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/java/com/example/MainActivity.kt"
  "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
  "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
  "app/src/main/java/com/example/ui/theme/Theme.kt"
  "app/src/main/java/com/example/ui/theme/Color.kt"
  "app/src/main/java/com/example/ui/theme/Type.kt"
  "app/src/main/java/com/example/ui/components/GoldButton.kt"
  "app/src/main/java/com/example/ui/components/GlassCard.kt"
  "app/src/main/java/com/example/data/DataProvider.kt"
  "build.gradle.kts"
  "settings.gradle.kts"
  "gradle/libs.versions.toml"
)

for FILE in "${FILES[@]}"; do
  if [ -f "$PROJECT_DIR/$FILE" ]; then
    echo "===== ${FILE} ====="
    termux-clipboard-set "$(cat $PROJECT_DIR/$FILE)"
    echo "✅ تم نسخ محتوى: $FILE إلى الحافظة"
    echo "👆 الصق المحتوى الآن في المحادثة..."
    read -p "ثم اضغط Enter للمتابعة إلى الملف التالي"
  else
    echo "⚠️ غير موجود: $FILE"
  fi
done

echo "🎉 انتهت جميع الملفات."
