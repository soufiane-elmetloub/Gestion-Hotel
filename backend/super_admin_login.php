<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

require_once __DIR__ . '/config.php';

// Start session
session_start();

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// Validate input
if (!isset($input['username']) || !isset($input['password'])) {
    echo json_encode([
        'success' => false,
        'message' => "Le nom d'utilisateur et le mot de passe sont requis"
    ]);
    exit;
}

$username = trim($input['username']);
$password = trim($input['password']);

// Validate credentials
if (empty($username) || empty($password)) {
    echo json_encode([
        'success' => false,
        'message' => "Veuillez saisir le nom d'utilisateur et le mot de passe"
    ]);
    exit;
}

try {
    // Create database connection via central configuration
    $database = new Database();
    $pdo = $database->getConnection();
    if (!$pdo) {
        throw new PDOException("Échec de la connexion à la base de données");
    }
    
    // Check if super_admin table exists, if not create it
    $checkTable = $pdo->query("SHOW TABLES LIKE 'super_admin'");
    if ($checkTable->rowCount() == 0) {
        // Create table and insert default admin
        $createTable = "
            CREATE TABLE `super_admin` (
                `id` int(11) NOT NULL AUTO_INCREMENT,
                `username` varchar(100) NOT NULL,
                `password_hash` varchar(255) NOT NULL,
                `created_at` timestamp NULL DEFAULT current_timestamp(),
                `last_login` timestamp NULL DEFAULT NULL,
                `is_active` tinyint(1) DEFAULT 1,
                PRIMARY KEY (`id`),
                UNIQUE KEY `username` (`username`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
        ";
        $pdo->exec($createTable);
        
        // Insert default super admin with hashed password
        $hash = password_hash('super-1234', PASSWORD_DEFAULT);
        $insertAdmin = $pdo->prepare("INSERT INTO `super_admin` (`username`, `password_hash`, `is_active`) VALUES ('superadmin', :ph, 1)");
        $insertAdmin->execute([':ph' => $hash]);
    }
    
    // Query to find user
    $stmt = $pdo->prepare("SELECT * FROM super_admin WHERE username = ? AND is_active = 1");
    $stmt->execute([$username]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($user) {
        // Verify hashed password; backward-compatibility: migrate if stored as plain text
        $isValid = false;
        if (password_verify($password, $user['password_hash'])) {
            $isValid = true;
        } elseif ($password === $user['password_hash']) {
            // migrate to hashed password
            $newHash = password_hash($password, PASSWORD_DEFAULT);
            $upd = $pdo->prepare("UPDATE super_admin SET password_hash = ? WHERE id = ?");
            $upd->execute([$newHash, $user['id']]);
            $isValid = true;
        }
        
        if ($isValid) {
            // Update last login
            $updateLogin = $pdo->prepare("UPDATE super_admin SET last_login = NOW() WHERE id = ?");
            $updateLogin->execute([$user['id']]);
            
            // Set session variables
            $_SESSION['super_admin_id'] = $user['id'];
            $_SESSION['super_admin_username'] = $user['username'];
            $_SESSION['super_admin_logged_in'] = true;
            
            echo json_encode([
                'success' => true,
                'message' => 'Connexion réussie',
                'user' => [
                    'id' => $user['id'],
                    'username' => $user['username'],
                    'last_login' => $user['last_login']
                ]
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'message' => 'Mot de passe incorrect'
            ]);
        }
    } else {
        echo json_encode([
            'success' => false,
            'message' => "Nom d'utilisateur introuvable ou inactif"
        ]);
    }
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Erreur de base de données: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    error_log("General error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Erreur inattendue'
    ]);
}
?>
