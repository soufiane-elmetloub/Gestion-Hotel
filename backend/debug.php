<?php
// Debug endpoint for Android connectivity troubleshooting
require_once 'config.php';

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Set JSON response headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$response = array(
    'success' => true,
    'timestamp' => date('Y-m-d H:i:s'),
    'server_info' => array(
        'server_name' => $_SERVER['SERVER_NAME'],
        'server_addr' => $_SERVER['SERVER_ADDR'],
        'server_port' => $_SERVER['SERVER_PORT'],
        'remote_addr' => $_SERVER['REMOTE_ADDR'],
        'request_method' => $_SERVER['REQUEST_METHOD'],
        'http_host' => $_SERVER['HTTP_HOST'] ?? 'N/A',
        'script_name' => $_SERVER['SCRIPT_NAME']
    ),
    'database' => array(),
    'connectivity' => array(),
    'recommendations' => array()
);

// Test database connection
try {
    $database = new Database();
    $conn = $database->getConnection();
    
    if ($conn) {
        $response['database']['status'] = 'connected';
        $response['database']['host'] = DB_HOST;
        $response['database']['name'] = DB_NAME;
        
        // Test basic query
        $stmt = $conn->query("SELECT 1");
        $response['database']['test_query'] = 'successful';
        
        // Check if users table exists
        $stmt = $conn->query("SHOW TABLES LIKE 'receptionists'");
        $table_exists = $stmt->fetch();
        $response['database']['receptionists_table'] = $table_exists ? 'exists' : 'missing';
        
        // Count users
        if ($table_exists) {
            $stmt = $conn->query("SELECT COUNT(*) as count FROM receptionists");
            $count = $stmt->fetch();
            $response['database']['user_count'] = $count['count'];
        }
    } else {
        $response['database']['status'] = 'failed';
        $response['database']['error'] = 'Could not connect to database';
    }
} catch (Exception $e) {
    $response['database']['status'] = 'error';
    $response['database']['error'] = $e->getMessage();
}

// Connectivity tests
$base_url = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['SCRIPT_NAME']);
$response['connectivity']['base_url'] = $base_url;
$response['connectivity']['endpoints'] = array(
    'login' => $base_url . '/login.php',
    'config' => $base_url . '/config.php',
    'debug' => $base_url . '/debug.php'
);

// Generate recommended URLs for Android
$server_ip = $_SERVER['SERVER_ADDR'];
$server_port = $_SERVER['SERVER_PORT'];
$current_host = $_SERVER['HTTP_HOST'];

$response['recommendations']['android_urls'] = array(
    'emulator' => 'http://10.0.2.2:' . $server_port . str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])) . '/',
    'real_device_current' => 'http://' . $current_host . '/',
    'real_device_ip' => 'http://' . $server_ip . ':' . $server_port . str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])) . '/',
    'localhost' => 'http://127.0.0.1:' . $server_port . str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])) . '/'
);

// Network troubleshooting tips
$response['troubleshooting'] = array(
    'common_issues' => array(
        '1. تأكد أن XAMPP يعمل',
        '2. تأكد أن Apache يعمل على المنفذ 80',
        '3. تأكد أن الجهاز الأندرويد والكمبيوتر على نفس الشبكة',
        '4. افحص جدار الحماية (Firewall)',
        '5. استخدم ipconfig للحصول على IP الصحيح'
    ),
    'test_commands' => array(
        'Windows' => 'ipconfig للحصول على IP',
        'Android' => 'افتح المتصفح واختبر الURL'
    )
);

echo json_encode($response, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
?>
