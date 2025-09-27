<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

try {
    // Create database connection using Database class
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Database connection failed');
    }
    
    // Get recent clients (last 10)
    $stmt = $pdo->prepare("
        SELECT 
            id,
            first_name,
            last_name,
            phone,
            national_id,
            created_at,
            CASE 
                WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 'نشط'
                ELSE 'غير نشط'
            END as status
        FROM clients 
        ORDER BY created_at DESC 
        LIMIT 10
    ");
    
    $stmt->execute();
    $clients = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'data' => $clients,
        'count' => count($clients)
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
