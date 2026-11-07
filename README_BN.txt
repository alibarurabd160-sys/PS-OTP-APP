OTP STUDIO — GitHub Upload Package

এই ZIP-টি সরাসরি GitHub repository-র root-এ আপলোড করার জন্য সাজানো। ZIP extract করলে .github, app, build.gradle.kts, settings.gradle.kts ইত্যাদি একই root-এ থাকবে।

GitHub-এ আপলোড:
1) GitHub-এ New repository তৈরি করুন।
2) repository খুলে Add file > Upload files চাপুন।
3) এই ZIP-টি আগে ফোনে Extract করুন। Extract করা ফোল্ডারের ভেতরের সব ফাইল/ফোল্ডার (বিশেষ করে .github ফোল্ডারসহ) আপলোড করুন; ZIP ফাইলটিকে একা আপলোড করবেন না।
4) Commit changes চাপুন।
5) Actions ট্যাবে Build APK workflow চলবে। প্রথমবার workflow permission চাইলে Allow করুন।
6) Build সফল হলে workflow run খুলে Artifacts থেকে OtpApp-debug-apk ডাউনলোড করুন। Artifact ZIP extract করলে app-debug.apk পাবেন।

গুরুত্বপূর্ণ:
- .github ফোল্ডারটি লুকানো (hidden) হতে পারে। Upload করার সময় এটি বাদ দেবেন না।
- GitHub-এ ফোল্ডার কাঠামো অপরিবর্তিত রাখতে হবে।
- এটি Debug APK তৈরি করে; Play Store release signing করা নেই।
- প্রথমবার অ্যাপে নিজের API Base URL এবং API Key সেট করতে হবে। API key গোপন রাখুন।
- Floating window চালাতে Android-এর Display over other apps অনুমতি দিতে হবে।
- অ্যাপটি তৃতীয় পক্ষের অ্যাপ/ওয়েবসাইটের ফর্ম বা OTP ফিল্ড শনাক্ত করে স্বয়ংক্রিয়ভাবে পূরণ করে না।


রিসেন্ট ট্যাব থেকে কেটে দিলেও ব্যাকগ্রাউন্ডে চালু রাখা (সেটআপ):
১) অ্যাপে ▶ চেপে "অন্য অ্যাপের উপরে দেখান" অনুমতি দিন।
২) ব্যাটারির ডায়ালগ এলে "Allow / অনুমতি দিন" চাপুন (ব্যাটারি অপ্টিমাইজেশন থেকে বাদ)।
৩) Xiaomi/Redmi, Oppo/Realme, Vivo, Huawei ফোনে পরের বার ▶ চাপলে Autostart পেজ খুলবে — PS NUMBER চালু করুন।
৪) রিসেন্ট ট্যাবে অ্যাপ কার্ড ধরে রেখে "Lock" করে রাখুন (Xiaomi/Oppo/Vivo/Samsung-এ আছে)।
৫) নোটিফিকেশন অনুমতি দিন (Android 13+)।
- ফোন রিস্টার্ট করলে বা অ্যাপ আপডেট হলে, আপনি নিজে বন্ধ না করে থাকলে বাবল নিজে ফিরে আসবে।
- "বন্ধ করুন" (নোটিফিকেশন / ■ বাটন / ✕) চাপলে পুরোপুরি বন্ধ হয় এবং আর নিজে চালু হয় না।
