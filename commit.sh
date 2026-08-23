#!/data/data/com.termux/files/usr/bin/bash
# Degisiklikleri commit eder ve push lar.
# Kullanim:  bash commit.sh
# Mesaj asagida MSG satirinda tutulur; her duzeltmede guncellenir.
set -e

DIR="$HOME/hillclimb-patches"
MSG="fix: replace returnEarly with inline return-void instructions"

cd "$DIR"

command -v git >/dev/null || { echo "git yok: pkg install git -y"; exit 1; }

# Commit kimligi yoksa ayarla
git config user.name  >/dev/null 2>&1 || git config user.name  "legendsciber"
git config user.email >/dev/null 2>&1 || git config user.email "legendsciber@users.noreply.github.com"

git add -A

if git diff --cached --quiet; then
    echo "Commitlenecek degisiklik yok."
else
    git commit -m "$MSG"
fi

git push
echo ""
echo "Push tamamlandi. Workflow durumu:"
echo "https://github.com/legendsciber/morphe-patches/actions"
