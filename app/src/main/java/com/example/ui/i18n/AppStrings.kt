package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

data class ScamShieldStrings(
    val code: String,
    val isRtl: Boolean,
    val appName: String,
    val tagline: String,
    val slogan: String,
    val navHome: String,
    val navHistory: String,
    val navAlerts: String,
    val navFamily: String,
    val navProfile: String,
    val greetingHello: String,
    val greetingSubtitle: String,
    val assistantTitle: String,
    val assistantDesc: String,
    val scanSomething: String,
    val whatToCheck: String,
    val checkMessageTitle: String,
    val checkMessageDesc: String,
    val checkWebsiteTitle: String,
    val checkWebsiteDesc: String,
    val checkNumberTitle: String,
    val checkNumberDesc: String,
    val scanScreenshotTitle: String,
    val scanScreenshotDesc: String,
    val recentActivity: String,
    val viewAll: String,
    val stayProtected: String,
    val communityAlerts: String,
    val learnCenterTitle: String,
    val analyzeMessageBtn: String,
    val checkWebsiteBtn: String,
    val checkNumberBtn: String,
    val scanScreenshotBtn: String,
    val signInWithGoogle: String,
    val welcomeTitle: String,
    val skip: String,
    val next: String,
    val getStarted: String
)

private val EnglishStrings = ScamShieldStrings(
    code = "en",
    isRtl = false,
    appName = "ScamShield AI",
    tagline = "Your Personal Digital Safety Companion.",
    slogan = "BEFORE YOU CLICK, PAY OR REPLY — CHECK IT WITH SCAMSHIELD.",
    navHome = "Home",
    navHistory = "History",
    navAlerts = "Alerts",
    navFamily = "Family",
    navProfile = "Profile",
    greetingHello = "Hello",
    greetingSubtitle = "Your digital safety matters. What would you like to check today?",
    assistantTitle = "Your Digital Safety Assistant",
    assistantDesc = "Check suspicious messages, websites and phone numbers before taking action.",
    scanSomething = "Scan Something",
    whatToCheck = "What would you like to check?",
    checkMessageTitle = "Check a Message",
    checkMessageDesc = "Analyze suspicious SMS, WhatsApp messages, emails and offers.",
    checkWebsiteTitle = "Check a Website",
    checkWebsiteDesc = "Find suspicious and potentially dangerous links.",
    checkNumberTitle = "Check a Number",
    checkNumberDesc = "Review available phone reputation and community reports.",
    scanScreenshotTitle = "Scan a Screenshot",
    scanScreenshotDesc = "Upload a screenshot and identify suspicious content.",
    recentActivity = "Recent Activity",
    viewAll = "View All",
    stayProtected = "Stay Protected",
    communityAlerts = "Community Alerts",
    learnCenterTitle = "Learn to Stay Safe",
    analyzeMessageBtn = "Analyze Message",
    checkWebsiteBtn = "Check Website",
    checkNumberBtn = "Check Phone Number",
    scanScreenshotBtn = "Scan Screenshot",
    signInWithGoogle = "Continue with Google",
    welcomeTitle = "Welcome to ScamShield AI",
    skip = "Skip",
    next = "Next",
    getStarted = "Get Started"
)

private val UrduStrings = ScamShieldStrings(
    code = "ur",
    isRtl = true,
    appName = "ScamShield AI",
    tagline = "آپ کا ذاتی ڈیجیٹل حفاظتی ساتھی",
    slogan = "کلک کرنے، ادائیگی یا جواب دینے سے پہلے — اسکیم شیلڈ سے چیک کریں۔",
    navHome = "ہوم",
    navHistory = "ہسٹری",
    navAlerts = "الرٹس",
    navFamily = "فیملی",
    navProfile = "پروفائل",
    greetingHello = "سلام",
    greetingSubtitle = "آپ کی ڈیجیٹل حفاظت اہم ہے۔ آج آپ کیا چیک کرنا چاہیں گے؟",
    assistantTitle = "آپ کا ڈیجیٹل حفاظتی معاون",
    assistantDesc = "کوئی بھی قدم اٹھانے سے پہلے مشکوک پیغامات، ویب سائٹس اور نمبر چیک کریں۔",
    scanSomething = "ابھی اسکین کریں",
    whatToCheck = "آپ کیا چیک کرنا چاہتے ہیں؟",
    checkMessageTitle = "پیغام چیک کریں",
    checkMessageDesc = "مشکوک ایس ایم ایس، واٹس ایپ، ای میل اور آفرز کی جانچ کریں۔",
    checkWebsiteTitle = "ویب سائٹ چیک کریں",
    checkWebsiteDesc = "مشکوک اور خطرناک لنکس کی شناخت کریں۔",
    checkNumberTitle = "فون نمبر چیک کریں",
    checkNumberDesc = "فون نمبر کی ساکھ اور کمیونٹی رپورٹس دیکھیں۔",
    scanScreenshotTitle = "اسکرین شاٹ اسکین کریں",
    scanScreenshotDesc = "اسکرین شاٹ اپ لوڈ کریں اور مشکوک مواد پہچانیں۔",
    recentActivity = "حالیہ سرگرمی",
    viewAll = "سب دیکھیں",
    stayProtected = "محفوظ رہیں",
    communityAlerts = "کمیونٹی الرٹس",
    learnCenterTitle = "محفوظ رہنا سیکھیں",
    analyzeMessageBtn = "پیغام کا تجزیہ کریں",
    checkWebsiteBtn = "ویب سائٹ چیک کریں",
    checkNumberBtn = "نمبر چیک کریں",
    scanScreenshotBtn = "اسکرین شاٹ اسکین کریں",
    signInWithGoogle = "Google کے ساتھ جاری رکھیں",
    welcomeTitle = "ScamShield AI میں خوش آمدید",
    skip = "چھوڑیں",
    next = "اگلا",
    getStarted = "شروع کریں"
)

