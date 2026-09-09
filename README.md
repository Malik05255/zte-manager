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

## دقة تفسير الشبكة

في عائلة ZTE القديمة التي تستخدم `goform` لا يكفي البحث عن كلمة `5G` داخل `network_type`:

- `ENDC` / `EN-DC`: اتصال **5G NSA فعلي** مع مرساة LTE.
- `LTE-NSA`: الموقع/الجهاز يدعم NSA لكن حامل NR ليس نشطًا في تلك اللحظة، والبيانات تمر عبر مرساة LTE.
- `SA`: اتصال **5G Standalone**.
- `LTE`: اتصال 4G بدون حامل NR نشط.

التطبيق يجمع `network_type` مع حقول NR/CA الفعلية ولا يعتمد على مؤشر واحد فقط. كما أن بعض حقول ZTE مثل `lte_pci` و`nr5g_pci` و`cell_id` قد تُرجع بصيغة hexadecimal في واجهات goform القديمة، لذلك تتم معالجتها وفق صيغة الـFirmware بدل افتراض decimal دائمًا.

## مراجع التوافق والبروتوكول

المشروع يُكتب من الصفر، لكن يتم تدقيق سلوك ZTE وحقول القراءة مقابل مشاريع مفتوحة المصدر وخبرات عملية، أهمها:

- `tpoechtrager/ZTE-Web-Script` — مرجع قوي لتفسير ENDC/LTE-NSA وقراءة LTE/NR/CA في أجهزة ZTE المختلفة.
- `Kajkac/ZTE-MC-Home-assistant-repo` — مجموعة واسعة من حقول telemetry لأجهزة MC801/MC888/MC889 وغيرها.
- `PlayFaster/ha-zte-router-5g-monitor` — discovery واسع لأسماء الحقول واختلافات Firmware بين أجهزة ZTE.
- `nicjac/python-zte-mc801a` — مرجع مستقل لمصادقة MC801A وبعض أوامر goform.
- ZManager v1.1.1 — يستخدم فقط كمرجع interoperability لفهم سلوك بعض أجهزة MC801A/MC801A1؛ لا يتم نسخ كوده أو واجهته أو أصوله.

أي سلوك غير مؤكد يبقى داخل Profile خاص بالموديل/Firmware ولا يتم تعميمه على بقية الأجهزة حتى يتم التحقق منه.

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
