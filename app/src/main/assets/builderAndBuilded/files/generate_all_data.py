#!/usr/bin/env python3
"""
السكربت الشامل النهائي المصحح:
- يضيف بيانات الأجهزة والمواد العامة إلى app_assets_map.json
- ينشئ المجلدات والملفات والأغلفة الافتراضية (العربية الجميلة)
- يولد PDF أولي احترافي لكل منها
- يدعم اللغة العربية بدون تكسير في الأغلفة
"""
import json, os, tempfile
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

BASE_DIR = Path(__file__).parent
JSON_PATH = BASE_DIR / "data" / "app_assets_map.json"
LOGO_PATH = BASE_DIR / "resources" / "images" / "app_logo_main.png"
FONT_DIR = BASE_DIR / "resources" / "font"
AMIRI_REGULAR = FONT_DIR / "Amiri-Regular.ttf"
AMIRI_BOLD = FONT_DIR / "Amiri-Bold.ttf"
DATA_DIR = BASE_DIR / "data"

def load_font():
    if AMIRI_REGULAR.exists():
        pdfmetrics.registerFont(TTFont('Amiri', str(AMIRI_REGULAR)))
        return 'Amiri', 'Amiri'
    if os.path.exists("C:/Windows/Fonts/trado.ttf"):
        pdfmetrics.registerFont(TTFont('Trado', "C:/Windows/Fonts/trado.ttf"))
        return 'Trado', 'Trado'
    return 'Helvetica', 'Helvetica'

FONT_NAME, FONT_NAME_BOLD = load_font()

def arabic_text(text):
    reshaped = arabic_reshaper.reshape(text)
    return get_display(reshaped)

def create_cover_image(cover_path, title):
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

    cover_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(cover_path)

def create_pdf(filepath, title, subtitle=""):
    c = canvas.Canvas(str(filepath), pagesize=A4)
    w, h = A4
    c.setFillColor(HexColor('#FCFCFC'))
    c.rect(0, 0, w, h, fill=1, stroke=0)
    c.setStrokeColor(HexColor('#D4AF37'))
    c.setLineWidth(2)
    c.rect(15, 15, w-30, h-30, fill=0, stroke=1)
    c.setFillColor(HexColor('#0A1128'))
    c.rect(0, h-3.5*cm, w, 3.5*cm, fill=1, stroke=0)
    def draw_arabic(text, x, y, font=FONT_NAME, size=14, color=HexColor('#D4AF37')):
        c.setFont(font, size)
        c.setFillColor(color)
        c.drawRightString(x, y, arabic_text(text))
    draw_arabic("كلية الطب والعلوم الصحية", w-2.5*cm, h-2.2*cm, FONT_NAME_BOLD, 18, HexColor('#D4AF37'))
    draw_arabic("القوات المسلحة اليمنية", w-2.5*cm, h-3.0*cm, FONT_NAME, 12, HexColor('#BDC3C7'))
    draw_arabic(title, w-2.5*cm, h-6*cm, FONT_NAME_BOLD, 24, HexColor('#0A1128'))
    if subtitle:
        draw_arabic(subtitle, w-2.5*cm, h-7*cm, FONT_NAME, 16, HexColor('#34495E'))
    draw_arabic("ملف أولي - يُرجى استبداله بالمحتوى النهائي", w-2.5*cm, h-8.5*cm, FONT_NAME, 11, HexColor('#95A5A6'))
    today = date.today().strftime("%Y/%m/%d")
    draw_arabic(f"تاريخ الإصدار: {today}", w-2.5*cm, h-9.5*cm, FONT_NAME, 10, HexColor('#7F8C8D'))
    c.setStrokeColor(HexColor('#D4AF37'))
    c.line(2.5*cm, h-10*cm, w-2.5*cm, h-10*cm)
    draw_arabic("برنامج الطب البشري - جميع الحقوق محفوظة © 2026", w-2.5*cm, 1.5*cm, FONT_NAME, 9, HexColor('#2C3E50'))
    c.save()

