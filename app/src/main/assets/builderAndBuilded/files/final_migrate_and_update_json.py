#!/usr/bin/env python3
"""
1. ينقل الكتب الحقيقية من النسخة الاحتياطية (الهيكل القديم) إلى الهيكل العربي الجديد.
2. يعيد بناء app_assets_map.json بالكامل بناءً على الملفات الفعلية الموجودة على القرص.
"""
import json
import shutil
from pathlib import Path

BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
JSON_PATH = DATA_DIR / "app_assets_map.json"

# ========== 1. إعداد مسار النسخة الاحتياطية ==========
# افترض أن النسخة الاحتياطية في المجلد الأصلي للمشروع القديم
BACKUP_BASE = BASE_DIR.parent / "in the half the project" / "data"

# ========== 2. جداول الترجمة ==========
FOLDER_MAP = {
    "class1": "01-الأساسيات",
    "class2": "02-التطبيقات المتقدمة1",
    "class3": "03-المقرر 3",
    "class4": "04-المقرر 4",
    "chapter8": "08-التخصصات الدقيقة",
    "chapter9": "09-البحث العلمي",
    "chapter10": "10-التدريب الميداني",
    "chapter11": "11-التطبيقات المتقدمة2",
    "chapter12": "12-المشاريع",
    "chapter13": "13-التخرج",
}

BOOK_NAMES = {
    "class1": ["bk1.pdf","bk2.pdf","bk3.pdf","bk4.pdf","bk5.pdf","bk6.pdf","bk7.pdf","bk8.pdf","bk9.pdf","book10.pdf"],
    "class2": ["bk_11.pdf","bk_22.pdf","bk_33.pdf","bk_44.pdf","bk_55.pdf","bk_66.pdf","bk_77.pdf","bk_88.pdf","bk_99.pdf"],
    "class3": ["bk10.pdf","bk20.pdf","bk30.pdf","bk40.pdf","bk50.pdf","bk60.pdf","bk70.pdf","bk80.pdf","bk90.pdf","bk100.pdf","bk101.pdf","bk201.pdf"],
    "class4": ["we_1.pdf","we_2.pdf","we_3.pdf","we_4.pdf","we_5.pdf","we_6.pdf","we_7.pdf","we_8.pdf","we_9.pdf","we_10.pdf","we_11.pdf","we_12.pdf"],
}

TITLE_MAP = {
    "class1": ["الثقافة القرآنية","الحرب الجرثومية","الطب الوقائي","الإسعاف المتقدم","امراض شائعة","أسس تمريض","علم التشريح ووظائف الأعضاء","علم الادوية","مصطلحات طبية","اساسيات اللغة الإنجليزية"],
    "class2": ["الثقافة القرآنية (2)","إصابات الحروب","الأمراض الشائعة","الانعاش المتقدم","ادارة المراكز","اساسيات الإنعاش","علم الأدوية 2","علم التشريح الناحي","نقل الدم"],
    "class3": ["ثقافة قرآنية - الولاية","ثقافة قرآنية - السيرة النبوية","اخلاقيات العمل الطبي","الباطنية الهضمية","التشريح الجراحي","التقييم والفحص السريري","اللغة العربية","المصطلحات الطبية 1","كيمياء عامة","كيمياء حيوية","علم الأنسجة الجزء الأول","مهارات الاتصال والتواصل"],
    "class4": ["ثقافة قرآنية طبيعة الصراع مع أهل الكتاب","ثقافة قرآنية - الأحكام 1","الاحياء الدقيقة","الباطنية الأمراض المعدية","الباطنية التنفسية","التشريح الجراحي 2","المصطلحات الطبية 2","تقنيات العمليات الجراحية 1","علم الامراض الخاص","علم الأمراض العام","فيزياء طبية","مهارات تشخيصية"],
}

