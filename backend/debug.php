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
        'server_name' => $_SERVER['SERVER_NAME'] ?? 'N/A',
        'server_addr' => $_SERVER['SERVER_ADDR'] ?? 'N/A',
        'server_port' => $_SERVER['SERVER_PORT'] ?? 'N/A',
        'remote_addr' => $_SERVER['REMOTE_ADDR'] ?? 'N/A',
        'request_method' => $_SERVER['REQUEST_METHOD'] ?? 'N/A',
        'http_host' => $_SERVER['HTTP_HOST'] ?? 'N/A',
        'script_name' => $_SERVER['SCRIPT_NAME'] ?? 'N/A'
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
        $response['database']['test_query'] = $stmt ? 'successful' : 'failed';
        
        // Check if receptionists table exists
        $stmt = $conn->query("SHOW TABLES LIKE 'receptionists'");
        $table_exists = $stmt && $stmt->fetch();
        $response['database']['receptionists_table'] = $table_exists ? 'exists' : 'missing';
        
        // Count users if table exists
        if ($table_exists) {
            $stmt = $conn->query("SELECT COUNT(*) as count FROM receptionists");
            $count = $stmt->fetch();
            $response['database']['user_count'] = $count['count'] ?? 0;
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
$base_dir = rtrim(str_replace('\\\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '')), '/');
$base_host = $_SERVER['HTTP_HOST'] ?? 'localhost';
$base_url = 'http://' . $base_host . $base_dir;
$response['connectivity']['base_url'] = $base_url;
$response['connectivity']['endpoints'] = array(
    'login' => $base_url . '/login.php',
    'config' => $base_url . '/config.php',
    'debug' => $base_url . '/debug.php'
);

// Generate recommended URLs for Android
$server_ip = $_SERVER['SERVER_ADDR'] ?? '127.0.0.1';
$server_port = $_SERVER['SERVER_PORT'] ?? '80';

$response['recommendations']['android_urls'] = array(
    'emulator' => 'http://10.0.2.2:' . $server_port . $base_dir . '/',
    'real_device_current' => 'http://' . $base_host . '/',
    'real_device_ip' => 'http://' . $server_ip . ':' . $server_port . $base_dir . '/',
    'localhost' => 'http://127.0.0.1:' . $server_port . $base_dir . '/'
);

// Network troubleshooting tips (French)
$response['troubleshooting'] = array(
    'common_issues' => array(
        '1. Assurez-vous que XAMPP est en cours d\'exécution',
        '2. Vérifiez qu\'Apache écoute sur le port 80',
        '3. Assurez-vous que l\'appareil Android et le PC sont sur le même réseau',
        '4. Vérifiez le pare-feu (Firewall)',
        '5. Utilisez ipconfig pour obtenir la bonne adresse IP'
    ),
    'test_commands' => array(
        'Windows' => 'ipconfig pour obtenir l\'adresse IP',
        'Android' => 'Ouvrez le navigateur et testez l\'URL'
    )
);

echo json_encode($response, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
?>
