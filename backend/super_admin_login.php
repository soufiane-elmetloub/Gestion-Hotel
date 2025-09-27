<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Database configuration
$host = 'localhost';
$dbname = 'smart-hotel';
$username_db = 'Unknown';
$password_db = '5L7Fqp9GG-@r7trj';

// Start session
session_start();

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// Validate input
if (!isset($input['username']) || !isset($input['password'])) {
    echo json_encode([
        'success' => false,
        'message' => 'اسم المستخدم وكلمة المرور مطلوبان'
    ]);
    exit;
}

$username = trim($input['username']);
$password = trim($input['password']);

// Validate credentials
if (empty($username) || empty($password)) {
    echo json_encode([
        'success' => false,
        'message' => 'يرجى إدخال اسم المستخدم وكلمة المرور'
    ]);
    exit;
}

try {
    // Create database connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username_db, $password_db);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Check if super_admin table exists, if not create it
    $checkTable = $pdo->query("SHOW TABLES LIKE 'super_admin'");
    if ($checkTable->rowCount() == 0) {
        // Create table and insert default admin
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
        
        // Insert default super admin (password is plain text for now as per SQL file)
        $insertAdmin = "INSERT INTO `super_admin` (`username`, `password_hash`, `is_active`) VALUES ('superadmin', 'super-1234', 1)";
        $pdo->exec($insertAdmin);
    }
    
    // Query to find user
    $stmt = $pdo->prepare("SELECT * FROM super_admin WHERE username = ? AND is_active = 1");
    $stmt->execute([$username]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($user) {
        // For now, we're using plain text password as per the SQL file
        // In production, you should use password_hash() and password_verify()
        if ($password === $user['password_hash']) {
            // Update last login
            $updateLogin = $pdo->prepare("UPDATE super_admin SET last_login = NOW() WHERE id = ?");
            $updateLogin->execute([$user['id']]);
            
            // Set session variables
            $_SESSION['super_admin_id'] = $user['id'];
            $_SESSION['super_admin_username'] = $user['username'];
            $_SESSION['super_admin_logged_in'] = true;
            
            echo json_encode([
                'success' => true,
                'message' => 'تم تسجيل الدخول بنجاح',
                'user' => [
                    'id' => $user['id'],
                    'username' => $user['username'],
                    'last_login' => $user['last_login']
                ]
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'message' => 'كلمة المرور غير صحيحة'
            ]);
        }
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'اسم المستخدم غير موجود أو غير مفعل'
        ]);
    }
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'خطأ في قاعدة البيانات: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    error_log("General error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'حدث خطأ غير متوقع'
    ]);
}
?>
