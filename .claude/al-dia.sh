#!/bin/sh
# Hook SessionStart de Claude Code: al empezar a trabajar avisa si tu copia está atrasada
# respecto a GitHub. Solo avisa, no obliga a nada. Sin red o fuera de un repo no dice nada.
git fetch -q origin 2>/dev/null || exit 0
detras=$(git rev-list --count HEAD..origin/main 2>/dev/null) || exit 0
[ "$detras" -gt 0 ] || exit 0
rama=$(git branch --show-current)
msg="⚠️ Aviso: tu copia está $detras commit(s) detrás de main (rama: $rama). Puedes trabajar igual; cuando quieras actualizar: git checkout main && git pull && git checkout $rama && git merge main"
printf '{"systemMessage":"%s","hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"%s. Menciónaselo a la persona al empezar, una sola vez; no la obligues a actualizar."}}\n' "$msg" "$msg"
