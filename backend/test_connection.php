<?php
// Simple test connection endpoint for Android app
require_once 'config.php';

// Add CORS headers
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json; charset=UTF-8");

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

try {
    // Test database connection
    $database = new Database();
    $conn = $database->getConnection();
    
    // Test query
    $stmt = $conn->query("SELECT 1 as test");
    $result = $stmt->fetch();
    
    // Return success response
    $response = array(
        'success' => true,
        'message' => 'Connection successful',
        'timestamp' => date('Y-m-d H:i:s'),
        'server' => $_SERVER['SERVER_NAME']
    );
    
    echo json_encode($response);
    
} catch (Exception $e) {
    // Return error response
    $response = array(
        'success' => false,
        'message' => 'Connection failed: ' . $e->getMessage(),
        'timestamp' => date('Y-m-d H:i:s')
    );
    
    echo json_encode($response);
}
?>
