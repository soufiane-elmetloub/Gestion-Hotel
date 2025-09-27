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
    
    // Get all tasks with employee information
    $sql = "SELECT 
                t.id,
                t.employee_id,
                t.title,
                t.description,
                t.status,
                t.created_at,
                t.updated_at,
                CONCAT(e.first_name, ' ', e.last_name) as employee_name
            FROM tasks t
            LEFT JOIN employees e ON t.employee_id = e.id
            ORDER BY t.created_at DESC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'data' => $tasks,
        'count' => count($tasks)
    ]);

} catch (Exception $e) {
    error_log('get_all_tasks error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage(),
        'data' => []
    ]);
}
?>
