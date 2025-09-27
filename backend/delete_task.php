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

    if ($id <= 0) {
        echo json_encode([
            'success' => false,
            'message' => 'Task ID is required.'
        ]);
        exit;
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

    // Delete task
    $del = $pdo->prepare('DELETE FROM tasks WHERE id = ?');
    $del->execute([$id]);

    echo json_encode([
        'success' => true,
        'message' => 'Task deleted successfully.'
    ]);

} catch (Exception $e) {
    error_log('delete_task error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
}
?>
