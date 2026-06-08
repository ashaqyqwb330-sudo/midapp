#!/usr/bin/env python3
"""
نقل الكتب الحقيقية من الهيكل القديم (class1..4) إلى الهيكل العربي.
يبحث تلقائياً عن مجلد data القديم في المسار الأصلي.
"""
import json, shutil
from pathlib import Path

BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
JSON_PATH = DATA_DIR / "app_assets_map.json"

# خريطة المجلدات والأسماء
FOLDER_MAP = {
    "class1": ("01-الأساسيات", ["bk1.pdf","bk2.pdf","bk3.pdf","bk4.pdf","bk5.pdf","bk6.pdf","bk7.pdf","bk8.pdf","bk9.pdf","book10.pdf"],
               ["الثقافة القرآنية","الحرب الجرثومية","الطب الوقائي","الإسعاف المتقدم","امراض شائعة","أسس تمريض","علم التشريح ووظائف الأعضاء","علم الادوية","مصطلحات طبية","اساسيات اللغة الإنجليزية"]),
    "class2": ("02-التطبيقات المتقدمة1", ["bk_11.pdf","bk_22.pdf","bk_33.pdf","bk_44.pdf","bk_55.pdf","bk_66.pdf","bk_77.pdf","bk_88.pdf","bk_99.pdf"],
               ["الثقافة القرآنية (2)","إصابات الحروب","الأمراض الشائعة","الانعاش المتقدم","ادارة المراكز","اساسيات الإنعاش","علم الأدوية 2","علم التشريح الناحي","نقل الدم"]),
    "class3": ("03-المقرر 3", ["bk10.pdf","bk20.pdf","bk30.pdf","bk40.pdf","bk50.pdf","bk60.pdf","bk70.pdf","bk80.pdf","bk90.pdf","bk100.pdf","bk101.pdf","bk201.pdf"],
               ["ثقافة قرآنية - الولاية","ثقافة قرآنية - السيرة النبوية","اخلاقيات العمل الطبي","الباطنية الهضمية","التشريح الجراحي","التقييم والفحص السريري","اللغة العربية","المصطلحات الطبية 1","كيمياء عامة","كيمياء حيوية","علم الأنسجة الجزء الأول","مهارات الاتصال والتواصل"]),
    "class4": ("04-المقرر 4", ["we_1.pdf","we_2.pdf","we_3.pdf","we_4.pdf","we_5.pdf","we_6.pdf","we_7.pdf","we_8.pdf","we_9.pdf","we_10.pdf","we_11.pdf","we_12.pdf"],
               ["ثقافة قرآنية طبيعة الصراع مع أهل الكتاب","ثقافة قرآنية - الأحكام 1","الاحياء الدقيقة","الباطنية الأمراض المعدية","الباطنية التنفسية","التشريح الجراحي 2","المصطلحات الطبية 2","تقنيات العمليات الجراحية 1","علم الامراض الخاص","علم الأمراض العام","فيزياء طبية","مهارات تشخيصية"]),
}

def find_old_data():
    """البحث عن مجلد data القديم"""
    possible = [
        BASE_DIR.parent / "in the half the project" / "data",
        Path("F:/MedicalApp_Windows/in the half the project/data"),
        Path("F:/MedicalApp_Windows/in the half the project/MedicalApp_Final/data"),
    ]
    for p in possible:
        if p.exists():
            return p
    return None

def main():
    old_data = find_old_data()
    if not old_data:
        print("❌ لم يتم العثور على مجلد data القديم. الرجاء تحديد المسار يدوياً.")
        return

    print(f"📁 تم العثور على نسخة احتياطية: {old_data}")
    count = 0
    for old_dir, (new_dir_name, filenames, titles) in FOLDER_MAP.items():
        src_dir = old_data / old_dir
        if not src_dir.exists():
            print(f"⚠️  مجلد {old_dir} غير موجود في النسخة الاحتياطية.")
            continue
        dst_dir = DATA_DIR / new_dir_name
        dst_dir.mkdir(parents=True, exist_ok=True)
        for fname, title in zip(filenames, titles):
            src = src_dir / fname
            dst = dst_dir / f"{title}.pdf"
            if not src.exists():
                continue
            if dst.exists() and dst.stat().st_size > 10240:
                continue  # تخطي الملفات الحقيقية الكبيرة
            shutil.copy2(str(src), str(dst))
            print(f"✅ {fname} -> {title}.pdf")
            count += 1
    print(f"🎉 تم نقل {count} كتاب حقيقي.")

if __name__ == "__main__":
    main()
