PROJECT_DIR="/storage/4403-B0CA/midapp"
FILE="$PROJECT_DIR/app/src/main/assets/data/app_assets_map.json"
echo "📋 سيتم نسخ السطور من 2539 إلى نهاية الملف..."
tail -n +2539 "$FILE" | termux-clipboard-set
echo "✅ تم نسخ باقي الملف إلى الحافظة"
echo "👈 الصق المحتوى الآن في المحادثة"
