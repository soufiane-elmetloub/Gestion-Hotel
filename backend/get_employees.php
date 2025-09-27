<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'config.php';

try {
    // Create database connection
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Database connection failed');
    }
    
    // First check what columns exist in the employees table
    $columns_result = $pdo->query("SHOW COLUMNS FROM employees");
    $available_columns = array();
    while($col = $columns_result->fetch(PDO::FETCH_ASSOC)) {
        $available_columns[] = $col['Field'];
    }
    
    // Build SQL query based on available columns
    $select_columns = array('id', 'username', 'created_at');
    
    // Add optional columns if they exist
    if (in_array('first_name', $available_columns)) $select_columns[] = 'first_name';
    if (in_array('last_name', $available_columns)) $select_columns[] = 'last_name';
    if (in_array('phone', $available_columns)) $select_columns[] = 'phone';
    // Prefer department over role if exists
    if (in_array('department', $available_columns)) $select_columns[] = 'department';
    if (in_array('role', $available_columns)) $select_columns[] = 'role';
    if (in_array('email', $available_columns)) $select_columns[] = 'email';
    if (in_array('status', $available_columns)) $select_columns[] = 'status';
    if (in_array('assigned_section', $available_columns)) $select_columns[] = 'assigned_section';
    
    $sql = "SELECT " . implode(', ', $select_columns) . " FROM employees ORDER BY created_at DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $employees = array();
    
    foreach($result as $row) {
        $employees[] = array(
            'id' => $row['id'],
            'first_name' => isset($row['first_name']) ? $row['first_name'] : 'N/A',
            'last_name' => isset($row['last_name']) ? $row['last_name'] : 'N/A',
            'username' => $row['username'],
            'phone' => isset($row['phone']) ? $row['phone'] : 'N/A',
            // Provide department if available, otherwise fallback to role
            'department' => isset($row['department']) ? $row['department'] : (isset($row['role']) ? $row['role'] : 'Employee'),
            'role' => isset($row['role']) ? $row['role'] : null,
            'email' => isset($row['email']) ? $row['email'] : null,
            'status' => isset($row['status']) ? $row['status'] : 'active',
            'assigned_section' => isset($row['assigned_section']) ? $row['assigned_section'] : 'N/A',
            'created_at' => $row['created_at']
        );
    }
    
    // Return success response
    echo json_encode(array(
        'success' => true,
        'data' => $employees,
        'total' => count($employees)
    ));
    
} catch (Exception $e) {
    // Return error response
    echo json_encode(array(
        'success' => false,
        'error' => $e->getMessage()
    ));
}
?>