def generate_devices_and_general():
    with open(JSON_PATH, 'r', encoding='utf-8') as f:
        data = json.load(f)

    devices_data = {
        "5": ["الجهاز الهيكلي العضلي", "الجهاز القلبي الوعائي", "الجهاز التنفسي"],
        "6": ["الجهاز الهضمي", "الجهاز البولي التناسلي", "الجهاز الدموي واللمفاوي"],
        "7": ["الجهاز الصمائي", "الجهاز العصبي 1", "الجهاز العصبي 2"]
    }
    subjects_list = [
        "علم التشريح", "علم وظائف الأعضاء", "علم الأنسجة", "علم الأحياء الدقيقة والطفيليات",
        "الكيمياء الحيوية", "علم الأمراض", "علم الأدوية", "التقنيات التشخيصية المخبرية والإشعاعية",
        "الفحص والتقييم السريري", "الباطنية الطارئة", "علم التشريح الجراحي", "التخدير أثناء الحروب",
        "الجراحة العامة", "جراحة الحروب", "طب الطوارئ", "رعاية الحالات الحرجة والعناية المركزة",
        "الطب الوقائي", "التغذية العلاجية", "طب الأطفال"
    ]
    general_data = {
        "5": ["الثقافة القرآنية - يوم الفرقان", "مكارم الأخلاق وأخلاقيات المهنة"],
        "6": ["الثقافة القرآنية - الأحكام", "مبادى التغذية العلاجية عام"],
        "7": ["الثقافة القرآنية - التأريخ الإسلامي", "العلوم العسكرية المتخصصة"]
    }

    ch_prefix = {"5": "05", "6": "06", "7": "07"}
    
    for ch_num, devices in devices_data.items():
        if ch_num == "5":
            ch_folder = f"{ch_prefix[ch_num]}-الأجهزة الطبية"
        elif ch_num == "6":
            ch_folder = f"{ch_prefix[ch_num]}-الأجهزة المتقدمة"
        else:
            ch_folder = f"{ch_prefix[ch_num]}-الأجهزة المتخصصة"
        for device in devices:
            device_folder = DATA_DIR / ch_folder / device
            device_folder.mkdir(parents=True, exist_ok=True)
            for subject in subjects_list:
                subj_folder = device_folder / subject
                subj_folder.mkdir(parents=True, exist_ok=True)
                for ct in ["النظري", "العملي", "المرجع"]:
                    ct_folder = subj_folder / ct
                    ct_folder.mkdir(parents=True, exist_ok=True)
                    pdf_path = ct_folder / f"{subject}_{ct}.pdf"
                    create_pdf(pdf_path, f"{subject} - {ct}", f"{device} - {subject}")
                    cover_path = ct_folder / f"غلاف_{subject}_{ct}.png"
                    create_cover_image(cover_path, f"{subject}\n{ct}")
                    data['books'].append({
                        "chapter": int(ch_num),
                        "title": f"{subject} ({ct}) - {device}",
                        "type": "subject",
                        "file": str(pdf_path.relative_to(DATA_DIR)),
                        "cover": cover_path.name,
                        "cover_path": str(cover_path.relative_to(DATA_DIR))
                    })

    for ch_num, subjects in general_data.items():
        ch_folder = f"{ch_prefix[ch_num]}-مواد عامة"
        gen_folder = DATA_DIR / ch_folder
        gen_folder.mkdir(parents=True, exist_ok=True)
        for subject in subjects:
            subj_folder = gen_folder / subject
            subj_folder.mkdir(parents=True, exist_ok=True)
            for ct in ["النظري", "العملي", "المرجع"]:
                ct_folder = subj_folder / ct
                ct_folder.mkdir(parents=True, exist_ok=True)
                pdf_path = ct_folder / f"{subject}_{ct}.pdf"
                create_pdf(pdf_path, f"{subject} - {ct}", f"مادة عامة - {subject}")
                cover_path = ct_folder / f"غلاف_{subject}_{ct}.png"
                create_cover_image(cover_path, f"{subject}\n{ct}")
                data['books'].append({
                    "chapter": int(ch_num),
                    "title": f"{subject} ({ct})",
                    "type": "general",
                    "file": str(pdf_path.relative_to(DATA_DIR)),
                    "cover": cover_path.name,
                    "cover_path": str(cover_path.relative_to(DATA_DIR))
                })

    with open(JSON_PATH, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print("✅ تمت إضافة جميع بيانات الأجهزة والمواد العامة والملفات والأغلفة.")

if __name__ == "__main__":
    generate_devices_and_general()
