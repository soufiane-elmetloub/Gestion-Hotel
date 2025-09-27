<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

try {
    $database = new Database();
    $pdo = $database->getConnection();
    
    // Get search query - support both 'query' and 'last_name' parameters for Android compatibility
    $query = $_GET['query'] ?? $_GET['last_name'] ?? '';
    
    if (empty($query)) {
        echo json_encode([
            'success' => false,
            'message' => 'Search query is required',
            'supported_params' => ['query', 'last_name']
        ]);
        exit;
    }
    
    // Search clients by name, phone, or national ID
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
        WHERE 
            CONCAT(first_name, ' ', last_name) LIKE ? OR
            phone LIKE ? OR
            national_id LIKE ?
        ORDER BY created_at DESC
    ");
    
    $searchTerm = '%' . $query . '%';
    $stmt->execute([$searchTerm, $searchTerm, $searchTerm]);
    $clients = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'results' => $clients,
        'count' => count($clients),
        'query' => $query
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}
?>
