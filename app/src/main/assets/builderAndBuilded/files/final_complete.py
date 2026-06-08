#!/usr/bin/env python3
"""
السكربت النهائي الكامل – لبناء المشروع من الصفر.
يشمل: كل الكتب (1-13)، الأجهزة، المواد العامة، التدريب، الخطة الدراسية، دليل المتدرب.
أسماء عربية، مجلدات مرقمة، أغلفة افتراضية جميلة، و PDF أولي احترافي.
"""
import json, os
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

# ========== الأساسيات ==========
BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
JSON_PATH = DATA_DIR / "app_assets_map.json"
LOGO_PATH = BASE_DIR / "resources" / "images" / "app_logo_main.png"
FONT_DIR = BASE_DIR / "resources" / "font"
AMIRI_REG = FONT_DIR / "Amiri-Regular.ttf"
AMIRI_B = FONT_DIR / "Amiri-Bold.ttf"

# ========== الخطوط ==========
def load_font():
    if AMIRI_B.exists():
        pdfmetrics.registerFont(TTFont('AmiriB', str(AMIRI_B)))
        pdfmetrics.registerFont(TTFont('AmiriR', str(AMIRI_REG)))
        return 'AmiriB', 'AmiriR'
    trad = Path("C:/Windows/Fonts/trado.ttf")
    if trad.exists():
        pdfmetrics.registerFont(TTFont('Trado', str(trad)))
        return 'Trado', 'Trado'
    return 'Helvetica-Bold', 'Helvetica'

FB, FR = load_font()

def ar(text):
    return get_display(arabic_reshaper.reshape(text))

# ========== غلاف افتراضي ==========
def create_cover(cover_path, title):
    cover_path.parent.mkdir(parents=True, exist_ok=True)
    img = Image.new('RGB', (300, 400), color=(10, 17, 40))
    d = ImageDraw.Draw(img)
    try:
        ft = ImageFont.truetype(str(AMIRI_B), 24) if AMIRI_B.exists() else ImageFont.load_default()
        fs = ImageFont.truetype(str(AMIRI_REG), 18) if AMIRI_REG.exists() else ft
    except:
        ft = fs = ImageFont.load_default()
    ta = ar(title)
    d.text((20, 150), ta, fill=(212, 175, 55), font=ft)
    d.text((20, 80), ar("كلية الطب والعلوم الصحية"), fill=(241, 196, 15), font=fs)
    d.text((20, 350), ar("القوات المسلحة اليمنية"), fill=(255,255,255), font=fs)
    img.save(cover_path)

# ========== PDF أولي ==========
def create_pdf(filepath, title, subtitle=""):
    filepath.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(filepath), pagesize=A4)
    w, h = A4
    c.setFillColor(HexColor('#FCFCFC'))
    c.rect(0, 0, w, h, fill=1, stroke=0)
    c.setStrokeColor(HexColor('#D4AF37'))
    c.setLineWidth(2)
    c.rect(15, 15, w-30, h-30)
    c.setFillColor(HexColor('#0A1128'))
    c.rect(0, h-3.5*cm, w, 3.5*cm, fill=1, stroke=0)
    def wr(t, x, y, f=FR, s=14, clr=HexColor('#D4AF37')):
        c.setFont(f, s)
        c.setFillColor(clr)
        c.drawRightString(x, y, ar(t))
    wr("كلية الطب والعلوم الصحية", w-2.5*cm, h-2.2*cm, FB, 18, HexColor('#D4AF37'))
    wr("القوات المسلحة اليمنية", w-2.5*cm, h-3.0*cm, FR, 12, HexColor('#BDC3C7'))
    wr(title, w-2.5*cm, h-6*cm, FB, 24, HexColor('#0A1128'))
    if subtitle: wr(subtitle, w-2.5*cm, h-7*cm, FR, 16, HexColor('#34495E'))
    wr("ملف أولي - يُرجى استبداله بالمحتوى النهائي", w-2.5*cm, h-8.5*cm, FR, 11, HexColor('#95A5A6'))
    wr(f"تاريخ الإصدار: {date.today().strftime('%Y/%m/%d')}", w-2.5*cm, h-9.5*cm, FR, 10, HexColor('#7F8C8D'))
    c.line(2.5*cm, h-10*cm, w-2.5*cm, h-10*cm)
    wr("برنامج الطب البشري - جميع الحقوق محفوظة © 2026", w-2.5*cm, 1.5*cm, FR, 9, HexColor('#2C3E50'))
    c.save()

