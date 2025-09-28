<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Gérer les requêtes preflight (CORS)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Utiliser la configuration centralisée pour éviter toute fuite d'identifiants dans le code source
// config.php prend en charge les surcharges via config.local.php (ignoré par Git) ou variables d'environnement
require_once __DIR__ . '/config.php';

try {
    // Obtenir une connexion PDO via la classe Database (aucun secret exposé ici)
    $database = new Database();
    $pdo = $database->getConnection();
    if (!$pdo) {
        // config.php a déjà envoyé une réponse JSON d'erreur
        exit();
    }

    // Vérification minimale et non sensible de la connectivité
    $stmt = $pdo->query("SELECT VERSION() as version, DATABASE() as current_db");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    echo json_encode([
        'success' => true,
        'message' => 'Connexion à la base de données réussie',
        'mysql_version' => $result['version'] ?? null,
        'current_database' => $result['current_db'] ?? null
    ]);

} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Échec de la connexion à la base de données',
        'error_code' => $e->getCode()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Erreur générale'
    ]);
}
?>
