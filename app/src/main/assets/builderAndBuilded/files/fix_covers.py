#!/usr/bin/env python3
"""
إعادة بناء جميع الأغلفة الافتراضية بتصميم فاخر:
- نص عربي غير مكسور (arabic_reshaper + bidi + خط Amiri)
- شعار الكلية كعلامة مائية شفافة
- خلفية كحلية متدرجة وإطار ذهبي
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageEnhance
import arabic_reshaper
from bidi.algorithm import get_display

BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
JSON_PATH = DATA_DIR / "app_assets_map.json"
LOGO_PATH = BASE_DIR / "resources" / "images" / "app_logo_main.png"
# استخدام الشعار البديل إن وجد
ALT_LOGO = DATA_DIR / "images" / "logo_app.png"
FONT_DIR = BASE_DIR / "resources" / "font"
AMIRI_BOLD = FONT_DIR / "Amiri-Bold.ttf"
AMIRI_REGULAR = FONT_DIR / "Amiri-Regular.ttf"
# خط Traditional Arabic من ويندوز كبديل (يدعم العربية)
TRADO_PATH = Path("C:/Windows/Fonts/trado.ttf")

def load_arabic_font(size=24):
    """تحميل أفضل خط عربي متاح"""
    if AMIRI_BOLD.exists():
        return ImageFont.truetype(str(AMIRI_BOLD), size)
    if AMIRI_REGULAR.exists():
        return ImageFont.truetype(str(AMIRI_REGULAR), size)
    if TRADO_PATH.exists():
        return ImageFont.truetype(str(TRADO_PATH), size)
    # خط Arial يدعم العربية جزئياً في الإصدارات الحديثة
    arial = Path("C:/Windows/Fonts/arial.ttf")
    if arial.exists():
        return ImageFont.truetype(str(arial), size)
    return ImageFont.load_default()

def arabic_text(text):
    """معالجة النص العربي"""
    reshaped = arabic_reshaper.reshape(text)
    return get_display(reshaped)

def draw_centered_text(draw, text, y, font, fill, image_width):
    """رسم نص عربي في المنتصف"""
    try:
        bbox = draw.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        x = (image_width - tw) / 2
        draw.text((x, y), text, font=font, fill=fill)
    except:
        # fallback
        draw.text((10, y), text, font=font, fill=fill)

def create_cover_image(cover_path, title, subtitle=""):
    """إنشاء غلاف احترافي"""
    cover_path.parent.mkdir(parents=True, exist_ok=True)
    
    # أبعاد الصورة
    width, height = 300, 400
    img = Image.new('RGB', (width, height), color=(10, 17, 40))
    draw = ImageDraw.Draw(img)
    
    # ---- تحميل الخطوط ----
    font_title = load_arabic_font(24)
    font_sub = load_arabic_font(16)
    font_small = load_arabic_font(12)
    
    # ---- إضافة شعار كعلامة مائية ----
    logo = None
    if ALT_LOGO.exists():
        logo = ALT_LOGO
    elif LOGO_PATH.exists():
        logo = LOGO_PATH
    
    if logo and logo.exists():
        try:
            logo_img = Image.open(logo).convert("RGBA")
            # تصغير الشعار ووضعه في المنتصف بشفافية
            logo_img.thumbnail((120, 120), Image.Resampling.LANCZOS)
            # زيادة الشفافية
            alpha = logo_img.split()[3]
            alpha = alpha.point(lambda p: int(p * 0.25))
            logo_img.putalpha(alpha)
            # توسيط الشعار
            lx = (width - logo_img.width) // 2
            ly = (height - logo_img.height) // 2
            img.paste(logo_img, (lx, ly), logo_img)
        except Exception as e:
            print(f"تعذر إضافة الشعار: {e}")
    
    # ---- إطار ذهبي خارجي ----
    draw.rectangle([5, 5, width-5, height-5], outline=(212, 175, 55), width=2)
    draw.rectangle([8, 8, width-8, height-8], outline=(241, 196, 15), width=1)
    
    # ---- رأس الصفحة (شريط علوي) ----
    draw.rectangle([0, 0, width, 70], fill=(26, 47, 63))
    
    # ---- كتابة اسم الكلية ----
    college = arabic_text("كلية الطب والعلوم الصحية")
    draw_centered_text(draw, college, 15, font_small, (212, 175, 55), width)
    
    # ---- كتابة العنوان الرئيسي ----
    title_ar = arabic_text(title)
    # تقسيم العنوان إذا كان طويلاً
    if len(title_ar) > 25:
        # تقسيم إلى جزئين
        parts = title_ar.split()
        line1 = " ".join(parts[:len(parts)//2])
        line2 = " ".join(parts[len(parts)//2:])
        draw_centered_text(draw, line1, 150, font_title, (255, 255, 255), width)
        draw_centered_text(draw, line2, 180, font_title, (255, 255, 255), width)
    else:
        draw_centered_text(draw, title_ar, 160, font_title, (255, 255, 255), width)
    
    # ---- كتابة نص "دليل" أو "كتاب" ----
    if subtitle:
        sub_ar = arabic_text(subtitle)
        draw_centered_text(draw, sub_ar, 220, font_sub, (212, 175, 55), width)
    
    # ---- تذييل ----
    footer = arabic_text("برنامج الطب البشري")
    draw_centered_text(draw, footer, 360, font_small, (212, 175, 55), width)
    
    # ---- شريط سفلي ذهبي ----
    draw.rectangle([0, height-5, width, height], fill=(212, 175, 55))
    
    img.save(str(cover_path))
    print(f"🖼️  تم: {cover_path.relative_to(DATA_DIR)}")

def main():
    # تحميل JSON
    with open(JSON_PATH, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    total = 0
    # معالجة الكتب
    for book in data.get('books', []):
        cover_rel = book.get('cover_path')
        if not cover_rel:
            continue
        cover_path = DATA_DIR / cover_rel
        if cover_path.suffix.lower() == '.png':
            title = book['title']
            subtitle = f"الفصل {book.get('chapter', '')}" if book.get('chapter') else ""
            create_cover_image(cover_path, title, subtitle)
            total += 1
    
    # معالجة التدريب
    for section in ['trainer_guides', 'skills_guides', 'meetings', 'field_training']:
        for item in data.get('training', {}).get(section, []):
            cover_rel = item.get('cover_path')
            if not cover_rel:
                continue
            cover_path = DATA_DIR / cover_rel
            if cover_path.suffix.lower() == '.png':
                create_cover_image(cover_path, item['title'], item.get('type', ''))
                total += 1
    
    # الخطة الدراسية
    study = data.get('training', {}).get('study_plan', {})
    if study.get('cover_path'):
        cover_path = DATA_DIR / study['cover_path']
        if cover_path.suffix.lower() == '.png':
            create_cover_image(cover_path, study['title'], 'الخطة الدراسية')
            total += 1
    
    print(f"✅ تم إعادة إنشاء {total} غلافاً بجودة عالية.")

if __name__ == "__main__":
    main()
