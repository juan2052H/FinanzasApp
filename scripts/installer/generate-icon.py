"""Genera scripts/installer/FinanzasApp.ico (icono de la aplicacion y del instalador).

Uso: python scripts/installer/generate-icon.py      (requiere: pip install pillow)
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

AZUL = (26, 115, 232)
AZUL_OSCURO = (27, 38, 69)
VERDE = (52, 168, 83)
BLANCO = (255, 255, 255)


def dibujar(size: int) -> Image.Image:
    escala = 4  # dibujar grande y reducir para bordes suaves
    s = size * escala
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    radio = int(s * 0.22)
    d.rounded_rectangle((0, 0, s - 1, s - 1), radius=radio, fill=AZUL_OSCURO)

    # barras ascendentes (finanzas que crecen)
    base = int(s * 0.78)
    ancho = int(s * 0.14)
    alturas = (0.22, 0.36, 0.52)
    x = int(s * 0.17)
    for i, h in enumerate(alturas):
        color = VERDE if i == len(alturas) - 1 else AZUL
        d.rounded_rectangle((x, base - int(s * h), x + ancho, base), radius=int(ancho * 0.25), fill=color)
        x += int(ancho * 1.45)

    # moneda
    cx, cy, r = int(s * 0.70), int(s * 0.30), int(s * 0.17)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=BLANCO)
    d.ellipse((cx - int(r * 0.78), cy - int(r * 0.78), cx + int(r * 0.78), cy + int(r * 0.78)), outline=VERDE, width=max(1, int(r * 0.12)))
    fuente = cargar_fuente(int(r * 1.25))
    d.text((cx, cy), "$", fill=VERDE, font=fuente, anchor="mm")
    return img.resize((size, size), Image.LANCZOS)


def cargar_fuente(tamano: int) -> ImageFont.ImageFont:
    for nombre in ("arialbd.ttf", "segoeuib.ttf", "DejaVuSans-Bold.ttf"):
        try:
            return ImageFont.truetype(nombre, tamano)
        except OSError:
            continue
    return ImageFont.load_default(tamano)


if __name__ == "__main__":
    destino = Path(__file__).with_name("FinanzasApp.ico")
    tamanos = [16, 24, 32, 48, 64, 128, 256]
    dibujar(256).save(destino, sizes=[(t, t) for t in tamanos], append_images=[dibujar(t) for t in tamanos[:-1]])
    dibujar(256).save(destino.with_suffix(".png"))
    recursos = Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "icons"
    recursos.mkdir(parents=True, exist_ok=True)
    for t in (16, 32, 48, 256):
        dibujar(t).save(recursos / f"finanzasapp-{t}.png")
    print(f"Icono generado: {destino} (+ iconos de ventana en {recursos})")
