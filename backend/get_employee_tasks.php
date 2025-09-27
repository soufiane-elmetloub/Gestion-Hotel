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
    
    // Get employee_id from request
    $employee_id = isset($_GET['employee_id']) ? intval($_GET['employee_id']) : 0;
    
    if ($employee_id <= 0) {
        echo json_encode([
            'success' => false,
            'message' => 'معرف الموظف مطلوب',
            'tasks' => []
        ]);
        exit;
    }
    
    // Get tasks for the employee
    $stmt = $pdo->prepare("
        SELECT 
            id,
            title,
            description,
            status,
            created_at,
            updated_at
        FROM tasks 
        WHERE employee_id = ? 
        ORDER BY 
            CASE 
                WHEN status = 'pending' THEN 1
                WHEN status = 'in_progress' THEN 2
                WHEN status = 'done' THEN 3
            END,
            created_at DESC
    ");
    
    $stmt->execute([$employee_id]);
    $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Format tasks for Android
    $formatted_tasks = [];
    foreach ($tasks as $task) {
        $formatted_tasks[] = [
            'id' => intval($task['id']),
            'title' => $task['title'],
            'description' => $task['description'],
            'status' => $task['status'],
            'created_at' => $task['created_at'],
            'updated_at' => $task['updated_at'],
            'status_text' => getStatusText($task['status']),
            'status_color' => getStatusColor($task['status']),
            'priority_level' => getPriorityLevel($task['status'])
        ];
    }
    
    // Get task counts by status
    $counts = [
        'total' => count($tasks),
        'pending' => 0,
        'in_progress' => 0,
        'done' => 0
    ];
    
    foreach ($tasks as $task) {
        $counts[$task['status']]++;
    }
    
    echo json_encode([
        'success' => true,
        'message' => 'تم جلب المهام بنجاح',
        'tasks' => $formatted_tasks,
        'counts' => $counts
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'خطأ في جلب المهام: ' . $e->getMessage(),
        'tasks' => []
    ]);
}

function getStatusText($status) {
    switch ($status) {
        case 'pending':
            return 'في الانتظار';
        case 'in_progress':
            return 'قيد التنفيذ';
        case 'done':
            return 'مكتملة';
        default:
            return 'غير محدد';
    }
}

function getStatusColor($status) {
    switch ($status) {
        case 'pending':
            return '#FF9800'; // Orange
        case 'in_progress':
            return '#2196F3'; // Blue
        case 'done':
            return '#4CAF50'; // Green
        default:
            return '#9E9E9E'; // Grey
    }
}

function getPriorityLevel($status) {
    switch ($status) {
        case 'pending':
            return 'high';
        case 'in_progress':
            return 'medium';
        case 'done':
            return 'low';
        default:
            return 'medium';
    }
}
?>