private val HindiStrings = ScamShieldStrings(
    code = "hi",
    isRtl = false,
    appName = "ScamShield AI",
    tagline = "आपका व्यक्तिगत डिजिटल सुरक्षा साथी",
    slogan = "क्लिक करने, भुगतान या जवाब देने से पहले — SCAMSHIELD से जांचें।",
    navHome = "होम",
    navHistory = "इतिहास",
    navAlerts = "अलर्ट",
    navFamily = "परिवार",
    navProfile = "प्रोफ़ाइल",
    greetingHello = "नमस्ते",
    greetingSubtitle = "आपकी डिजिटल सुरक्षा महत्वपूर्ण है। आज आप क्या जांचना चाहेंगे?",
    assistantTitle = "आपका डिजिटल सुरक्षा सहायक",
    assistantDesc = "कोई भी कदम उठाने से पहले संदिग्ध संदेश, वेबसाइट और नंबर जांचें।",
    scanSomething = "स्कैन करें",
    whatToCheck = "आप क्या जांचना चाहेंगे?",
    checkMessageTitle = "संदेश जांचें",
    checkMessageDesc = "संदिग्ध SMS, WhatsApp संदेश, ईमेल और ऑफ़र का विश्लेषण करें।",
    checkWebsiteTitle = "वेबसाइट जांचें",
    checkWebsiteDesc = "संदिग्ध और खतरनाक लिंक की पहचान करें।",
    checkNumberTitle = "फ़ोन नंबर जांचें",
    checkNumberDesc = "फ़ोन नंबर की प्रतिष्ठा और समुदाय रिपोर्ट देखें।",
    scanScreenshotTitle = "स्क्रीनशॉट स्कैन करें",
    scanScreenshotDesc = "स्क्रीनशॉट अपलोड करें और संदिग्ध सामग्री पहचानें।",
    recentActivity = "हाल की गतिविधि",
    viewAll = "सभी देखें",
    stayProtected = "सुरक्षित रहें",
    communityAlerts = "समुदाय अलर्ट",
    learnCenterTitle = "सुरक्षित रहना सीखें",
    analyzeMessageBtn = "संदेश का विश्लेषण करें",
    checkWebsiteBtn = "वेबसाइट जांचें",
    checkNumberBtn = "फ़ोन नंबर जांचें",
    scanScreenshotBtn = "स्क्रीनशॉट स्कैन करें",
    signInWithGoogle = "Google के साथ जारी रखें",
    welcomeTitle = "ScamShield AI में आपका स्वागत है",
    skip = "छोड़ें",
    next = "अगला",
    getStarted = "शुरू करें"
)

private val ArabicStrings = ScamShieldStrings(
    code = "ar",
    isRtl = true,
    appName = "ScamShield AI",
    tagline = "رفيقك الشخصي للأمان الرقمي",
    slogan = "قبل أن تنقر أو تدفع أو ترد — افحصها عبر SCAMSHIELD.",
    navHome = "الرئيسية",
    navHistory = "السجل",
    navAlerts = "التنبيهات",
    navFamily = "العائلة",
    navProfile = "الحساب",
    greetingHello = "مرحباً",
    greetingSubtitle = "سلامتك الرقمية تهمنا. ماذا تود أن تفحص اليوم؟",
    assistantTitle = "مساعدك للأمان الرقمي",
    assistantDesc = "افحص الرسائل المشبوهة والمواقع وأرقام الهواتف قبل اتخاذ أي إجراء.",
    scanSomething = "افحص الآن",
    whatToCheck = "ماذا تريد أن تفحص؟",
    checkMessageTitle = "فحص رسالة",
    checkMessageDesc = "تحليل رسائل SMS وواتساب والبريد الإلكتروني المشبوهة.",
    checkWebsiteTitle = "فحص موقع ويب",
    checkWebsiteDesc = "اكتشف الروابط المشبوهة والضارة.",
    checkNumberTitle = "فحص رقم هاتف",
    checkNumberDesc = "مراجعة سمعة الرقم وتقارير المجتمع المتاحة.",
    scanScreenshotTitle = "مسح لقطة شاشة",
    scanScreenshotDesc = "ارفع لقطة شاشة لاكتشاف المحتوى المشبوه.",
    recentActivity = "النشاط الأخير",
    viewAll = "عرض الكل",
    stayProtected = "ابقَ آمناً",
    communityAlerts = "تنبيهات المجتمع",
    learnCenterTitle = "تعلّم كيف تبقى آمناً",
    analyzeMessageBtn = "تحليل الرسالة",
    checkWebsiteBtn = "فحص الموقع",
    checkNumberBtn = "فحص الرقم",
    scanScreenshotBtn = "مسح لقطة الشاشة",
    signInWithGoogle = "المتابعة باستخدام Google",
    welcomeTitle = "مرحباً بك في ScamShield AI",
    skip = "تخطي",
    next = "التالي",
    getStarted = "ابدأ الآن"
)

fun stringsForLanguage(code: String): ScamShieldStrings = when (code.lowercase()) {
    "ur" -> UrduStrings
    "hi" -> HindiStrings
    "ar" -> ArabicStrings
    else -> EnglishStrings
}

val LocalStrings = staticCompositionLocalOf { EnglishStrings }

@Composable
fun ProvideAppLanguage(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val strings = stringsForLanguage(languageCode)
    val layoutDirection = if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}
