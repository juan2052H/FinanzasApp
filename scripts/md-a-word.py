"""Convierte un documento Markdown sencillo del proyecto a Word (.docx).

Soporta: titulos #, parrafos, listas (- y 1.), tablas, bloques de codigo, **negrita**, *cursiva*, `codigo` y enlaces.
Uso: python scripts/md-a-word.py docs/REQUISITOS_E_INSTALACION.md docs/REQUISITOS_E_INSTALACION.docx
Requiere: pip install python-docx
"""
import os
import re
import sys

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Inches
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Pt, RGBColor

AZUL = RGBColor(0x1A, 0x26, 0x45)
CODIGO = re.compile(r"(`[^`]+`)")
INLINE = re.compile(r"(\*[^*\s][^*]*\*|\[[^\]]+\]\([^)]+\)|<https?://[^>]+>)")


def sombrear(celda, color_hex):
    props = celda._tc.get_or_add_tcPr()
    sombra = OxmlElement("w:shd")
    sombra.set(qn("w:val"), "clear")
    sombra.set(qn("w:color"), "auto")
    sombra.set(qn("w:fill"), color_hex)
    props.append(sombra)


def agregar_texto(parrafo, texto, negrita=False):
    # El codigo `...` se separa primero: su contenido (p. ej. la clave Prueba2026*) nunca es formato.
    # '**' abre/cierra negrita aunque envuelva codigo, como en **`Prueba2026*`**.
    en_negrita = False
    for segmento in CODIGO.split(texto):
        if not segmento:
            continue
        if segmento.startswith("`") and segmento.endswith("`"):
            run = parrafo.add_run(segmento[1:-1])
            run.font.name = "Consolas"
            run.font.size = Pt(9.5)
            run.bold = negrita or en_negrita
            continue
        for trozo in re.split(r"(\*\*)", segmento):
            if trozo == "**":
                en_negrita = not en_negrita
                continue
            for parte in INLINE.split(trozo):
                if not parte:
                    continue
                if len(parte) > 2 and parte.startswith("*") and parte.endswith("*"):
                    run = parrafo.add_run(parte[1:-1])
                    run.italic = True
                elif parte.startswith("[") and "](" in parte:
                    run = parrafo.add_run(parte[1:parte.index("](")])
                elif parte.startswith("<http"):
                    run = parrafo.add_run(parte[1:-1])
                    run.font.color.rgb = RGBColor(0x1A, 0x73, 0xE8)
                else:
                    run = parrafo.add_run(parte)
                run.bold = negrita or en_negrita


def agregar_tabla(doc, filas):
    encabezado, *cuerpo = filas
    tabla = doc.add_table(rows=1, cols=len(encabezado))
    tabla.style = "Table Grid"
    tabla.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, texto in enumerate(encabezado):
        celda = tabla.rows[0].cells[i]
        celda.text = ""
        agregar_texto(celda.paragraphs[0], texto, negrita=True)
        sombrear(celda, "DCE6F2")
    for fila in cuerpo:
        celdas = tabla.add_row().cells
        for i, texto in enumerate(fila[:len(encabezado)]):
            celdas[i].text = ""
            agregar_texto(celdas[i].paragraphs[0], texto)
    for fila in tabla.rows:
        for celda in fila.cells:
            for p in celda.paragraphs:
                for run in p.runs:
                    run.font.size = Pt(9.5)
    doc.add_paragraph()


def agregar_codigo(doc, lineas):
    tabla = doc.add_table(rows=1, cols=1)
    celda = tabla.rows[0].cells[0]
    sombrear(celda, "F2F4F7")
    celda.text = ""
    p = celda.paragraphs[0]
    run = p.add_run("\n".join(lineas))
    run.font.name = "Consolas"
    run.font.size = Pt(9.5)
    doc.add_paragraph()


