<?php
require_once __DIR__ . '/config.php';

// Ensure JSON response
header('Content-Type: application/json');

// Start / resume session to get super admin id
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

try {
    // Validate session: expect super admin info stored like other endpoints
    $superAdminId = null;
    if (!empty($_SESSION['super_admin_logged_in']) && $_SESSION['super_admin_logged_in'] === true && !empty($_SESSION['super_admin_id'])) {
        $superAdminId = (int)$_SESSION['super_admin_id'];
    }
    if (!$superAdminId) {
        http_response_code(401);
        sendResponse(false, 'Unauthorized: super admin session required');
    }

    // Only POST
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        http_response_code(405);
        sendResponse(false, 'Method not allowed, use POST');
    }

    // Read JSON body
    $raw = file_get_contents('php://input');
    $body = json_decode($raw, true);
    if (!is_array($body)) {
        sendResponse(false, 'Invalid JSON body');
    }

    $employee_id = isset($body['employee_id']) ? (int)$body['employee_id'] : 0;
    $title = isset($body['title']) ? trim($body['title']) : '';
    $message = isset($body['message']) ? trim($body['message']) : '';

    if ($employee_id <= 0 || $title === '' || $message === '') {
        sendResponse(false, 'employee_id, title and message are required');
    }

    // DB
    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) {
        sendResponse(false, 'Database connection failed');
    }

    // Insert
    $stmt = $pdo->prepare("INSERT INTO notifications (super_admin_id, employee_id, title, message, is_read) VALUES (?, ?, ?, ?, 0)");
    $stmt->execute([$superAdminId, $employee_id, $title, $message]);

    $insertId = (int)$pdo->lastInsertId();
    sendResponse(true, 'Notification sent successfully', [
        'id' => $insertId,
        'super_admin_id' => $superAdminId,
        'employee_id' => $employee_id,
        'title' => $title,
        'message' => $message,
    ]);

} catch (Throwable $e) {
    http_response_code(500);
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
