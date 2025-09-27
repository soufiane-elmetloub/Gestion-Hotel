<?php
// Setup script to import super_admin table and data
// Database configuration
$host = 'localhost';
$dbname = 'smart-hotel';
$username_db = 'Unknown';
$password_db = '5L7Fqp9GG-@r7trj';

try {
    // Create database connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username_db, $password_db);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Drop table if exists
    $pdo->exec("DROP TABLE IF EXISTS `super_admin`");
    
    // Create super_admin table
    $createTable = "
        CREATE TABLE `super_admin` (
            `id` int(11) NOT NULL AUTO_INCREMENT,
            `username` varchar(100) NOT NULL,
            `password_hash` varchar(255) NOT NULL,
            `created_at` timestamp NULL DEFAULT current_timestamp(),
            `last_login` timestamp NULL DEFAULT NULL,
            `is_active` tinyint(1) DEFAULT 1,
            PRIMARY KEY (`id`),
            UNIQUE KEY `username` (`username`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
    ";
    
    $pdo->exec($createTable);
    
    // Insert super admin data
    $insertData = "INSERT INTO `super_admin` (`id`, `username`, `password_hash`, `created_at`, `last_login`, `is_active`) VALUES (1, 'superadmin', 'super-1234', '2025-09-08 13:14:02', NULL, 1)";
    
    $pdo->exec($insertData);
    
    echo "<!DOCTYPE html>
    <html lang='ar' dir='rtl'>
    <head>
        <meta charset='UTF-8'>
        <title>إعداد قاعدة البيانات</title>
        <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>
        <link href='https://fonts.googleapis.com/css2?family=Cairo:wght@400;600&display=swap' rel='stylesheet'>
        <style>body{font-family:'Cairo',sans-serif;background:#f8f9fc;}</style>
    </head>
    <body>
        <div class='container mt-5'>
            <div class='alert alert-success text-center'>
                <h3><i class='fas fa-check-circle'></i> تم إعداد قاعدة البيانات بنجاح!</h3>
                <p>تم إنشاء جدول super_admin وإضافة المدير العام.</p>
                <hr>
                <p><strong>بيانات تسجيل الدخول:</strong></p>
                <p>اسم المستخدم: <code>superadmin</code></p>
                <p>كلمة المرور: <code>super-1234</code></p>
                <hr>
                <a href='../Web/login.html' class='btn btn-primary'>الذهاب لصفحة تسجيل الدخول</a>
            </div>
        </div>
    </body>
    </html>";
    
} catch (PDOException $e) {
    echo "<!DOCTYPE html>
    <html lang='ar' dir='rtl'>
    <head>
        <meta charset='UTF-8'>
        <title>خطأ في الإعداد</title>
        <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>
        <link href='https://fonts.googleapis.com/css2?family=Cairo:wght@400;600&display=swap' rel='stylesheet'>
        <style>body{font-family:'Cairo',sans-serif;background:#f8f9fc;}</style>
    </head>
    <body>
        <div class='container mt-5'>
            <div class='alert alert-danger text-center'>
                <h3><i class='fas fa-exclamation-triangle'></i> خطأ في إعداد قاعدة البيانات</h3>
                <p>" . $e->getMessage() . "</p>
                <p>تأكد من تشغيل XAMPP وإعدادات قاعدة البيانات في config.php</p>
            </div>
        </div>
    </body>
    </html>";
}
?>
