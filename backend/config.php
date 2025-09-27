<?php
// Database configuration
// NOTE: To keep secrets out of GitHub, this file supports overrides via:
// 1) backend/config.local.php (gitignored) — recommended for local development
// 2) Environment variables: DB_HOST, DB_USER, DB_PASS, DB_NAME
// Safe defaults below will be used if no overrides are provided.

// 1) Load local override if present (not committed to Git)
$__localConfigPath = __DIR__ . DIRECTORY_SEPARATOR . 'config.local.php';
if (file_exists($__localConfigPath)) {
    require_once $__localConfigPath; // May define DB_* constants
}

// 2) Helper to read environment variables with default fallback
if (!function_exists('envOrDefault')) {
    function envOrDefault($key, $default) {
        $val = getenv($key);
        return ($val !== false && $val !== '') ? $val : $default;
    }
}

// 3) Define constants only if not already defined by config.local.php
if (!defined('DB_HOST')) {
    define('DB_HOST', envOrDefault('DB_HOST', 'localhost'));
}
if (!defined('DB_USER')) {
    // Use a safe placeholder by default; set real value in config.local.php or env
    define('DB_USER', envOrDefault('DB_USER', 'root'));
}
if (!defined('DB_PASS')) {
    define('DB_PASS', envOrDefault('DB_PASS', ''));
}
if (!defined('DB_NAME')) {
    define('DB_NAME', envOrDefault('DB_NAME', 'smart-hotel'));
}

class Database {
    private $host = DB_HOST;
    private $db_name = DB_NAME;
    private $username = DB_USER;
    private $password = DB_PASS;
    private $conn;

    public function getConnection() {
        $this->conn = null;
        
        try {
            $this->conn = new PDO("mysql:host=" . $this->host . ";dbname=" . $this->db_name . ";charset=utf8mb4", 
                                 $this->username, $this->password);
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->conn->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        } catch(PDOException $e) {
            error_log("Database connection failed: " . $e->getMessage());
            if (!headers_sent()) {
                header('Content-Type: application/json');
                echo json_encode([
                    'success' => false,
                    'message' => 'خطأ في الاتصال بقاعدة البيانات',
                    'error' => $e->getMessage()
                ]);
            }
            return null;
        }
        
        return $this->conn;
    }
}

// Response helper function
function sendResponse($success, $message, $data = null) {
    if (!headers_sent()) {
        header('Content-Type: application/json');
    }
    
    $response = array(
        'success' => $success,
        'message' => $message
    );
    
    if ($data !== null) {
        $response['data'] = $data;
    }
    
    echo json_encode($response);
    exit();
}
?>
