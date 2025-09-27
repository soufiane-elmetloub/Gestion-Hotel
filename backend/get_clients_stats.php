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
    
    // Get total clients count
    $stmt = $pdo->prepare("SELECT COUNT(*) as total FROM clients");
    $stmt->execute();
    $total = $stmt->fetch(PDO::FETCH_ASSOC)['total'];
    
    // Get active clients (registered in last 30 days)
    $stmt = $pdo->prepare("
        SELECT COUNT(*) as active 
        FROM clients 
        WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    ");
    $stmt->execute();
    $active = $stmt->fetch(PDO::FETCH_ASSOC)['active'];
    
    // Calculate inactive clients
    $inactive = $total - $active;
    
    echo json_encode([
        'success' => true,
        'data' => [
            'total' => (int)$total,
            'active' => (int)$active,
            'inactive' => (int)$inactive
        ]
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
