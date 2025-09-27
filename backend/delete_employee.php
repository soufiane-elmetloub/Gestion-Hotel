<?php
require_once __DIR__ . '/config.php';

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Method not allowed');
}

try {
    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) {
        sendResponse(false, 'Database connection failed');
    }

    // Read JSON body
    $raw = file_get_contents('php://input');
    $payload = json_decode($raw, true);
    if (!is_array($payload)) {
        sendResponse(false, 'Invalid JSON payload');
    }

    $id = (int)($payload['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, 'Valid employee ID is required');
    }

    // Check if employee exists
    $checkStmt = $pdo->prepare('SELECT COUNT(*) AS c FROM employees WHERE id = ?');
    $checkStmt->execute([$id]);
    $exists = (int)$checkStmt->fetch()['c'];
    if ($exists === 0) {
        sendResponse(false, 'Employee not found');
    }

    // Delete employee
    $deleteStmt = $pdo->prepare('DELETE FROM employees WHERE id = ?');
    $deleteStmt->execute([$id]);

    if ($deleteStmt->rowCount() > 0) {
        sendResponse(true, 'Employee deleted successfully');
    } else {
        sendResponse(false, 'Failed to delete employee');
    }

} catch (Throwable $e) {
    sendResponse(false, 'Error: ' . $e->getMessage());
}
