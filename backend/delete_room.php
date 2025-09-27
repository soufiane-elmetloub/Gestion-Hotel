<?php
require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Method not allowed');
}

try {
    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) {
        sendResponse(false, 'Database connection failed');
    }

    $raw = file_get_contents('php://input');
    $payload = json_decode($raw, true);
    if (!is_array($payload)) {
        sendResponse(false, 'Invalid JSON payload');
    }

    $id = (int)($payload['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, 'Valid room ID is required');
    }

    // Discover PK column
    $colsStmt = $pdo->query('SHOW COLUMNS FROM rooms');
    $available = [];
    while ($r = $colsStmt->fetch()) { $available[$r['Field']] = true; }
    $pkCol = isset($available['id']) ? 'id' : (isset($available['room_id']) ? 'room_id' : null);
    if ($pkCol === null) {
        sendResponse(false, 'Primary key column not found (expected id or room_id)');
    }

    // Check exists
    $chk = $pdo->prepare("SELECT COUNT(*) c FROM rooms WHERE $pkCol = ?");
    $chk->execute([$id]);
    if ((int)$chk->fetch()['c'] === 0) {
        sendResponse(false, 'Room not found');
    }

    // Delete
    $del = $pdo->prepare("DELETE FROM rooms WHERE $pkCol = ?");
    $del->execute([$id]);

    if ($del->rowCount() > 0) {
        sendResponse(true, 'Room deleted successfully');
    } else {
        sendResponse(false, 'Failed to delete room');
    }

} catch (Throwable $e) {
    sendResponse(false, 'Error: ' . $e->getMessage());
}
