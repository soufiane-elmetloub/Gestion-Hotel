<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'config.php';

try {
    // Create database connection
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Database connection failed');
    }
    $createSql = "CREATE TABLE IF NOT EXISTS `tasks` (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `employee_id` int(11) NOT NULL,
        `title` varchar(255) NOT NULL,
        `description` text NOT NULL,
        `status` enum('pending','in_progress','done') DEFAULT 'pending',
        `created_at` timestamp NULL DEFAULT current_timestamp(),
        `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
        PRIMARY KEY (`id`),
        KEY `employee_id` (`employee_id`),
        CONSTRAINT `tasks_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci";
    
    $pdo->exec($createSql);

    // Read JSON body
    $raw = file_get_contents('php://input');
    $body = json_decode($raw, true);
    if (!is_array($body)) {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid JSON payload.'
        ]);
        exit;
    }

    $employee_id = isset($body['employee_id']) ? (int)$body['employee_id'] : 0;
    $title = isset($body['title']) ? trim((string)$body['title']) : '';
    $description = isset($body['description']) ? trim((string)$body['description']) : '';
    $status = isset($body['status']) ? trim((string)$body['status']) : 'pending';

    if ($employee_id <= 0 || $title === '' || $description === '') {
        echo json_encode([
            'success' => false,
            'message' => 'Required fields: employee_id, title, description.'
        ]);
        exit;
    }

    $allowedStatuses = ['pending','in_progress','done'];
    if (!in_array($status, $allowedStatuses, true)) {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid status. Allowed: pending, in_progress, done'
        ]);
        exit;
    }

    // Check if employee exists
    $stmt = $pdo->prepare('SELECT id FROM employees WHERE id = ? LIMIT 1');
    $stmt->execute([$employee_id]);
    if (!$stmt->fetch()) {
        echo json_encode([
            'success' => false,
            'message' => 'Employee not found.'
        ]);
        exit;
    }

    // Insert task
    $ins = $pdo->prepare('INSERT INTO tasks (employee_id, title, description, status) VALUES (?, ?, ?, ?)');
    $ins->execute([$employee_id, $title, $description, $status]);

    $newId = (int)$pdo->lastInsertId();
    
    echo json_encode([
        'success' => true,
        'message' => 'Task created successfully.',
        'data' => ['id' => $newId]
    ]);

} catch (Exception $e) {
    error_log('add_task error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
}
?>
