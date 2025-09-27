<?php
// Database configuration
$host = 'localhost';
$dbname = 'smart-hotel';
$username_db = 'Unknown';
$password_db = '5L7Fqp9GG-@r7trj';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username_db, $password_db);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get all tables
    $stmt = $pdo->query("SHOW TABLES");
    $tables = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    echo "<h2>Tables in smart-hotel database:</h2>";
    foreach ($tables as $table) {
        echo "<h3>$table</h3>";
        
        // Get table structure and count
        try {
            $countStmt = $pdo->query("SELECT COUNT(*) as count FROM `$table`");
            $count = $countStmt->fetch(PDO::FETCH_ASSOC)['count'];
            echo "<p>Records: $count</p>";
            
            $structStmt = $pdo->query("DESCRIBE `$table`");
            $columns = $structStmt->fetchAll(PDO::FETCH_ASSOC);
            echo "<ul>";
            foreach ($columns as $col) {
                echo "<li>{$col['Field']} ({$col['Type']})</li>";
            }
            echo "</ul>";
        } catch (Exception $e) {
            echo "<p>Error: " . $e->getMessage() . "</p>";
        }
        echo "<hr>";
    }
    
} catch (PDOException $e) {
    echo "Database error: " . $e->getMessage();
}
?>
