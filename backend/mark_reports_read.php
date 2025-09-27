<?php
require_once __DIR__ . '/config.php';

function ensureIsReadColumn(PDO $db) {
    try {
        $stmt = $db->prepare("SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'is_read'");
        $stmt->execute();
        $row = $stmt->fetch();
        $exists = isset($row['c']) ? (int)$row['c'] > 0 : false;
        if (!$exists) {
            $db->exec("ALTER TABLE reports ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0");
        }
    } catch (Throwable $e) {
        // ignore, best-effort
    }
}

try {
    $database = new Database();
    $db = $database->getConnection();
    if (!$db) { sendResponse(false, 'Database connection failed.'); }

    // Ensure base table
    $db->exec("CREATE TABLE IF NOT EXISTS reports (
        id INT AUTO_INCREMENT PRIMARY KEY,
        room_id INT NOT NULL,
        employee_id INT NOT NULL,
        priority ENUM('urgent','medium','low') NOT NULL DEFAULT 'low',
        title VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        status ENUM('open','in_progress','closed') DEFAULT 'open',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX (employee_id), INDEX (priority), INDEX (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
    ensureIsReadColumn($db);

    // Inputs (allow GET or POST)
    $employeeId = isset($_REQUEST['employee_id']) ? (int)$_REQUEST['employee_id'] : 0;
    $priority = isset($_REQUEST['priority']) ? strtolower(trim($_REQUEST['priority'])) : '';
    if ($employeeId <= 0) { sendResponse(false, 'employee_id is required'); }
    $allowedPriority = ['', 'urgent','medium','low'];
    if (!in_array($priority, $allowedPriority, true)) { $priority = ''; }

    // Only mark currently unread ones
    $sql = "UPDATE reports SET is_read = 1 WHERE employee_id = :eid AND is_read = 0";
    if ($priority !== '') { $sql .= " AND priority = :p"; }
    $stmt = $db->prepare($sql);
    $stmt->bindValue(':eid', $employeeId, PDO::PARAM_INT);
    if ($priority !== '') { $stmt->bindValue(':p', $priority, PDO::PARAM_STR); }
    $stmt->execute();

    sendResponse(true, 'Marked as read.', ['affected' => $stmt->rowCount()]);
} catch (Throwable $e) {
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
