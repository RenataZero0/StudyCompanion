# -*- coding: utf-8 -*-
"""生成 StudyCompanion 的 assets/app.ico（多尺寸程序图标）"""
import os, sys
from PIL import Image, ImageDraw
sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))      # tools\
ROOT = os.path.dirname(HERE)                           # StudyCompanion\
ASSETS = os.path.join(ROOT, "assets")
os.makedirs(ASSETS, exist_ok=True)
OUT = ROOT
TOP = (94, 146, 245)      # #5E92F5
BOT = (43, 86, 219)       # #2B56DB
WHITE = (255, 255, 255)
ACCENT = (47, 96, 224)
SS = 6                    # 超采样倍数


def grad(size):
    im = Image.new("RGB", (1, size))
    d = ImageDraw.Draw(im)
    for y in range(size):
        t = y / max(1, size - 1)
        d.point((0, y), tuple(int(TOP[i] + (BOT[i] - TOP[i]) * t) for i in range(3)))
    return im.resize((size, size), Image.BILINEAR)


def squircle(size):
    S = size * SS
    m = Image.new("L", (S, S), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.235), fill=255)
    return m.resize((size, size), Image.LANCZOS)


def draw_check(d, box, color, width):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    p1 = (x0 + w * 0.04, y0 + h * 0.52)
    p2 = (x0 + w * 0.36, y0 + h * 0.86)
    p3 = (x0 + w * 0.98, y0 + h * 0.12)
    d.line([p1, p2], fill=color, width=width, joint="curve")
    d.line([p2, p3], fill=color, width=width, joint="curve")
    r = width // 2
    for p in (p1, p2, p3):
        d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=color)


def icon(size):
    S = size * SS
    im = grad(size).convert("RGBA")
    im.putalpha(squircle(size))

    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    # 挂环（小尺寸下会更细，避免糊成一团）
    ring_w = max(SS, int(S * 0.048))
    ring_h = int(S * 0.135)
    for cx in (S * 0.375, S * 0.625):
        d.rounded_rectangle([cx - ring_w / 2, S * 0.155, cx + ring_w / 2, S * 0.155 + ring_h],
                            radius=ring_w / 2, fill=WHITE)

    # 白色日历页
    px0, py0, px1, py1 = S * 0.185, S * 0.225, S * 0.815, S * 0.825
    rad = int(S * 0.072)
    d.rounded_rectangle([px0, py0, px1, py1], radius=rad, fill=WHITE)

    # 顶部色条（只保留上方圆角）
    hb = S * 0.395
    d.rounded_rectangle([px0, py0, px1, py0 + (hb - py0) + rad], radius=rad, fill=ACCENT)
    d.rectangle([px0, hb - rad, px1, hb], fill=ACCENT)

    # 对勾
    draw_check(d, (S * 0.295, S * 0.475, S * 0.705, S * 0.755), ACCENT, max(SS, int(S * 0.082)))

    layer = layer.resize((size, size), Image.LANCZOS)
    return Image.alpha_composite(im, layer)


sizes = [16, 20, 24, 32, 40, 48, 64, 96, 128, 256]
imgs = [icon(s) for s in sizes]
ico = os.path.join(ASSETS, "app.ico")
imgs[-1].save(ico, format="ICO", sizes=[(s, s) for s in sizes], append_images=imgs[:-1])

# 预览：各尺寸并排 + 16px 放大
prev = Image.new("RGBA", (420, 150), (245, 246, 248, 255))
x = 14
for s in (128, 48, 32, 16):
    ic = icon(s)
    prev.alpha_composite(ic, (x, 76 - s // 2))
    x += s + 22
prev.alpha_composite(icon(16).resize((96, 96), Image.NEAREST), (x + 16, 27))
prev.save(os.path.join(ASSETS, "icon-preview.png"))

print("已生成", ico)
print("包含尺寸:", sizes)
# 逐尺寸检查是否真的写进去了
with Image.open(ico) as t:
    print("ICO 内尺寸:", sorted(t.ico.sizes()))
