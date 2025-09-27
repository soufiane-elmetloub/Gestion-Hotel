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

    $username = trim($payload['username'] ?? '');
    $password = (string)($payload['password'] ?? '');

    if ($username === '' || $password === '') {
        sendResponse(false, 'Username and password are required');
    }

    // Fetch available columns in employees table to keep it flexible with current schema
    $colsStmt = $pdo->query('SHOW COLUMNS FROM employees');
    $availableCols = [];
    while ($row = $colsStmt->fetch()) {
        $availableCols[] = $row['Field'];
    }

    // Determine password column
    $passwordColumn = in_array('password_hash', $availableCols, true) ? 'password_hash' : (in_array('password', $availableCols, true) ? 'password' : null);
    if ($passwordColumn === null) {
        sendResponse(false, 'No password column found in employees table');
    }

    // Check for duplicate username if username column exists
    if (in_array('username', $availableCols, true)) {
        $dupStmt = $pdo->prepare('SELECT COUNT(*) AS c FROM employees WHERE username = ?');
        $dupStmt->execute([$username]);
        $exists = (int)$dupStmt->fetch()['c'];
        if ($exists > 0) {
            sendResponse(false, 'Username already exists');
        }
    }

    // Prepare insert data
    $insertData = [];
    $columns = [];
    $placeholders = [];

    // Helper to add if exists and provided
    $addField = function(string $key, $value) use (&$columns, &$placeholders, &$insertData, $availableCols) {
        if (in_array($key, $availableCols, true) && $value !== null && $value !== '') {
            $columns[] = $key;
            $placeholders[] = '?';
            $insertData[] = $value;
        }
    };

    // Names, phone, department/role, section, status, email
    $addField('first_name', trim($payload['first_name'] ?? ''));
    $addField('last_name', trim($payload['last_name'] ?? ''));
    $addField('username', $username);

    // Always hash password securely, regardless of column name
    $hashed = password_hash($password, PASSWORD_BCRYPT);
    $addField($passwordColumn, $hashed);

    $addField('phone', trim($payload['phone'] ?? ''));

    // Prefer `department` column. If it doesn't exist, fallback to `role`.
    $dept = trim($payload['department'] ?? ($payload['role'] ?? 'Employee'));
    if (in_array('department', $availableCols, true)) {
        $addField('department', $dept);
    } else {
        $addField('role', $dept);
    }

    // Optional email if column exists
    $addField('email', trim($payload['email'] ?? ''));

    $addField('assigned_section', trim($payload['assigned_section'] ?? ''));
    $addField('status', trim($payload['status'] ?? 'active'));

    // created_at if exists
    if (in_array('created_at', $availableCols, true)) {
        $columns[] = 'created_at';
        $placeholders[] = 'NOW()';
    }

    if (empty($columns)) {
        sendResponse(false, 'No valid columns to insert');
    }

    // Build SQL
    $colsSql = implode(', ', $columns);
    $phSql = implode(', ', $placeholders);
    $sql = "INSERT INTO employees ($colsSql) VALUES ($phSql)";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($insertData);

    // Return the newly created ID and the plain password ONLY in the immediate response
    // so the UI can show it once. The database stores only the hashed version.
    sendResponse(true, 'Employee added successfully', [ 'id' => $pdo->lastInsertId(), 'plain_password' => $password ]);

} catch (Throwable $e) {
    sendResponse(false, 'Error: ' . $e->getMessage());
}
