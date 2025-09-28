<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Gérer les requêtes preflight (CORS)
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'config.php';

try {
    // Créer une connexion à la base de données
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Échec de la connexion à la base de données');
    }
    
    // Récupérer employee_id depuis GET, POST ou JSON
    $employee_id = 0;
    if (isset($_GET['employee_id'])) {
        $employee_id = intval($_GET['employee_id']);
    } elseif (isset($_POST['employee_id'])) {
        $employee_id = intval($_POST['employee_id']);
    } else {
        $raw = file_get_contents('php://input');
        if ($raw) {
            $json = json_decode($raw, true);
            if (json_last_error() === JSON_ERROR_NONE && isset($json['employee_id'])) {
                $employee_id = intval($json['employee_id']);
            }
        }
    }
    
    if ($employee_id <= 0) {
        echo json_encode([
            'success' => false,
            'message' => "L'identifiant de l'employé est requis",
            'tasks' => []
        ]);
        exit;
    }
    
    // Récupérer les tâches de l'employé
    $stmt = $pdo->prepare("\n        SELECT \n            id,\n            title,\n            description,\n            status,\n            created_at,\n            updated_at\n        FROM tasks \n        WHERE employee_id = ? \n        ORDER BY \n            CASE \n                WHEN status = 'pending' THEN 1\n                WHEN status = 'in_progress' THEN 2\n                WHEN status = 'done' THEN 3\n            END,\n            created_at DESC\n    ");
    
    $stmt->execute([$employee_id]);
    $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Formater les tâches pour Android
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
    
    // Compter les tâches par statut
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
        'message' => 'Tâches récupérées avec succès',
        'tasks' => $formatted_tasks,
        'counts' => $counts
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Erreur lors de la récupération des tâches: ' . $e->getMessage(),
        'tasks' => []
    ]);
}

function getStatusText($status) {
    switch ($status) {
        case 'pending':
            return 'En attente';
        case 'in_progress':
            return 'En cours';
        case 'done':
            return 'Terminée';
        default:
            return 'Indéfini';
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
