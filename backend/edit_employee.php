<?php
require_once __DIR__ . '/config.php';

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Method not allowed');
}

try {
    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) {
        sendResponse(false, 'Database connection failed');
    }

    // Read JSON body
    $raw = file_get_contents('php://input');
    $payload = json_decode($raw, true);
    if (!is_array($payload)) {
        sendResponse(false, 'Invalid JSON payload');
    }

    $id = (int)($payload['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, 'Valid employee ID is required');
    }

    // Check if employee exists
    $checkStmt = $pdo->prepare('SELECT COUNT(*) AS c FROM employees WHERE id = ?');
    $checkStmt->execute([$id]);
    $exists = (int)$checkStmt->fetch()['c'];
    if ($exists === 0) {
        sendResponse(false, 'Employee not found');
    }

    // Fetch available columns in employees table
    $colsStmt = $pdo->query('SHOW COLUMNS FROM employees');
    $availableCols = [];
    while ($row = $colsStmt->fetch()) {
        $availableCols[] = $row['Field'];
    }

    // Prepare update data
    $updateData = [];
    $setParts = [];

    // Helper to add field for update if exists and provided
    $addField = function(string $key, $value) use (&$setParts, &$updateData, $availableCols) {
        if (in_array($key, $availableCols, true) && $value !== null && $value !== '') {
            $setParts[] = "$key = ?";
            $updateData[] = $value;
        }
    };

    // Update fields
    $addField('first_name', trim($payload['first_name'] ?? ''));
    $addField('last_name', trim($payload['last_name'] ?? ''));
    $addField('username', trim($payload['username'] ?? ''));
    $addField('phone', trim($payload['phone'] ?? ''));
    
    // Handle department/role
    $dept = trim($payload['department'] ?? ($payload['role'] ?? ''));
    if ($dept !== '') {
        if (in_array('department', $availableCols, true)) {
            $addField('department', $dept);
        } else {
            $addField('role', $dept);
        }
    }
    
    $addField('email', trim($payload['email'] ?? ''));
    $addField('assigned_section', trim($payload['assigned_section'] ?? ''));
    $addField('status', trim($payload['status'] ?? ''));

    // Handle password if provided - ALWAYS hash securely regardless of column name
    if (!empty($payload['password'])) {
        $passwordColumn = in_array('password_hash', $availableCols, true) ? 'password_hash' : (in_array('password', $availableCols, true) ? 'password' : null);
        if ($passwordColumn !== null) {
            $hashed = password_hash($payload['password'], PASSWORD_BCRYPT);
            $setParts[] = "$passwordColumn = ?";
            $updateData[] = $hashed;
        }
    }

    if (empty($setParts)) {
        sendResponse(false, 'No valid fields to update');
    }

    // Build and execute update query
    $setSql = implode(', ', $setParts);
    $sql = "UPDATE employees SET $setSql WHERE id = ?";
    $updateData[] = $id;
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute($updateData);

    sendResponse(true, 'Employee updated successfully');

} catch (Throwable $e) {
    sendResponse(false, 'Error: ' . $e->getMessage());
}
