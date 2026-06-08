PROJECT_DIR="/storage/4403-B0CA/midapp"
echo "📋 جاري نسخ محتويات الملفات على دفعات..."

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
    echo "===== $FILE =====" | termux-clipboard-set
    cat "$PROJECT_DIR/$FILE" | termux-clipboard-set --append
    echo "✅ تم نسخ: $FILE"
    read -p "اضغط Enter بعد لصق المحتوى في المحادثة..."
  else
    echo "⚠️ غير موجود: $FILE"
  fi
done

echo "🎉 انتهت جميع الدفعات."