def migrate_real_books():
    print("📚 نقل الكتب الحقيقية من النسخة الاحتياطية...")
    count = 0
    for old_folder, arabic_folder in FOLDER_MAP.items():
        if old_folder not in BOOK_NAMES:
            continue
        src_dir = BACKUP_BASE / old_folder
        dst_dir = DATA_DIR / arabic_folder
        dst_dir.mkdir(parents=True, exist_ok=True)
        if not src_dir.exists():
            print(f"⚠️  مجلد المصدر مفقود: {src_dir}")
            continue
        for i, old_filename in enumerate(BOOK_NAMES[old_folder]):
            src = src_dir / old_filename
            if not src.exists():
                print(f"⚠️  مفقود: {src}")
                continue
            new_name = TITLE_MAP[old_folder][i] + ".pdf"
            dst = dst_dir / new_name
            # لا نستبدل إذا كان الملف الهدف موجوداً وحجمه > 10 كيلوبايت (حقيقي)
            if dst.exists() and dst.stat().st_size > 10240:
                print(f"   ⏩  تخطي (موجود حقيقي): {new_name}")
                continue
            try:
                shutil.copy2(str(src), str(dst))
                print(f"   ✅  {old_filename} -> {new_name}")
                count += 1
            except Exception as e:
                print(f"   ❌  فشل {old_filename}: {e}")
    print(f"✅ تم نقل {count} كتاب حقيقي.")

def rebuild_json_from_disk():
    print("📝 إعادة بناء app_assets_map.json من الملفات الفعلية...")
    data = {
        "version": "1.0",
        "base_paths": {"data": "data/", "images": "data/images/", "sounds": "data/sounds/", "fonts": "data/fonts/"},
        "books": [],
        "training": {"trainer_guides": [], "skills_guides": [], "meetings": [], "field_training": [], "study_plan": {}}
    }
    # فحص جميع المجلدات التي تبدأ برقم (مثل 01-، 02-...)
    for folder in sorted(DATA_DIR.iterdir()):
        if not folder.is_dir() or not folder.name[0].isdigit():
            continue
        try:
            chapter_num = int(folder.name[:2])
        except:
            continue
        
        for pdf_file in folder.glob("*.pdf"):
            if pdf_file.stat().st_size < 100:  # تجاهل الملفات الصغيرة جداً
                continue
            title = pdf_file.stem
            cover_candidates = list(folder.glob(f"غلاف_{title}.*"))
            cover = cover_candidates[0] if cover_candidates else None
            
            data['books'].append({
                "chapter": chapter_num,
                "title": title,
                "type": "book",
                "file": str(pdf_file.relative_to(DATA_DIR)),
                "cover": cover.name if cover else "",
                "cover_path": str(cover.relative_to(DATA_DIR)) if cover else ""
            })
    
    # فحص مجلدات التدريب إن وجدت (اختياري)
    training_dir = DATA_DIR / "training"
    if training_dir.exists():
        for section, key in [("guides", "trainer_guides"), ("skills", "skills_guides"), ("meetings", "meetings"), ("field_training", "field_training")]:
            section_dir = training_dir / section
            if section_dir.exists():
                for pdf in section_dir.glob("*.pdf"):
                    if pdf.stat().st_size < 100:
                        continue
                    title = pdf.stem
                    cover = list(section_dir.glob(f"غلاف_{title}.*"))
                    data['training'][key].append({
                        "title": title,
                        "type": key,
                        "file": str(pdf.relative_to(DATA_DIR)),
                        "cover": cover[0].name if cover else "",
                        "cover_path": str(cover[0].relative_to(DATA_DIR)) if cover else ""
                    })
        # الخطة الدراسية
        sp = DATA_DIR / "study_plan.pdf"
        if sp.exists() and sp.stat().st_size > 100:
            data['training']['study_plan'] = {
                "title": "الخطة الدراسية",
                "type": "study_plan",
                "file": "study_plan.pdf"
            }
    
    with open(JSON_PATH, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"✅ تم حفظ {len(data['books'])} كتاب في app_assets_map.json.")

if __name__ == "__main__":
    if not BACKUP_BASE.exists():
        print(f"❌ مجلد النسخة الاحتياطية غير موجود: {BACKUP_BASE}")
        print("يرجى تعديل المتغير BACKUP_BASE في السكربت ليشير إلى النسخة الاحتياطية الصحيحة.")
        exit(1)
    migrate_real_books()
    rebuild_json_from_disk()
    print("🎉 تم النقل والتحديث. التطبيق جاهز.")
