# بناء NexShare من GitHub (الطريقة الصحيحة)

لا أستطيع الدخول إلى حسابك. ابنِ الـ APK على GitHub بنفسك بهذه الخطوات. البناء يتم على سيرفرات GitHub بـ Gradle الرسمي، وهذا ما يقبله مثبّت أندرويد.

## الخطوات

1. ادخل إلى github.com وسجّل دخولك.
2. New repository
   - الاسم: `NexShare`
   - اتركه فارغاً (بدون README)
   - Create repository
3. ارفع مجلد المشروع كاملاً إلى المستودع:
   - من الموقع: Add file → Upload files
   - أو من الكمبيوتر:

```bash
cd NexShare
git init
git add .
git commit -m "NexShare 1.0.1"
git branch -M main
git remote add origin https://github.com/YOUR_USER/NexShare.git
git push -u origin main
```

4. افتح تبويب **Actions**
5. اختر **Build NexShare APK** → **Run workflow**
6. انتظر حتى تصبح النقطة خضراء
7. حمّل **NexShare-APK** وفك الضغط ثم ثبّت `NexShare-1.0.1.apk`

إذا فشل الـ Action افتح السجل وأرسله لي.
