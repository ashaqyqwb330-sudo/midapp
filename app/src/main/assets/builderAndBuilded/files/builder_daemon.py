# file: builder_daemon.py
# @builder:mode overwrite
import os
import re
import time
from pathlib import Path
import pyperclip

BASE_DIR = Path(__file__).parent

START_MARKER = "<<<START>>>"
END_MARKER = "<<<END>>>"

def ensure_dir(path):
    full_path = BASE_DIR / path
    full_path.mkdir(parents=True, exist_ok=True)
    print(f"📁 [OK] {full_path}")

def write_file(path, content, mode='w'):
    full_path = BASE_DIR / path
    full_path.parent.mkdir(parents=True, exist_ok=True)
    with open(full_path, mode, encoding='utf-8') as f:
        f.write(content)
    print(f"📄 [OK] {full_path}")

def delete_file(path):
    full_path = BASE_DIR / path
    if full_path.exists():
        full_path.unlink()
        print(f"🗑️ [OK] تم حذف {full_path}")

def clean_content(content):
    return content.rstrip() + '\n' if content else ''

def extract_target_path_and_mode(first_lines):
    target_path = None
    mode = 'w'
    for line in first_lines:
        line = line.strip()
        if not line:
            continue
        # @builder:file path
        if line.startswith('# @builder:') or line.startswith('// @builder:'):
            directive = line.split(':', 1)[1].strip()
            parts = directive.split(maxsplit=1)
            cmd = parts[0]
            arg = parts[1] if len(parts) > 1 else ''
            if cmd == 'file':
                target_path = arg
            elif cmd == 'mode':
                if arg.lower() in ['append', 'a']:
                    mode = 'a'
                elif arg.lower() in ['overwrite', 'w']:
                    mode = 'w'
            elif cmd == 'append':
                # دعم توجيه @builder:append للتوافق
                mode = 'a'
                target_path = arg
            continue
        # صيغ أخرى
        match = re.match(r'^(#|//)\s*file:\s*(.+)$', line, re.IGNORECASE)
        if match:
            target_path = match.group(2).strip()
            continue
        match = re.match(r'^--\s*FILE\s+(.+?)\s*--$', line, re.IGNORECASE)
        if match:
            target_path = match.group(1).strip()
            continue
        if line.startswith('###'):
            target_path = line[3:].strip()
            continue
        if re.match(r'^[\w\-./\\]+\.(py|qss|json|txt|md|bat)$', line, re.IGNORECASE):
            target_path = line
            continue
    return target_path, mode

def process_code_block(block_text):
    lines = block_text.split('\n')
    if not lines:
        return False
    first_chunk = lines[:15]
    target_path, mode = extract_target_path_and_mode(first_chunk)
    if not target_path:
        print("⚠️ لم يتم العثور على توجيه مسار صالح.")
        return False
    content_start_idx = 0
    for i, line in enumerate(lines):
        stripped = line.strip()
        if (stripped.startswith('# @builder:') or stripped.startswith('// @builder:') or
            re.match(r'^(#|//)\s*file:', stripped, re.IGNORECASE) or
            re.match(r'^--\s*FILE\s+', stripped, re.IGNORECASE) or
            stripped.startswith('###') or
            re.match(r'^[\w\-./\\]+\.(py|qss|json|txt|md|bat)$', stripped, re.IGNORECASE)):
            content_start_idx = i + 1
        else:
            break
    else:
        content_start_idx = len(lines)
    content = '\n'.join(lines[content_start_idx:])
    content = clean_content(content)
    print(f"🎯 الأمر: file | المسار: {target_path} | الوضع: {mode}")
    write_file(target_path, content, mode)
    return True

def extract_blocks_with_markers(text):
    pattern = re.escape(START_MARKER) + r'\s*\n(.*?)' + re.escape(END_MARKER)
    matches = re.findall(pattern, text, re.DOTALL)
    return [m.strip() for m in matches]

def process_clipboard():
    text = pyperclip.paste()
    if not text:
        return False
    print(f"\n📋 الحافظة تحتوي على {len(text)} حرفاً")
    blocks = extract_blocks_with_markers(text)
    if blocks:
        print(f"🔍 تم العثور على {len(blocks)} كتلة.")
    else:
        pattern_old = r'```[^\n]*\n(.*?)```'
        blocks = re.findall(pattern_old, text, re.DOTALL)
        if blocks:
            print(f"🔍 تم العثور على {len(blocks)} كتلة بالطريقة التقليدية.")
        else:
            print("ℹ️ لم يتم العثور على كتل.")
            return False
    any_processed = False
    for i, block in enumerate(blocks):
        print(f"\n--- معالجة الكتلة {i+1} ---")
        if process_code_block(block):
            any_processed = True
    if any_processed:
        print("\n✅ تمت المعالجة بنجاح.")
    else:
        print("\nℹ️ لم تتم معالجة أي كتلة.")
    return any_processed

def watcher_loop():
    print("=" * 60)
    print("👀 المراقب الذكي يعمل (نسخة محسّنة)")
    print("انسخ أي نص بين <<<START>>> و <<<END>>>")
    print("يجب أن تحتوي الكتلة على توجيه مسار مثل: # file: path")
    print("اضغط Ctrl+C للإيقاف.")
    print("=" * 60 + "\n")
    last_text = pyperclip.paste()
    while True:
        time.sleep(0.5)
        current_text = pyperclip.paste()
        if current_text != last_text:
            last_text = current_text
            process_clipboard()

if __name__ == "__main__":
    try:
        watcher_loop()
    except KeyboardInterrupt:
        print("\n👋 تم إيقاف المراقب.")
