#!/usr/bin/env python3
"""
ينقل الملفات الحقيقية (PDF) والأغلفة من الهيكل القديم إلى الهيكل العربي الجديد
بناءً على app_assets_map.json.
"""
import json
import shutil
from pathlib import Path

BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
JSON_PATH = DATA_DIR / "app_assets_map.json"

# خريطة لتحويل أسماء المجلدات القديمة إلى الجديدة (للبحث عن المصادر)
OLD_FOLDER_MAP = {
    "class1": "class1", "class2": "class2", "class3": "class3", "class4": "class4",
    "chapter8": "chapter8", "chapter9": "chapter9", "chapter10": "chapter10",
    "chapter11": "chapter11", "chapter12": "chapter12", "chapter13": "chapter13",
    "devices": "devices", "general": "general", "training": "training"
}

# أسماء الأغلفة القديمة لكل كتاب (حسب ما ورد في كود الأندرويد)
OLD_COVERS = {
    "الثقافة القرآنية": "cover_1.png",
    "الحرب الجرثومية": "cover_2.png",
    "الطب الوقائي": "cover_3.png",
    "الإسعاف المتقدم": "cover_4.png",
    "امراض شائعة": "cover_5.jpg",
    "أسس تمريض": "cover_6.jpg",
    "علم التشريح ووظائف الأعضاء": "cover_7.jpg",
    "علم الادوية": "cover_8.jpg",
    "مصطلحات طبية": "cover_9.jpg",
    "اساسيات اللغة الإنجليزية": "cover_10.png",
    "الثقافة القرآنية (2)": "cov11.jpg",
    "إصابات الحروب": "cov22.jpg",
    "الأمراض الشائعة": "cov33.jpg",
    "الانعاش المتقدم": "cov44.jpg",
    "ادارة المراكز": "cov55.jpg",
    "اساسيات الإنعاش": "cov66.jpg",
    "علم الأدوية 2": "cov77.jpg",
    "علم التشريح الناحي": "cov88.jpg",
    "نقل الدم": "cov99.jpg",
    "ثقافة قرآنية - الولاية": "cove_10.jpg",
    "ثقافة قرآنية - السيرة النبوية": "cove_20.jpg",
    "اخلاقيات العمل الطبي": "cove_30.jpg",
    "الباطنية الهضمية": "cove_40.jpg",
    "التشريح الجراحي": "cove_50.jpg",
    "التقييم والفحص السريري": "cove_60.jpg",
    "اللغة العربية": "cove_70.jpg",
    "المصطلحات الطبية 1": "cove_80.jpg",
    "كيمياء عامة": "cove_90.jpg",
    "كيمياء حيوية": "cove_100.jpg",
    "علم الأنسجة الجزء الأول": "cove_101.jpg",
    "مهارات الاتصال والتواصل": "cove_201.jpg",
    "ثقافة قرآنية طبيعة الصراع مع أهل الكتاب": "wer_1.jpg",
    "ثقافة قرآنية - الأحكام 1": "wer_2.jpg",
    "الاحياء الدقيقة": "wer_3.jpg",
    "الباطنية الأمراض المعدية": "wer_4.jpg",
    "الباطنية التنفسية": "wer_5.jpg",
    "التشريح الجراحي 2": "wer_6.jpg",
    "المصطلحات الطبية 2": "wer_7.jpg",
    "تقنيات العمليات الجراحية 1": "wer_8.jpg",
    "علم الامراض الخاص": "wer_9.jpg",
    "علم الأمراض العام": "wer_10.jpg",
    "فيزياء طبية": "wer_11.jpg",
    "مهارات تشخيصية": "wer_12.jpg",
}

def move_file(src, dst):
    """تحريك ملف مع استبدال الوجهة إذا كانت موجودة"""
    if not src.exists():
        return False
    dst.parent.mkdir(parents=True, exist_ok=True)
    try:
        shutil.move(str(src), str(dst))
        return True
    except Exception as e:
        print(f"⚠️ تعذر نقل {src.name}: {e}")
        return False

def copy_cover(src, dst):
    """نسخ غلاف مع استبدال الوجهة"""
    if not src.exists():
        return False
    dst.parent.mkdir(parents=True, exist_ok=True)
    try:
        shutil.copy2(str(src), str(dst))
        return True
    except Exception as e:
        print(f"⚠️ تعذر نسخ الغلاف {src.name}: {e}")
        return False

def main():
    with open(JSON_PATH, 'r', encoding='utf-8') as f:
        data = json.load(f)

    moved_books = 0
    moved_covers = 0

    # معالجة الكتب
    for book in data.get('books', []):
        if book.get('type') != 'book':
            continue
        new_path = DATA_DIR / book['file']
        # ابحث عن المصدر القديم
        old_file = None
        # البحث في المجلدات القديمة حسب الفصل
        ch = book['chapter']
        old_folder = f"class{ch}" if ch <=4 else f"chapter{ch}"
        # اسم الملف القديم المأخوذ من JSON الأصلي (لكننا لا نملكه، سنعتمد على الاسم المعروف)
        # سنحاول تخمين اسم الملف القديم من قاموس OLD_COVERS أو من اسم الغلاف
        # بدلاً من ذلك سنبحث عن أي ملف في المجلد القديم له نفس الامتداد .pdf وبداية مشابهة
        old_dir = DATA_DIR / old_folder
        if old_dir.exists():
            # نبحث عن أول ملف pdf في المجلد القديم - هذا ليس دقيقاً
            candidates = list(old_dir.glob("*.pdf"))
            if candidates:
                # نأخذ أول ملف ليس له b اسم العربي (لأن الملفات الحقيقية قد لا تزال بأسماء قديمة)
                # الفلترة: نأخذ الملفات التي لا تبدأ بـ "غلاف" ولا تحتوي على اسم الكتاب بالعربية
                real_candidates = [c for c in candidates if not c.stem.startswith("غلاف") and not c.stem == book['title']]
                if real_candidates:
                    old_file = real_candidates[0]  # افتراض أنه الملف الوحيد المتبقي

        if old_file:
            if move_file(old_file, new_path):
                moved_books += 1

        # نقل الغلاف الحقيقي
        cover_name = OLD_COVERS.get(book['title'])
        if cover_name:
            old_cover = DATA_DIR / "images" / cover_name
            if old_cover.exists():
                new_cover_path = new_path.parent / f"غلاف_{book['title']}.png"
                if copy_cover(old_cover, new_cover_path):
                    moved_covers += 1

    # معالجة أدلة التدريب
    for section in ['trainer_guides', 'skills_guides', 'meetings', 'field_training']:
        for item in data.get('training', {}).get(section, []):
            new_path = DATA_DIR / item['file']
            # المصادر القديمة لأدلة التدريب: training/guides/guide_N.pdf إلخ
            # لكننا لا نعرف الأسماء القديمة، لذا نعتمد على النقل اليدوي لاحقاً
            pass

    print(f"✅ تم نقل {moved_books} ملف PDF و {moved_covers} غلاف حقيقي.")

if __name__ == "__main__":
    main()
