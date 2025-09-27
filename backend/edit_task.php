<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
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

    $id = isset($body['id']) ? (int)$body['id'] : 0;
    $employee_id = isset($body['employee_id']) ? (int)$body['employee_id'] : 0;
    $title = isset($body['title']) ? trim((string)$body['title']) : '';
    $description = isset($body['description']) ? trim((string)$body['description']) : '';
    $status = isset($body['status']) ? trim((string)$body['status']) : 'pending';

    if ($id <= 0 || $employee_id <= 0 || $title === '' || $description === '') {
        echo json_encode([
            'success' => false,
            'message' => 'Required fields: id, employee_id, title, description.'
        ]);
        exit;
    }

    $allowedStatuses = ['pending','in_progress','done','completed'];
    if (!in_array($status, $allowedStatuses, true)) {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid status. Allowed: pending, in_progress, done, completed'
        ]);
        exit;
    }

    // Map 'completed' to 'done' for database compatibility
    if ($status === 'completed') {
        $status = 'done';
    }

    // Check if task exists
    $stmt = $pdo->prepare('SELECT id FROM tasks WHERE id = ? LIMIT 1');
    $stmt->execute([$id]);
    if (!$stmt->fetch()) {
        echo json_encode([
            'success' => false,
            'message' => 'Task not found.'
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

    // Update task
    $upd = $pdo->prepare('UPDATE tasks SET employee_id = ?, title = ?, description = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?');
    $upd->execute([$employee_id, $title, $description, $status, $id]);

    echo json_encode([
        'success' => true,
        'message' => 'Task updated successfully.'
    ]);

} catch (Exception $e) {
    error_log('edit_task error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
}
?>
