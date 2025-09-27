<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Start session
session_start();

// Destroy all session data
session_unset();
session_destroy();

// Start a new session to send response
session_start();

echo json_encode([
    'success' => true,
    'message' => 'تم تسجيل الخروج بنجاح'
]);
?>
