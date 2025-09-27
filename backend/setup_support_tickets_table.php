<?php
require_once 'config.php';

header('Content-Type: text/html; charset=utf-8');

$conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$conn->set_charset("utf8mb4");

echo "<h1>Support Tickets Table Setup</h1>";

// Create table with normalized schema (matches support_tickets.sql provided)
$createSql = "CREATE TABLE IF NOT EXISTS support_tickets (
    id INT(11) NOT NULL AUTO_INCREMENT,
    employee_id INT(11) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status ENUM('open','in_progress','closed') DEFAULT 'open',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_support_employee (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

if ($conn->query($createSql) === TRUE) {
    echo "<p style='color:green;'>Table 'support_tickets' created successfully or already exists.</p>";
} else {
    echo "<p style='color:red;'>Error creating table: " . $conn->error . "</p>";
}

// Check if FK already exists to avoid errno: 121 duplicate key errors (use schema-unique name)
$fkExists = false;
$checkFkSql = "SELECT COUNT(*) AS cnt FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
               WHERE CONSTRAINT_SCHEMA = ? 
                 AND TABLE_NAME = 'support_tickets' 
                 AND CONSTRAINT_NAME = 'fk_support_tickets_employee_id' 
                 AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
if ($stmt = $conn->prepare($checkFkSql)) {
    $dbName = DB_NAME; // bind_param requires variables by reference, not constants
    $stmt->bind_param('s', $dbName);
    if ($stmt->execute()) {
        $stmt->bind_result($cnt);
        if ($stmt->fetch()) {
            $fkExists = ((int)$cnt > 0);
        }
    }
    $stmt->close();
}

if ($fkExists) {
    echo "<p style='color:blue;'>Foreign key 'fk_support_tickets_employee_id' already exists. Skipping.</p>";
} else {
    $fkSql = "ALTER TABLE support_tickets
        ADD CONSTRAINT fk_support_tickets_employee_id FOREIGN KEY (employee_id)
        REFERENCES employees (id)
        ON DELETE CASCADE ON UPDATE CASCADE;";

    if ($conn->query($fkSql) === TRUE) {
        echo "<p style='color:green;'>Foreign key 'fk_support_tickets_employee_id' added.</p>";
    } else {
        echo "<p style='color:orange;'>FK add notice: " . htmlspecialchars($conn->error) . "</p>";
    }
}

$conn->close();

echo "<p>Database setup script finished.</p>";
?>
