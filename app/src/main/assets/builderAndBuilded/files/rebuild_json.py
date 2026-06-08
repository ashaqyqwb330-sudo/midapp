import json
from pathlib import Path

DATA = Path(__file__).parent / "data"
JSON = DATA / "app_assets_map.json"

data = {"version":"1.0","base_paths":{"data":"data/","images":"data/images/"},"books":[],"training":{"trainer_guides":[],"skills_guides":[],"meetings":[],"field_training":[],"study_plan":{}}}

for folder in sorted(DATA.iterdir()):
    if folder.is_dir() and folder.name[:2].isdigit():
        ch = int(folder.name[:2])
        for pdf in folder.glob("*.pdf"):
            if pdf.stat().st_size < 100: continue
            title = pdf.stem
            covers = list(folder.glob(f"غلاف_{title}.*"))
            cov = covers[0] if covers else None
            data["books"].append({"chapter":ch,"title":title,"type":"book","file":str(pdf.relative_to(DATA)),"cover":cov.name if cov else "","cover_path":str(cov.relative_to(DATA)) if cov else ""})

# تدريب (إن وجد)
train = DATA / "training"
if train.exists():
    for sec, key in [("guides","trainer_guides"),("skills","skills_guides"),("meetings","meetings"),("field_training","field_training")]:
        d = train / sec
        if d.exists():
            for pdf in d.glob("*.pdf"):
                if pdf.stat().st_size < 100: continue
                title = pdf.stem
                covers = list(d.glob(f"غلاف_{title}.*"))
                cov = covers[0] if covers else None
                data["training"][key].append({"title":title,"type":key,"file":str(pdf.relative_to(DATA)),"cover":cov.name if cov else "","cover_path":str(cov.relative_to(DATA)) if cov else ""})
    sp = DATA / "study_plan.pdf"
    if sp.exists():
        data["training"]["study_plan"] = {"title":"الخطة الدراسية","file":"study_plan.pdf"}

with open(JSON, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
print("✅ تم تحديث app_assets_map.json")
