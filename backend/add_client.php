<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // Create database connection using Database class
        $database = new Database();
        $pdo = $database->getConnection();
        
        if (!$pdo) {
            throw new Exception('Database connection failed');
        }
        
        // Get POST data
        $first_name = trim($_POST['first_name'] ?? '');
        $last_name = trim($_POST['last_name'] ?? '');
        $phone = trim($_POST['phone'] ?? '');
        $national_id = trim($_POST['national_id'] ?? '');
        $email = trim($_POST['email'] ?? '');
        
        // Validate required fields
        if (empty($first_name) || empty($last_name) || empty($phone)) {
            echo json_encode([
                'success' => false,
                'message' => 'الاسم الأول والأخير ورقم الجوال مطلوبة'
            ]);
            exit;
        }
        
        // Check if phone already exists
        $stmt = $pdo->prepare("SELECT id FROM clients WHERE phone = ?");
        $stmt->execute([$phone]);
        if ($stmt->fetch()) {
            echo json_encode([
                'success' => false,
                'message' => 'رقم الجوال مسجل مسبقاً'
            ]);
            exit;
        }
        
        // Insert new client
        $stmt = $pdo->prepare("
            INSERT INTO clients (first_name, last_name, phone, national_id, email, created_at) 
            VALUES (?, ?, ?, ?, ?, NOW())
        ");
        
        $stmt->execute([$first_name, $last_name, $phone, $national_id, $email]);
        
        echo json_encode([
            'success' => true,
            'message' => 'تم إضافة العميل بنجاح',
            'client_id' => $pdo->lastInsertId()
        ]);
        
    } catch (PDOException $e) {
        echo json_encode([
            'success' => false,
            'message' => 'خطأ في قاعدة البيانات: ' . $e->getMessage()
        ]);
    }
} else {
    echo json_encode([
        'success' => false,
        'message' => 'طريقة الطلب غير مدعومة'
    ]);
}
?>
