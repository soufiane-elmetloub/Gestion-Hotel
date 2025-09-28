<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

try {
    $database = new Database();
    $pdo = $database->getConnection();
    
    // Récupérer le terme de recherche — prend en charge 'query' et 'last_name' pour compatibilité Android
    $query = $_GET['query'] ?? $_GET['last_name'] ?? '';
    
    if (empty($query)) {
        echo json_encode([
            'success' => false,
            'message' => 'Le paramètre de recherche est requis',
            'supported_params' => ['query', 'last_name']
        ]);
        exit;
    }
    
    // Rechercher des clients par nom, téléphone ou identifiant national
    $stmt = $pdo->prepare("
        SELECT 
            id,
            first_name,
            last_name,
            phone,
            national_id,
            created_at,
            CASE 
                WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 'Actif'
                ELSE 'Inactif'
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
        'message' => 'Erreur de base de données: ' . $e->getMessage()
    ]);
}
?>
