<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Start session
session_start();

// Check if super admin is logged in
if (isset($_SESSION['super_admin_logged_in']) && $_SESSION['super_admin_logged_in'] === true) {
    echo json_encode([
        'authenticated' => true,
        'user' => [
            'id' => $_SESSION['super_admin_id'],
            'username' => $_SESSION['super_admin_username']
        ]
    ]);
} else {
    echo json_encode([
        'authenticated' => false
    ]);
}
?>
