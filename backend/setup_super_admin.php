<?php
// Script d'installation pour créer la table super_admin et insérer des données
require_once 'config.php';

try {
    // Créer une connexion via la classe Database
    $database = new Database();
    $pdo = $database->getConnection();
    if (!$pdo) {
        throw new PDOException('Échec de la connexion à la base de données');
    }
    
    // Drop table if exists
    $pdo->exec("DROP TABLE IF EXISTS `super_admin`");
    
    // Create super_admin table
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
    
    // Insert super admin data
    $hash = password_hash('super-1234', PASSWORD_DEFAULT);
    $insertData = "INSERT INTO `super_admin` (`id`, `username`, `password_hash`, `created_at`, `last_login`, `is_active`) VALUES (1, 'superadmin', :ph, NOW(), NULL, 1)";
    
    $stmt = $pdo->prepare($insertData);
    $stmt->execute([':ph' => $hash]);
    
    echo "<!DOCTYPE html>
    <html lang='fr' dir='ltr'>
    <head>
        <meta charset='UTF-8'>
        <title>Initialisation de la base de données</title>
        <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>
        <style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,'Helvetica Neue',Arial,'Noto Sans',sans-serif;background:#f8f9fc;}</style>
    </head>
    <body>
        <div class='container mt-5'>
            <div class='alert alert-success text-center'>
                <h3><i class='fas fa-check-circle'></i> Base de données initialisée avec succès !</h3>
                <p>La table super_admin a été créée et le super administrateur a été ajouté.</p>
                <hr>
                <p><strong>Identifiants de connexion (démo):</strong></p>
                <p>Nom d'utilisateur : <code>superadmin</code></p>
                <p>Mot de passe : <code>super-1234</code></p>
                <hr>
                <a href='../Web/login.html' class='btn btn-primary'>Aller à la page de connexion</a>
            </div>
        </div>
    </body>
    </html>";
    
} catch (PDOException $e) {
    echo "<!DOCTYPE html>
    <html lang='fr' dir='ltr'>
    <head>
        <meta charset='UTF-8'>
        <title>Erreur d'initialisation</title>
        <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css' rel='stylesheet'>
        <style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,'Helvetica Neue',Arial,'Noto Sans',sans-serif;background:#f8f9fc;}</style>
    </head>
    <body>
        <div class='container mt-5'>
            <div class='alert alert-danger text-center'>
                <h3><i class='fas fa-exclamation-triangle'></i> Erreur lors de l'initialisation de la base de données</h3>
                <p>" . htmlspecialchars($e->getMessage()) . "</p>
                <p>Assurez-vous que XAMPP est en cours d'exécution et que les paramètres de base de données dans config.php sont corrects.</p>
            </div>
        </div>
    </body>
    </html>";
}
?>
