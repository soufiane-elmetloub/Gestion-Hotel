# دليل استخدام نظام الإشعارات

## نظرة عامة
تم إنشاء نظام إشعارات متكامل للتطبيق يتضمن:

### المكونات الرئيسية:
1. **صفحة الإشعارات** - عرض جميع الإشعارات
2. **أيقونة الإشعارات** - في الشريط العلوي مع عداد
3. **أيقونات مخصصة** - لكل نوع من الإشعارات
4. **حالة فارغة** - عند عدم وجود إشعارات

### الملفات المنشأة:

#### 1. Java Files:
- `Notification.java` - نموذج بيانات الإشعار
- `NotificationsActivity.java` - صفحة عرض الإشعارات
- `NotificationsAdapter.java` - محول عرض الإشعارات
- `MainActivity.java` - النشاط الرئيسي مع نظام الإشعارات

#### 2. Layout Files:
- `activity_notifications.xml` - تصميم صفحة الإشعارات
- `item_notification.xml` - تصميم عنصر الإشعار

#### 3. Drawables:
- `ic_notifications_active.xml` - أيقونة الإشعارات النشطة
- `ic_notifications_empty.xml` - أيقونة لا توجد إشعارات
- `ic_booking.xml` - أيقونة الحجز
- `ic_checkout.xml` - أيقونة المغادرة
- `ic_service.xml` - أيقونة الخدمة
- `badge_background.xml` - خلفية العداد
- `notification_icon_background.xml` - خلفية أيقونة الإشعار

### كيفية الاستخدام:

#### 1. عرض الإشعارات:
```java
// فتح صفحة الإشعارات
notificationIcon.setOnClickListener(v -> {
    Intent intent = new Intent(this, NotificationsActivity.class);
    startActivity(intent);
});
```

#### 2. إضافة إشعار جديد:
```java
// في MainActivity
addNotification("حجز جديد: غرفة 205");
```

#### 3. مسح الإشعارات:
```java
clearNotifications();
```

### أنواع الإشعارات المدعومة:
1. **حجز جديد** - باللون الأخضر
2. **تسجيل مغادرة** - باللون البرتقالي
3. **طلب خدمة** - باللون الأزرق
4. **إشعار عام** - باللون الرمادي

### مميزات النظام:
- ✅ دعم اللغة العربية الكامل
- ✅ تصميم Material Design حديث
- ✅ عداد الإشعارات مع عرض العدد
- ✅ عرض حالة فارغة عند عدم وجود إشعارات
- ✅ أيقونات مخصصة لكل نوع من الإشعارات
- ✅ دعم RTL (من اليمين لليسار)
- ✅ تفاعل كامل مع المستخدم

### تخصيص الإشعارات:

#### تغيير الألوان:
يمكن تغيير ألوان الإشعارات من ملف `colors.xml`

#### إضافة أنواع جديدة:
1. أضف أيقونة جديدة في `drawable`
2. عدل `NotificationsAdapter.getIconForType()`
3. أضف اللون المناسب في `colors.xml`

#### تغيير النصوص:
يمكن تغيير جميع النصوص من ملف `strings.xml`

### مثال على الإشعار:
```java
Notification notification = new Notification(
    1, 
    "حجز جديد", 
    "تم حجز غرفة 205 من قبل السيد أحمد", 
    "منذ 5 دقائق", 
    "booking"
);
```

### التوافق:
- Android API 21+
- دعم كامل للغة العربية
- متوافق مع جميع أحجام الشاشات
