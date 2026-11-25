#!/bin/sh
# Hook SessionStart de Claude Code: avisa si tu copia está atrasada respecto a GitHub.
# Sin red o fuera de un repo no dice nada.
git fetch -q origin 2>/dev/null || exit 0
detras=$(git rev-list --count HEAD..origin/main 2>/dev/null) || exit 0
[ "$detras" -gt 0 ] || exit 0
rama=$(git branch --show-current)
msg="⚠️ Tu copia está $detras commit(s) detrás de origin/main (rama: $rama). Antes de trabajar: git checkout main && git pull, y luego git checkout $rama && git merge main."
printf '{"systemMessage":"%s","hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"%s Avísale a la persona y ofrécete a actualizar antes de hacer cualquier otra cosa."}}\n' "$msg" "$msg"
