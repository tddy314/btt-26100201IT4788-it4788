"""Prepare a small Android demo batch and independent NumPy test fixtures.

Run from the repository root: python tools/prepare_sample.py
Only writes sample_data/ and Android test resources; source data is untouched.
"""
from pathlib import Path
import re
import shutil
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "android/app/src/test/resources"
RES.mkdir(parents=True, exist_ok=True)

a = np.arange(102, dtype=np.float64).reshape(2, 17, 3)
for name, data, version in [
    ("little_f64_v1.npy", a.astype("<f8"), (1, 0)),
    ("big_f32_v2.npy", a.astype(">f4"), (2, 0)),
    ("fortran_f64_v3.npy", np.asfortranarray(a), (3, 0)),
    ("object.npy", np.array([{"not": "numeric"}], dtype=object), (1, 0)),
    ("nan.npy", np.full((2, 17, 3), np.nan), (1, 0)),
]:
    with (RES / name).open("wb") as f:
        np.lib.format.write_array(f, data, version=version)

rng = np.random.default_rng(42)
p = rng.normal(size=(17, 3))
g = p @ np.array([[0., 1., 0.], [-1., 0., 0.], [0., 0., 1.]]) * 2.3
g += rng.normal(scale=.12, size=g.shape) + np.array([3., -7., 1.])
x = p - p.mean(0)
y = g - g.mean(0)
u, s, vt = np.linalg.svd(x.T @ y)
d = np.ones(3)
d[-1] = np.sign(np.linalg.det(u @ vt))
r = u @ np.diag(d) @ vt
scale = (s * d).sum() / (x * x).sum()
aligned = scale * x @ r + g.mean(0)
mpjpe = np.linalg.norm(p - g, axis=1).mean()
pa = np.linalg.norm(aligned - g, axis=1).mean()
with (RES / "metric_reference.csv").open("w", encoding="utf-8") as f:
    f.write(f"{mpjpe:.17g},{pa:.17g}\n")
    np.savetxt(f, np.concatenate([p, g], axis=1), delimiter=",", fmt="%.17g")

dataset = ROOT / "test_set"
pattern = re.compile(r"^(.+)_(\d+)_(\d+)\.jpg$", re.I)
for gt in sorted(dataset.rglob("*.npy")):
    clip = gt.stem.removeprefix("framed_").removesuffix("_h36m")
    data = np.load(gt, allow_pickle=False)
    images = []
    for img in sorted(gt.parent.glob("*.jpg")):
        m = pattern.fullmatch(img.name)
        if m and m[1] == clip:
            index = (int(m[2]) - 1) * 5 + int(m[3]) - 1
            if 0 <= index < len(data):
                images.append((index, img))
    if len(images) >= 5:
        target = ROOT / "sample_data" / gt.parent.parent.name / gt.parent.name
        target.mkdir(parents=True, exist_ok=True)
        shutil.copy2(gt, target / gt.name)
        for _, img in sorted(images)[:5]:
            shutil.copy2(img, target / img.name)
        shutil.copy2(gt, RES / "dataset_sample.npy")
        np.savetxt(RES / "dataset_sample_expected.csv", data.reshape(-1, 3), delimiter=",", fmt="%.17g")
        print(f"Sample: {target.relative_to(ROOT)}, 5 original images, GT {data.shape}")
        break
else:
    raise SystemExit("Need a clip with at least five original images and matching NPY")
print(f"NumPy reference MPJPE={mpjpe:.12f}, PA-MPJPE={pa:.12f}")
