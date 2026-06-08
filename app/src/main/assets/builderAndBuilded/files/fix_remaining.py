#!/usr/bin/env python3
"""
- يعيد توليد أغلفة الفصول 8-13 (لأنها كانت مكسرة)
- يضيف الخطة الدراسية ودليل المتدرب الميداني إلى JSON
- ينشئ ملفات PDF وأغلفة لها (مع إنشاء المجلدات تلقائياً)
"""
import json
from pathlib import Path
from datetime import date
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont
import os

BASE_DIR = Path(__file__).parent
JSON_PATH = BASE_DIR / "data" / "app_assets_map.json"
DATA_DIR = BASE_DIR / "data"
FONT_DIR = BASE_DIR / "resources" / "font"
AMIRI_REGULAR = FONT_DIR / "Amiri-Regular.ttf"
AMIRI_BOLD = FONT_DIR / "Amiri-Bold.ttf"

def load_font():
    if AMIRI_BOLD.exists():
        pdfmetrics.registerFont(TTFont('AmiriBold', str(AMIRI_BOLD)))
        pdfmetrics.registerFont(TTFont('AmiriRegular', str(AMIRI_REGULAR)))
        return 'AmiriBold', 'AmiriRegular'
    if os.path.exists("C:/Windows/Fonts/trado.ttf"):
        pdfmetrics.registerFont(TTFont('Trado', "C:/Windows/Fonts/trado.ttf"))
        return 'Trado', 'Trado'
    return 'Helvetica-Bold', 'Helvetica'

FONT_B, FONT_R = load_font()

def arabic_text(text):
    reshaped = arabic_reshaper.reshape(text)
    return get_display(reshaped)

def create_cover_image(cover_path, title):
    cover_path.parent.mkdir(parents=True, exist_ok=True)  # إنشاء المجلد إذا لم يوجد
    img = Image.new('RGB', (300, 400), color=(10, 17, 40))
    draw = ImageDraw.Draw(img)
    try:
        if AMIRI_BOLD.exists():
            font_title = ImageFont.truetype(str(AMIRI_BOLD), 24)
            font_sub = ImageFont.truetype(str(AMIRI_REGULAR), 18)
        else:
            font_title = ImageFont.load_default()
            font_sub = font_title
    except:
        font_title = ImageFont.load_default()
        font_sub = font_title

    title_ar = arabic_text(title)
    try:
        bbox = draw.textbbox((0, 0), title_ar, font=font_title)
        tw, th = bbox[2]-bbox[0], bbox[3]-bbox[1]
    except:
        tw, th = 200, 30
    draw.text(((300-tw)/2, 150), title_ar, fill=(212, 175, 55), font=font_title)
    college = arabic_text("كلية الطب والعلوم الصحية")
    draw.text(((300-200)/2, 80), college, fill=(241, 196, 15), font=font_sub)
    footer = arabic_text("القوات المسلحة اليمنية")
    draw.text(((300-150)/2, 350), footer, fill=(255,255,255), font=font_sub)
    img.save(cover_path)

def create_pdf(filepath, title, subtitle=""):
    filepath.parent.mkdir(parents=True, exist_ok=True)  # حل المشكلة
    c = canvas.Canvas(str(filepath), pagesize=A4)
    w, h = A4
    c.setFillColor(HexColor('#FCFCFC'))
    c.rect(0, 0, w, h, fill=1, stroke=0)
    c.setStrokeColor(HexColor('#D4AF37'))
    c.setLineWidth(2)
    c.rect(15, 15, w-30, h-30, fill=0, stroke=1)
    c.setFillColor(HexColor('#0A1128'))
    c.rect(0, h-3.5*cm, w, 3.5*cm, fill=1, stroke=0)
    def draw_ar(text, x, y, font=FONT_R, size=14, color=HexColor('#D4AF37')):
        c.setFont(font, size)
        c.setFillColor(color)
        c.drawRightString(x, y, arabic_text(text))
    draw_ar("كلية الطب والعلوم الصحية", w-2.5*cm, h-2.2*cm, FONT_B, 18, HexColor('#D4AF37'))
    draw_ar("القوات المسلحة اليمنية", w-2.5*cm, h-3.0*cm, FONT_R, 12, HexColor('#BDC3C7'))
    draw_ar(title, w-2.5*cm, h-6*cm, FONT_B, 24, HexColor('#0A1128'))
    if subtitle:
        draw_ar(subtitle, w-2.5*cm, h-7*cm, FONT_R, 16, HexColor('#34495E'))
    draw_ar("ملف أولي - يُرجى استبداله بالمحتوى النهائي", w-2.5*cm, h-8.5*cm, FONT_R, 11, HexColor('#95A5A6'))
    today = date.today().strftime("%Y/%m/%d")
    draw_ar(f"تاريخ الإصدار: {today}", w-2.5*cm, h-9.5*cm, FONT_R, 10, HexColor('#7F8C8D'))
    c.setStrokeColor(HexColor('#D4AF37'))
    c.line(2.5*cm, h-10*cm, w-2.5*cm, h-10*cm)
    draw_ar("برنامج الطب البشري - جميع الحقوق محفوظة © 2026", w-2.5*cm, 1.5*cm, FONT_R, 9, HexColor('#2C3E50'))
    c.save()

def main():
    with open(JSON_PATH, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # إصلاح أغلفة الفصول 8-13
    for book in data.get('books', []):
        if book['chapter'] in [8,9,10,11,12,13]:
            file_path = DATA_DIR / book['file']
            cover_path = file_path.parent / f"غلاف_{book['title']}.png"
            create_cover_image(cover_path, book['title'])
            book['cover'] = cover_path.name
            book['cover_path'] = str(cover_path.relative_to(DATA_DIR))
            if not file_path.exists():
                create_pdf(file_path, book['title'], f"الفصل {book['chapter']}")

    # إضافة الخطة الدراسية
    study_plan_path = DATA_DIR / "study_plan.pdf"
    if not study_plan_path.exists():
        create_pdf(study_plan_path, "الخطة الدراسية", "الخطة الدراسية للبرنامج")
    cover_sp = DATA_DIR / "غلاف_الخطة_الدراسية.png"
    create_cover_image(cover_sp, "الخطة الدراسية")
    data['training']['study_plan'] = {
        "title": "الخطة الدراسية",
        "type": "study_plan",
        "file": "study_plan.pdf",
        "cover": "غلاف_الخطة_الدراسية.png",
        "cover_path": "غلاف_الخطة_الدراسية.png"
    }

    # إضافة دليل المتدرب الميداني
    field_training_dir = DATA_DIR / "training" / "field_training"
    field_training_dir.mkdir(parents=True, exist_ok=True)
    field_training = []
    for i in [1,2]:
        ftitle = f"دليل المتدرب التدريب الميداني - الجزء {i}"
        fpath = field_training_dir / f"field_guide_part{i}.pdf"
        if not fpath.exists():
            create_pdf(fpath, ftitle, "التدريب الميداني")
        fcover = field_training_dir / f"غلاف_{ftitle}.png"
        create_cover_image(fcover, ftitle)
        field_training.append({
            "title": ftitle,
            "type": "field_guide",
            "file": str(fpath.relative_to(DATA_DIR)),
            "cover": fcover.name,
            "cover_path": str(fcover.relative_to(DATA_DIR))
        })
    data['training']['field_training'] = field_training

    with open(JSON_PATH, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print("✅ تم إصلاح الأغلفة وإضافة الخطة الدراسية ودليل المتدرب الميداني.")

if __name__ == "__main__":
    main()
