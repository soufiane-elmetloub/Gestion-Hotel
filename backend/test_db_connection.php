<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
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

try {
    // Test database connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username_db, $password_db);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Test query
    $stmt = $pdo->query("SELECT VERSION() as version, DATABASE() as current_db, USER() as current_user");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'message' => 'تم الاتصال بقاعدة البيانات بنجاح',
        'host' => $host,
        'database' => $dbname,
        'user' => $username_db,
        'mysql_version' => $result['version'],
        'current_database' => $result['current_db'],
        'current_user' => $result['current_user']
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'فشل الاتصال بقاعدة البيانات: ' . $e->getMessage(),
        'host' => $host,
        'database' => $dbname,
        'user' => $username_db,
        'error_code' => $e->getCode()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'خطأ عام: ' . $e->getMessage()
    ]);
}
?>