def convertir(origen, destino):
    doc = Document()
    estilo = doc.styles["Normal"]
    estilo.font.name = "Calibri"
    estilo.font.size = Pt(11)
    for nivel in (1, 2, 3):
        doc.styles[f"Heading {nivel}"].font.color.rgb = AZUL

    lineas = open(origen, encoding="utf-8").read().splitlines()
    hay_titulo = False
    hay_niveles_1 = sum(1 for linea in lineas if re.match(r"^# ", linea)) > 1
    i = 0
    while i < len(lineas):
        linea = lineas[i].rstrip()
        if linea.startswith("```"):
            bloque = []
            i += 1
            while i < len(lineas) and not lineas[i].startswith("```"):
                bloque.append(lineas[i])
                i += 1
            agregar_codigo(doc, bloque)
        elif linea.startswith("|"):
            filas = []
            while i < len(lineas) and lineas[i].startswith("|"):
                celdas = [c.strip() for c in lineas[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-{3,}:?", c) for c in celdas):
                    filas.append(celdas)
                i += 1
            agregar_tabla(doc, filas)
            continue
        elif linea.startswith("> "):
            avisos = []
            while i < len(lineas) and lineas[i].startswith("> "):
                avisos.append(lineas[i][2:])
                i += 1
            tabla = doc.add_table(rows=1, cols=1)
            celda = tabla.rows[0].cells[0]
            sombrear(celda, "FFF4CE")
            celda.text = ""
            for n, aviso in enumerate(avisos):
                parrafo = celda.paragraphs[0] if n == 0 else celda.add_paragraph()
                agregar_texto(parrafo, aviso)
            doc.add_paragraph()
            continue
        elif re.match(r"^!\[[^\]]*\]\([^)]+\)$", linea.strip()):
            alt, ruta = re.match(r"^!\[([^\]]*)\]\(([^)]+)\)$", linea.strip()).groups()
            imagen = os.path.join(os.path.dirname(os.path.abspath(origen)), ruta)
            doc.add_picture(imagen, width=Inches(6.2))
            doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER
            pie = doc.add_paragraph()
            pie.alignment = WD_ALIGN_PARAGRAPH.CENTER
            run = pie.add_run(alt)
            run.italic = True
            run.font.size = Pt(9)
        elif linea.startswith("#"):
            nivel = len(linea) - len(linea.lstrip("#"))
            texto = linea[nivel:].strip()
            if nivel == 1 and not hay_titulo:
                agregar_texto(doc.add_heading(level=0), texto)
                hay_titulo = True
            elif hay_niveles_1:
                # El documento usa '#' para secciones (Parte 1, Parte 2...): '#'->H1, '##'->H2, '###'->H3.
                agregar_texto(doc.add_heading(level=min(nivel, 3)), texto)
            else:
                agregar_texto(doc.add_heading(level=min(nivel - 1, 3)), texto)
        elif re.match(r"^\s*- ", linea):
            agregar_texto(doc.add_paragraph(style="List Bullet"), linea.split("- ", 1)[1])
        elif re.match(r"^\d+\. ", linea):
            # Se conserva el numero escrito: el estilo "List Number" de Word continua la numeracion
            # entre secciones (30, 31...) en lugar de reiniciar en 1 en cada lista.
            numero, contenido = linea.split(". ", 1)
            parrafo = doc.add_paragraph()
            parrafo.paragraph_format.left_indent = Inches(0.35)
            parrafo.paragraph_format.first_line_indent = Inches(-0.3)
            parrafo.paragraph_format.space_after = Pt(4)
            parrafo.add_run(f"{numero}.\t")
            parrafo.paragraph_format.tab_stops.add_tab_stop(Inches(0.35))
            agregar_texto(parrafo, contenido)
        elif linea.strip() in ("", "---"):
            pass
        else:
            agregar_texto(doc.add_paragraph(), linea)
        i += 1
    doc.save(destino)
    print(f"Word generado: {destino}")


if __name__ == "__main__":
    convertir(sys.argv[1], sys.argv[2])
