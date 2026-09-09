# ZTE Smart Manager

تطبيق أندرويد عربي مفتوح المصدر لإدارة وتحسين راوترات ZTE، مع أولوية لدعم **MC801A** ثم التوسع لباقي الموديلات عبر ملفات تعريف قدرات مستقلة لكل موديل وFirmware.

## الأهداف

- قراءة دقيقة ومفهومة لحالة الشبكة: RSRP / RSRQ / SINR / PCI / EARFCN / Cell ID / LTE & NR bands.
- LTE/5G Band Lock مع تحقق فعلي بعد التطبيق، وليس الاكتفاء بنجاح طلب HTTP.
- Carrier Aggregation: عرض PCell/SCells وحالة الدمج الفعلية.
- Cell Lock للموديلات التي تدعمه.
- Smart Mode لاختيار أفضل Band/CA/Cell حسب السرعة، Ping، Jitter، Packet Loss، جودة الإشارة والثبات.
- Placement Assistant لمساعدة المستخدم على إيجاد أفضل مكان واتجاه للراوتر لحظيًا، مع تقييم رقمي وصوت/اهتزاز.
- Compatibility Engine لاكتشاف قدرات كل موديل/Firmware بدل افتراض أن جميع أجهزة ZTE تستخدم نفس الأوامر.
- واجهة عربية RTL بسيطة، مع وضع خبير اختياري.
- زر استعادة للوضع التلقائي وحفظ Snapshot قبل أي تغيير حساس.

## المعمارية المستهدفة

```text
UI (Jetpack Compose)
        ↓
Domain / Use Cases
        ↓
Router Repository
        ↓
ZTE Protocol Engine
        ↓
Model & Firmware Profiles
        ↓
HTTP Client
        ↓
ZTE Router
```

## المرحلة الأولى

1. تأسيس Android/Kotlin/Compose.
2. اكتشاف الراوتر وتسجيل الدخول الآمن محليًا.
3. MC801A capability profile.
4. قراءة الإشارة والخلايا والترددات.
5. LTE/NR Band Lock مع read-back verification.
6. CA status & PCell/SCell parsing.
7. Smart Mode v1.
8. Placement Assistant v1.

> المشروع يُكتب من الصفر. تطبيق ZManager القديم يُستخدم فقط كمرجع لفهم بروتوكول الاتصال، ولا يُنسخ منه كود أو واجهة.
