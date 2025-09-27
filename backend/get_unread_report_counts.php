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

    // Ensure table and column exist
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

    $employeeId = isset($_GET['employee_id']) ? (int)$_GET['employee_id'] : 0;
    if ($employeeId <= 0) { sendResponse(false, 'employee_id is required'); }

    // Only unread
    $sql = "SELECT 
                SUM(priority='urgent' AND is_read=0) AS urgent,
                SUM(priority='medium' AND is_read=0) AS medium,
                SUM(priority='low' AND is_read=0) AS low,
                SUM(is_read=0) AS total
            FROM reports
            WHERE employee_id = :eid";

    $stmt = $db->prepare($sql);
    $stmt->bindValue(':eid', $employeeId, PDO::PARAM_INT);
    $stmt->execute();
    $row = $stmt->fetch();

    $data = [
        'urgent' => (int)($row['urgent'] ?? 0),
        'medium' => (int)($row['medium'] ?? 0),
        'low' => (int)($row['low'] ?? 0),
        'total' => (int)($row['total'] ?? 0),
    ];

    sendResponse(true, 'Unread report counts fetched.', $data);
} catch (Throwable $e) {
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
