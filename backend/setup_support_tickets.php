<?php
header('Content-Type: text/html; charset=utf-8');
require_once __DIR__ . '/config.php';

function out($msg){ echo htmlspecialchars($msg, ENT_QUOTES, 'UTF-8') . "<br>\n"; }

try {
    $db = (new Database())->getConnection();
    if (!$db) { throw new Exception('Database connection not initialized'); }

    $sql = "CREATE TABLE IF NOT EXISTS support_tickets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id INT NULL,
                name VARCHAR(150) NULL,
                email VARCHAR(190) NULL,
                section VARCHAR(100) NULL,
                subject VARCHAR(190) NULL,
                message TEXT NULL,
                status ENUM('open','pending','resolved') NOT NULL DEFAULT 'open',
                priority ENUM('low','normal','high') NOT NULL DEFAULT 'normal',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_status (status),
                INDEX idx_priority (priority),
                INDEX idx_created_at (created_at),
                INDEX idx_employee_id (employee_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

    $db->exec($sql);
    out('Table support_tickets is ready.');

    // Optionally seed a few sample rows if empty
    $c = (int)$db->query("SELECT COUNT(*) AS c FROM support_tickets")->fetch(PDO::FETCH_ASSOC)['c'];
    if ($c === 0) {
        $seed = $db->prepare("INSERT INTO support_tickets (employee_id, name, email, section, subject, message, status, priority) VALUES 
            (101, 'Ahmed R', 'ahmed@example.com', 'Section A', 'Issue with room status', 'The room 201 shows occupied but it is free.', 'open', 'high'),
            (102, 'Soufiane', 'soufiane@example.com', 'Section B', 'App login issue', 'Cannot login from Android app occasionally.', 'pending', 'normal'),
            (NULL, 'Guest User', 'guest@example.com', 'Website', 'Feedback', 'Great service, thank you!', 'resolved', 'low')");
        $seed->execute();
        out('Seed data inserted (3 rows).');
    } else {
        out('Existing rows: ' . $c);
    }

    out('Done. You can now open ../Web/contact.html and click Refresh.');
} catch (Throwable $e) {
    http_response_code(500);
    out('ERROR: ' . $e->getMessage());
}
