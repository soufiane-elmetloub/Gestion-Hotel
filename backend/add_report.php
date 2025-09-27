<?php
require_once __DIR__ . '/config.php';

try {
    $database = new Database();
    $db = $database->getConnection();
    if (!$db) {
        sendResponse(false, 'Database connection failed.');
    }

    // Ensure reports table exists
    $createSql = "CREATE TABLE IF NOT EXISTS reports (
        id INT AUTO_INCREMENT PRIMARY KEY,
        room_id INT NOT NULL,
        employee_id INT NOT NULL,
        priority ENUM('urgent','medium','low') NOT NULL DEFAULT 'low',
        title VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        status ENUM('open','in_progress','closed') DEFAULT 'open',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
        FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
    $db->exec($createSql);

    // Parse input JSON
    $raw = file_get_contents('php://input');
    $payload = json_decode($raw, true);
    if (!is_array($payload)) {
        sendResponse(false, 'Invalid JSON payload.');
    }

    $room_id = isset($payload['room_id']) ? (int)$payload['room_id'] : 0;
    $employee_id = isset($payload['employee_id']) ? (int)$payload['employee_id'] : 0;
    $priority = isset($payload['priority']) ? strtolower(trim($payload['priority'])) : 'low';
    $title = isset($payload['title']) ? trim($payload['title']) : '';
    $description = isset($payload['description']) ? trim($payload['description']) : '';
    $status = isset($payload['status']) ? strtolower(trim($payload['status'])) : 'open';

    $validPriorities = ['urgent','medium','low'];
    $validStatus = ['open','in_progress','closed'];

    if ($room_id <= 0) sendResponse(false, 'room_id is required and must be a positive integer.');
    if ($employee_id <= 0) sendResponse(false, 'employee_id is required and must be a positive integer.');
    if (!in_array($priority, $validPriorities, true)) sendResponse(false, 'Invalid priority. Allowed: urgent, medium, low');
    if ($title === '') sendResponse(false, 'title is required.');
    if ($description === '') sendResponse(false, 'description is required.');
    if (!in_array($status, $validStatus, true)) sendResponse(false, 'Invalid status. Allowed: open, in_progress, closed');

    $stmt = $db->prepare("INSERT INTO reports (room_id, employee_id, priority, title, description, status)
                          VALUES (:room_id, :employee_id, :priority, :title, :description, :status)");
    $stmt->bindValue(':room_id', $room_id, PDO::PARAM_INT);
    $stmt->bindValue(':employee_id', $employee_id, PDO::PARAM_INT);
    $stmt->bindValue(':priority', $priority, PDO::PARAM_STR);
    $stmt->bindValue(':title', $title, PDO::PARAM_STR);
    $stmt->bindValue(':description', $description, PDO::PARAM_STR);
    $stmt->bindValue(':status', $status, PDO::PARAM_STR);

    $stmt->execute();

    $insertId = (int)$db->lastInsertId();
    sendResponse(true, 'Report created successfully.', ['id' => $insertId]);
} catch (PDOException $e) {
    $msg = $e->getMessage();
    // Provide friendlier message for foreign key constraint failures
    if (stripos($msg, 'foreign key') !== false) {
        sendResponse(false, 'Invalid room_id or employee_id (foreign key error).');
    }
    sendResponse(false, 'Database error: ' . $msg);
} catch (Throwable $e) {
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
