<?php
require_once __DIR__ . '/config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { exit(0); }

try{
    $raw = file_get_contents('php://input');
    $body = json_decode($raw, true);
    if (!is_array($body)) { sendResponse(false, 'Invalid JSON body'); }

    $id = isset($body['id']) ? (int)$body['id'] : 0;
    $employeeId = isset($body['employee_id']) ? (int)$body['employee_id'] : 0;
    if ($id <= 0 || $employeeId <= 0) { sendResponse(false, 'id and employee_id are required'); }

    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) { sendResponse(false, 'Database connection failed'); }

    $stmt = $pdo->prepare('UPDATE notifications SET is_read = 1 WHERE id = ? AND employee_id = ?');
    $stmt->execute([$id, $employeeId]);

    sendResponse(true, 'Marked as read');
}catch(Throwable $e){
    http_response_code(500);
    sendResponse(false, 'Server error: '.$e->getMessage());
}
