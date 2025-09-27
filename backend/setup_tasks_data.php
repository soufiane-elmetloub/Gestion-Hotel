<?php
require_once 'config.php';

try {
    // Get database connection
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception("فشل الاتصال بقاعدة البيانات");
    }
    
    // First, ensure the tasks table exists (from the SQL file)
    $createTableSQL = "
    CREATE TABLE IF NOT EXISTS `tasks` (
      `id` int(11) NOT NULL AUTO_INCREMENT,
      `employee_id` int(11) NOT NULL,
      `title` varchar(255) NOT NULL,
      `description` text NOT NULL,
      `status` enum('pending','in_progress','done') DEFAULT 'pending',
      `created_at` timestamp NULL DEFAULT current_timestamp(),
      `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
      PRIMARY KEY (`id`),
      KEY `employee_id` (`employee_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
    ";
    
    $pdo->exec($createTableSQL);
    echo "✅ جدول المهام تم إنشاؤه بنجاح<br>";
    
    // Ensure employees exist first
    $employeesSQL = "
    INSERT IGNORE INTO employees (id, username, password, first_name, last_name, email, phone, assigned_section) VALUES
    (1, 'soufiane', ?, 'سفيان', 'الأحمد', 'soufiane@smarthotel.com', '0501234567', 'A'),
    (2, 'ahmed_r', ?, 'أحمد', 'الرشيد', 'ahmed@smarthotel.com', '0507654321', 'B'),
    (3, 'fatima_k', ?, 'فاطمة', 'الكريم', 'fatima@smarthotel.com', '0509876543', 'C'),
    (4, 'omar_s', ?, 'عمر', 'السعيد', 'omar@smarthotel.com', '0502468135', 'D')
    ";
    
    $stmt = $pdo->prepare($employeesSQL);
    $hashedPassword = password_hash('1234', PASSWORD_DEFAULT);
    $stmt->execute([$hashedPassword, $hashedPassword, $hashedPassword, $hashedPassword]);
    echo "✅ تم التأكد من وجود الموظفين<br>";
    
    // Clear existing test data
    $pdo->exec("DELETE FROM tasks WHERE employee_id IN (1, 2, 3, 4)");
    echo "✅ تم حذف البيانات التجريبية السابقة<br>";
    
    // Insert sample tasks for different employees
    $sampleTasks = [
        // Employee 1 (soufiane) - Mixed status tasks
        [1, 'فحص الحجوزات الصباحية', 'مراجعة حجوزات اليوم والتأكد من جاهزية الغرف للضيوف الجدد', 'done'],
        [1, 'تنظيف الغرف المحجوزة', 'تنظيف وتجهيز الغرف للضيوف الجدد وتغيير الملاءات', 'in_progress'],
        [1, 'معالجة طلبات الضيوف العاجلة', 'الرد على طلبات الضيوف العاجلة وحل المشاكل الفورية', 'pending'],
        [1, 'إعداد تقرير المساء', 'تحضير تقرير نهاية اليوم عن إشغال الغرف والإيرادات', 'pending'],
        
        // Employee 2 (ahmed_r) - More pending tasks
        [2, 'استقبال الضيوف الجدد', 'استقبال الضيوف وإنهاء إجراءات تسجيل الدخول', 'in_progress'],
        [2, 'فحص نظام الحجز', 'التأكد من عمل نظام الحجز بشكل صحيح ومعالجة أي مشاكل', 'pending'],
        [2, 'تحديث بيانات العملاء', 'تحديث وتنظيم بيانات العملاء في النظام', 'pending'],
        [2, 'التنسيق مع فريق التنظيف', 'التنسيق مع فريق التنظيف لضمان جاهزية الغرف', 'pending'],
        
        // Employee 3 (fatima_k) - Mostly completed tasks
        [3, 'مراجعة الفواتير', 'مراجعة فواتير الضيوف والتأكد من دقة الحسابات', 'done'],
        [3, 'تسوية الحسابات اليومية', 'تسوية حسابات اليوم وإعداد التقارير المالية', 'done'],
        [3, 'متابعة المدفوعات المعلقة', 'متابعة المدفوعات المعلقة من الضيوف', 'in_progress'],
        
        // Employee 4 (omar_s) - Various tasks
        [4, 'صيانة أنظمة الفندق', 'فحص وصيانة الأنظمة التقنية في الفندق', 'pending'],
        [4, 'تدريب الموظفين الجدد', 'تدريب الموظفين الجدد على استخدام النظام', 'in_progress'],
        [4, 'مراجعة سياسات الأمان', 'مراجعة وتحديث سياسات الأمان في الفندق', 'pending']
    ];
    
    $insertSQL = "INSERT INTO tasks (employee_id, title, description, status, created_at) VALUES (?, ?, ?, ?, NOW() - INTERVAL ? DAY)";
    $stmt = $pdo->prepare($insertSQL);
    
    foreach ($sampleTasks as $index => $task) {
        // Spread tasks over the last 7 days
        $daysAgo = $index % 7;
        $stmt->execute([$task[0], $task[1], $task[2], $task[3], $daysAgo]);
    }
    
    echo "✅ تم إدراج " . count($sampleTasks) . " مهمة تجريبية<br>";
    
    // Display summary
    $countSQL = "SELECT 
        employee_id,
        COUNT(*) as total,
        SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) as pending,
        SUM(CASE WHEN status = 'in_progress' THEN 1 ELSE 0 END) as in_progress,
        SUM(CASE WHEN status = 'done' THEN 1 ELSE 0 END) as done
        FROM tasks 
        WHERE employee_id IN (1, 2, 3, 4)
        GROUP BY employee_id";
    
    $result = $pdo->query($countSQL);
    
    echo "<br><h3>📊 ملخص المهام حسب الموظف:</h3>";
    echo "<table border='1' style='border-collapse: collapse; width: 100%;'>";
    echo "<tr><th>معرف الموظف</th><th>المجموع</th><th>في الانتظار</th><th>قيد التنفيذ</th><th>مكتملة</th></tr>";
    
    while ($row = $result->fetch(PDO::FETCH_ASSOC)) {
        echo "<tr>";
        echo "<td>{$row['employee_id']}</td>";
        echo "<td>{$row['total']}</td>";
        echo "<td style='color: orange;'>{$row['pending']}</td>";
        echo "<td style='color: blue;'>{$row['in_progress']}</td>";
        echo "<td style='color: green;'>{$row['done']}</td>";
        echo "</tr>";
    }
    echo "</table>";
    
    echo "<br><h3>✅ تم إعداد البيانات التجريبية بنجاح!</h3>";
    echo "<p>يمكنك الآن اختبار النظام باستخدام معرفات الموظفين: 1, 2, 3, 4</p>";
    
} catch (Exception $e) {
    echo "❌ خطأ في إعداد البيانات: " . $e->getMessage();
}
?>
