#!/usr/bin/env python3
"""Recalcula las barras de progreso del README a partir de las casillas marcadas.

Cada `### ` dentro de TAREAS es una fila. Cada casilla `- [ ] 💻` cuenta como código
y `- [ ] 📄` como documentación. Marca `[x]` y corre:

    python3 scripts/progreso.py           # actualiza README.md
    python3 scripts/progreso.py --test    # autoprueba
"""
import re
import sys
from pathlib import Path

CAT = {"💻": "code", "📄": "docs"}
P_INI, P_FIN = "<!-- PROGRESO:INICIO -->", "<!-- PROGRESO:FIN -->"
T_INI, T_FIN = "<!-- TAREAS:INICIO -->", "<!-- TAREAS:FIN -->"


def pct(done, total):
    return round(100 * done / total) if total else 0


def bar(done, total, width=10):
    if not total:
        return "—"
    n = round(width * done / total)
    return f"`{'█' * n}{'░' * (width - n)}` {pct(done, total)}%"


def parse(text):
    region = text.split(T_INI, 1)[1].split(T_FIN, 1)[0]
    rows = []
    for line in region.splitlines():
        if line.startswith("### "):
            title, _, who = line[4:].partition("—")
            rows.append({"title": title.strip(), "who": who.strip() or "_sin asignar_",
                         "code": [0, 0], "docs": [0, 0]})
        m = re.match(r"\s*- \[([ xX])\] (💻|📄)", line)
        if m and rows:
            c = rows[-1][CAT[m[2]]]
            c[0] += m[1] != " "
            c[1] += 1
    return rows


def render(rows):
    tot = {k: [sum(r[k][i] for r in rows) for i in (0, 1)] for k in CAT.values()}
    done, total = tot["code"][0] + tot["docs"][0], tot["code"][1] + tot["docs"][1]
    out = [
        "",
        "",
        "| | Avance |",
        "|---|---|",
        f"| 🏁 **Total** | {bar(done, total, 20)} &nbsp; ({done}/{total} tareas) |",
        f"| 💻 Código | {bar(*tot['code'], 20)} |",
        f"| 📄 Documentación | {bar(*tot['docs'], 20)} |",
        "",
        "| Área | Responsable | 💻 Código | 📄 Docs | Total |",
        "|---|---|---|---|---|",
    ]
    for r in rows:
        d, t = r["code"][0] + r["docs"][0], r["code"][1] + r["docs"][1]
        check = " ✅" if t and d == t else ""
        out.append(f"| {r['title']} | {r['who']} | {bar(*r['code'])} | {bar(*r['docs'])} | {bar(d, t)}{check} |")
    return "\n".join(out) + "\n", pct(done, total)


def update(text):
    table, p = render(parse(text))
    head, rest = text.split(P_INI, 1)
    tail = rest.split(P_FIN, 1)[1]
    color = "brightgreen" if p == 100 else "yellow" if p >= 67 else "orange" if p >= 34 else "red"
    new = head + P_INI + table + P_FIN + tail
    return re.sub(r"progreso-\d+%25-\w+", f"progreso-{p}%25-{color}", new), p


def selftest():
    doc = (f"![](https://img.shields.io/badge/progreso-0%25-red)\n{P_INI}\nviejo\n{P_FIN}\n"
           f"{T_INI}\n### 01 · Uno — Ana\n- [x] 💻 a\n- [ ] 📄 b\n### 02 · Dos —\n- [X] 💻 c\n{T_FIN}\n")
    new, p = update(doc)
    assert p == 67, p
    assert "progreso-67%25-yellow" in new
    assert "viejo" not in new
    assert "| 01 · Uno | Ana |" in new and "_sin asignar_" in new
    assert "| 02 · Dos | _sin asignar_ | `██████████` 100% | — | `██████████` 100% ✅ |" in new
    assert update(new)[0] == new  # idempotente
    print("ok")


if __name__ == "__main__":
    if sys.argv[1:] == ["--test"]:
        selftest()
        sys.exit()
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "README.md")
    old = path.read_text(encoding="utf-8")
    new, p = update(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
    print(f"Progreso total: {p}%" + ("" if new != old else " (sin cambios)"))
