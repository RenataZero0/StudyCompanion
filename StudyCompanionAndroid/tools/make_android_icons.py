# 生成 Android 启动图标（与桌面版同一个设计：蓝色渐变圆角方 + 白色日历页 + 对勾）
import os, sys
from PIL import Image, ImageDraw
sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "res")

TOP = (94, 146, 245)
BOT = (43, 86, 219)
WHITE = (255, 255, 255)
ACCENT = (47, 96, 224)
SS = 6

# Android 各密度下 launcher icon 的边长
DENSITIES = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
}


def grad(size):
    im = Image.new("RGB", (1, size))
    d = ImageDraw.Draw(im)
    for y in range(size):
        t = y / max(1, size - 1)
        d.point((0, y), tuple(int(TOP[i] + (BOT[i] - TOP[i]) * t) for i in range(3)))
    return im.resize((size, size), Image.BILINEAR)


def squircle(size, ratio=0.235):
    S = size * SS
    m = Image.new("L", (S, S), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * ratio), fill=255)
    return m.resize((size, size), Image.LANCZOS)


def check(d, box, color, width):
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


def icon(size, square=False):
    """square=True 时铺满整块（Android 5-7 的方形图标），否则圆角"""
    S = size * SS
    im = grad(size).convert("RGBA")
    if square:
        im.putalpha(Image.new("L", (size, size), 255))
    else:
        im.putalpha(squircle(size))

    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    ring_w = max(SS, int(S * 0.048))
    ring_h = int(S * 0.135)
    for cx in (S * 0.375, S * 0.625):
        d.rounded_rectangle([cx - ring_w / 2, S * 0.155, cx + ring_w / 2, S * 0.155 + ring_h],
                            radius=ring_w / 2, fill=WHITE)

    px0, py0, px1, py1 = S * 0.185, S * 0.225, S * 0.815, S * 0.825
    rad = int(S * 0.072)
    d.rounded_rectangle([px0, py0, px1, py1], radius=rad, fill=WHITE)
    hb = S * 0.395
    d.rounded_rectangle([px0, py0, px1, py0 + (hb - py0) + rad], radius=rad, fill=ACCENT)
    d.rectangle([px0, hb - rad, px1, hb], fill=ACCENT)
    check(d, (S * 0.295, S * 0.475, S * 0.705, S * 0.755), ACCENT, max(SS, int(S * 0.082)))

    return Image.alpha_composite(im, layer.resize((size, size), Image.LANCZOS))


total = 0
for dens, size in DENSITIES.items():
    d = os.path.join(RES, "mipmap-" + dens)
    os.makedirs(d, exist_ok=True)
    icon(size).save(os.path.join(d, "ic_launcher.png"))
    icon(size, square=True).save(os.path.join(d, "ic_launcher_square.png"))
    total += 2
    print(f"  mipmap-{dens}: ic_launcher.png ({size}px)")

# 应用商店/预览用的 512
os.makedirs(os.path.join(ROOT, "preview"), exist_ok=True)
icon(512).save(os.path.join(ROOT, "preview", "ic_launcher-512.png"))
print(f"已生成 {total} 个启动图标 + 512px 预览 -> {RES}")
