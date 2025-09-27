<?php
require_once __DIR__ . '/config.php';

try {
    $database = new Database();
    $db = $database->getConnection();
    if (!$db) { sendResponse(false, 'Database connection failed.'); }

    // Ensure table exists (schema aligned with add_report.php)
    $db->exec("CREATE TABLE IF NOT EXISTS reports (
        id INT AUTO_INCREMENT PRIMARY KEY,
        room_id INT NOT NULL,
        employee_id INT NOT NULL,
        priority ENUM('urgent','medium','low') NOT NULL DEFAULT 'low',
        title VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        status ENUM('open','in_progress','closed') DEFAULT 'open',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    // Params
    $employeeId = isset($_GET['employee_id']) ? (int)$_GET['employee_id'] : 0;
    $priority = isset($_GET['priority']) ? strtolower(trim($_GET['priority'])) : '';
    $status = isset($_GET['status']) ? strtolower(trim($_GET['status'])) : '';
    $limit = isset($_GET['limit']) ? max(1, (int)$_GET['limit']) : 50;
    $offset = isset($_GET['offset']) ? max(0, (int)$_GET['offset']) : 0;

    if ($employeeId <= 0) {
        sendResponse(false, 'employee_id is required');
    }
    $allowedPriority = ['', 'urgent','medium','low'];
    if (!in_array($priority, $allowedPriority, true)) { $priority = ''; }
    $allowedStatus = ['', 'open','in_progress','closed'];
    if (!in_array($status, $allowedStatus, true)) { $status = ''; }

    $sql = "SELECT r.id,
                   r.room_id,
                   rf.room_number AS room_number,
                   r.employee_id,
                   r.priority,
                   r.title,
                   r.description,
                   r.status,
                   r.created_at
            FROM reports r
            LEFT JOIN rooms rf ON rf.id = r.room_id
            WHERE r.employee_id = :eid";
    if ($priority !== '') { $sql .= " AND r.priority = :p"; }
    if ($status !== '') { $sql .= " AND r.status = :s"; }
    $sql .= " ORDER BY r.created_at DESC, r.id DESC LIMIT :lim OFFSET :off";

    $stmt = $db->prepare($sql);
    $stmt->bindValue(':eid', $employeeId, PDO::PARAM_INT);
    if ($priority !== '') { $stmt->bindValue(':p', $priority, PDO::PARAM_STR); }
    if ($status !== '') { $stmt->bindValue(':s', $status, PDO::PARAM_STR); }
    $stmt->bindValue(':lim', (int)$limit, PDO::PARAM_INT);
    $stmt->bindValue(':off', (int)$offset, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll();

    sendResponse(true, 'Reports fetched.', ['reports' => $rows, 'count' => count($rows)]);
} catch (Throwable $e) {
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