# ========== هيكل البيانات ==========
def build_all():
    data = {"version":"1.0","base_paths":{"data":"data/","images":"data/images/","sounds":"data/sounds/","fonts":"data/fonts/"},"books":[],"training":{"trainer_guides":[],"skills_guides":[],"meetings":[],"field_training":[],"study_plan":{}}}
    # الفصول 1-4
    for ch in range(1,5):
        folder = DATA_DIR / f"{ch:02d}-{['الأساسيات','التطبيقات المتقدمة1','المقرر 3','المقرر 4'][ch-1]}"
        folder.mkdir(parents=True, exist_ok=True)
        for t,f in [("الثقافة القرآنية","bk1.pdf"),("الحرب الجرثومية","bk2.pdf"),("الطب الوقائي","bk3.pdf"),("الإسعاف المتقدم","bk4.pdf"),("امراض شائعة","bk5.pdf"),("أسس تمريض","bk6.pdf"),("علم التشريح ووظائف الأعضاء","bk7.pdf"),("علم الادوية","bk8.pdf"),("مصطلحات طبية","bk9.pdf"),("اساسيات اللغة الإنجليزية","book10.pdf")] if ch==1 else [] + ([("الثقافة القرآنية (2)","bk_11.pdf"),("إصابات الحروب","bk_22.pdf"),("الأمراض الشائعة","bk_33.pdf"),("الانعاش المتقدم","bk_44.pdf"),("ادارة المراكز","bk_55.pdf"),("اساسيات الإنعاش","bk_66.pdf"),("علم الأدوية 2","bk_77.pdf"),("علم التشريح الناحي","bk_88.pdf"),("نقل الدم","bk_99.pdf")] if ch==2 else []) + ([("ثقافة قرآنية - الولاية","bk10.pdf"),("ثقافة قرآنية - السيرة النبوية","bk20.pdf"),("اخلاقيات العمل الطبي","bk30.pdf"),("الباطنية الهضمية","bk40.pdf"),("التشريح الجراحي","bk50.pdf"),("التقييم والفحص السريري","bk60.pdf"),("اللغة العربية","bk70.pdf"),("المصطلحات الطبية 1","bk80.pdf"),("كيمياء عامة","bk90.pdf"),("كيمياء حيوية","bk100.pdf"),("علم الأنسجة الجزء الأول","bk101.pdf"),("مهارات الاتصال والتواصل","bk201.pdf")] if ch==3 else []) + ([("ثقافة قرآنية طبيعة الصراع مع أهل الكتاب","we_1.pdf"),("ثقافة قرآنية - الأحكام 1","we_2.pdf"),("الاحياء الدقيقة","we_3.pdf"),("الباطنية الأمراض المعدية","we_4.pdf"),("الباطنية التنفسية","we_5.pdf"),("التشريح الجراحي 2","we_6.pdf"),("المصطلحات الطبية 2","we_7.pdf"),("تقنيات العمليات الجراحية 1","we_8.pdf"),("علم الامراض الخاص","we_9.pdf"),("علم الأمراض العام","we_10.pdf"),("فيزياء طبية","we_11.pdf"),("مهارات تشخيصية","we_12.pdf")] if ch==4 else []):
            pdf = folder / f"{t}.pdf"
            create_pdf(pdf, t, f"الفصل {ch}")
            cov = folder / f"غلاف_{t}.png"
            create_cover(cov, t)
            data['books'].append({"chapter":ch,"title":t,"type":"book","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # الفصول 8-13
    for ch,items in {8:[("التخصص الدقيق في الجراحة","specialty1.pdf"),("التخصص الدقيق في الباطنة","specialty2.pdf")],9:[("منهجية البحث العلمي","research1.pdf"),("الإحصاء الطبي","research2.pdf")],10:[("دليل التدريب الميداني","field1.pdf"),("تقييم الأداء الميداني","field2.pdf")],11:[("التطبيقات المتقدمة في التشخيص","advanced1.pdf"),("التطبيقات المتقدمة في العلاج","advanced2.pdf")],12:[("إدارة المشاريع الطبية","project1.pdf"),("تقييم المشاريع البحثية","project2.pdf")],13:[("متطلبات التخرج","graduation1.pdf"),("حفل التخرج","graduation2.pdf")]}.items():
        folder = DATA_DIR / f"{ch:02d}-{['التخصصات الدقيقة','البحث العلمي','التدريب الميداني','التطبيقات المتقدمة2','المشاريع','التخرج'][ch-8]}"
        folder.mkdir(parents=True, exist_ok=True)
        for t,f in items:
            pdf = folder / f"{t}.pdf"
            create_pdf(pdf, t, f"الفصل {ch}")
            cov = folder / f"غلاف_{t}.png"
            create_cover(cov, t)
            data['books'].append({"chapter":ch,"title":t,"type":"book","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # الأجهزة (5-7)
    devs = {"5":["الجهاز الهيكلي العضلي","الجهاز القلبي الوعائي","الجهاز التنفسي"],"6":["الجهاز الهضمي","الجهاز البولي التناسلي","الجهاز الدموي واللمفاوي"],"7":["الجهاز الصمائي","الجهاز العصبي 1","الجهاز العصبي 2"]}
    subs = ["علم التشريح","علم وظائف الأعضاء","علم الأنسجة","علم الأحياء الدقيقة والطفيليات","الكيمياء الحيوية","علم الأمراض","علم الأدوية","التقنيات التشخيصية المخبرية والإشعاعية","الفحص والتقييم السريري","الباطنية الطارئة","علم التشريح الجراحي","التخدير أثناء الحروب","الجراحة العامة","جراحة الحروب","طب الطوارئ","رعاية الحالات الحرجة والعناية المركزة","الطب الوقائي","التغذية العلاجية","طب الأطفال"]
    for ch,devs_list in devs.items():
        folder = DATA_DIR / f"{int(ch):02d}-{'الأجهزة الطبية' if ch=='5' else 'الأجهزة المتقدمة' if ch=='6' else 'الأجهزة المتخصصة'}"
        for dev in devs_list:
            dev_folder = folder / dev
            for sub in subs:
                for ct in ["النظري","العملي","المرجع"]:
                    ct_folder = dev_folder / sub / ct
                    pdf = ct_folder / f"{sub}_{ct}.pdf"
                    create_pdf(pdf, f"{sub} - {ct}", f"{dev}")
                    cov = ct_folder / f"غلاف_{sub}_{ct}.png"
                    create_cover(cov, f"{sub}\n{ct}")
                    data['books'].append({"chapter":int(ch),"title":f"{sub} ({ct}) - {dev}","type":"subject","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # المواد العامة
    gen = {"5":["الثقافة القرآنية - يوم الفرقان","مكارم الأخلاق وأخلاقيات المهنة"],"6":["الثقافة القرآنية - الأحكام","مبادى التغذية العلاجية عام"],"7":["الثقافة القرآنية - التأريخ الإسلامي","العلوم العسكرية المتخصصة"]}
    for ch,items in gen.items():
        folder = DATA_DIR / f"{int(ch):02d}-مواد عامة"
        for sub in items:
            for ct in ["النظري","العملي","المرجع"]:
                ct_folder = folder / sub / ct
                pdf = ct_folder / f"{sub}_{ct}.pdf"
                create_pdf(pdf, f"{sub} - {ct}", "مادة عامة")
                cov = ct_folder / f"غلاف_{sub}_{ct}.png"
                create_cover(cov, f"{sub}\n{ct}")
                data['books'].append({"chapter":int(ch),"title":f"{sub} ({ct})","type":"general","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # التدريب
    T = ["دليل الثقافة القرآنية","دليل تشريح ووظائف الاعضاء","دليل اسس تمريض","دليل امراض شائعة 1","دليل امراض شائعة 2","دليل الحرب الجرثومية","دليل علم الادوية 1","دليل علم الادوية 2","دليل مبادئ الطب الوقائي","دليل اساسيات اللغة الإنجليزية","دليل اسعاف حربي متقدم","دليل اللغة العربية","دليل الثقافة القرآنية (2)","دليل التشريح السريري","دليل أساسيات الإنعاش","دليل إصابات الحروب","دليل علم الادوية الطارئة","دليل أساسيات نقل الدم","دليل إدارة المراكز الميدانية","دليل مصطلحات طبية","دليل تأهيل طبي"]
    S = ["دليل مهارات "+t for t in T]
    for i,t in enumerate(T,1):
        pdf = DATA_DIR / "training" / "guides" / f"{t}.pdf"
        create_pdf(pdf, t, "دليل المدرب")
        cov = pdf.parent / f"غلاف_{t}.png"
        create_cover(cov, t)
        data['training']['trainer_guides'].append({"title":t,"type":"trainer_guide","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    for i,t in enumerate(S,1):
        pdf = DATA_DIR / "training" / "skills" / f"{t}.pdf"
        create_pdf(pdf, t, "دليل المهارات")
        cov = pdf.parent / f"غلاف_{t}.png"
        create_cover(cov, t)
        data['training']['skills_guides'].append({"title":t,"type":"skills_guide","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # اللقاءات
    for i,t in enumerate(["لقاءات السيد بالصحيين - الجزء الأول","لقاءات السيد بالصحيين - الجزء الثاني"],1):
        pdf = DATA_DIR / "training" / "meetings" / f"meeting{i}.pdf"
        create_pdf(pdf, t, "لقاءات")
        cov = pdf.parent / f"غلاف_{t}.png"
        create_cover(cov, t)
        data['training']['meetings'].append({"title":t,"type":"meeting","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # الخطة الدراسية
    sp = DATA_DIR / "study_plan.pdf"
    create_pdf(sp, "الخطة الدراسية")
    cov = DATA_DIR / "غلاف_الخطة_الدراسية.png"
    create_cover(cov, "الخطة الدراسية")
    data['training']['study_plan'] = {"title":"الخطة الدراسية","type":"study_plan","file":"study_plan.pdf","cover":"غلاف_الخطة_الدراسية.png","cover_path":"غلاف_الخطة_الدراسية.png"}
    # دليل المتدرب الميداني
    for i in [1,2]:
        t = f"دليل المتدرب التدريب الميداني - الجزء {i}"
        pdf = DATA_DIR / "training" / "field_training" / f"field_guide_part{i}.pdf"
        create_pdf(pdf, t, "التدريب الميداني")
        cov = pdf.parent / f"غلاف_{t}.png"
        create_cover(cov, t)
        data['training']['field_training'].append({"title":t,"type":"field_guide","file":str(pdf.relative_to(DATA_DIR)),"cover":cov.name,"cover_path":str(cov.relative_to(DATA_DIR))})
    # حفظ JSON
    with open(JSON_PATH, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print("✅ تم بناء المشروع بالكامل بنجاح!")

if __name__ == "__main__":
    build_all()
