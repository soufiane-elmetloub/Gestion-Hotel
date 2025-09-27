<?php
// Include database configuration first (without headers)
require_once 'config.php';

// Set headers after config
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Create database connection
$database = new Database();
$pdo = $database->getConnection();

if (!$pdo) {
    throw new Exception('Database connection failed');
}

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    if (!$input) {
        throw new Exception('Invalid JSON input');
    }
    
    // Validate required fields for rooms table
    if (empty($input['room_number'])) {
        throw new Exception('Room number is required');
    }
    
    // Start transaction
    $pdo->beginTransaction();
    
    // Prepare data for rooms table
    $room_data = [
        'room_code' => $input['room_code'] ?? '',
        'section' => $input['section'] ?? '',
        'room_number' => (int)$input['room_number'],
        'room_type' => $input['room_type'] ?? 'Standard',
        'status' => $input['status'] ?? 'Available',
        'employee_id' => !empty($input['employee_id']) ? (int)$input['employee_id'] : null,
        'capacity' => $input['capacity'] ?? 'Single'
    ];
    
    // Insert into rooms table
    $room_sql = "INSERT INTO rooms (room_code, section, room_number, room_type, status, employee_id, capacity) 
                 VALUES (:room_code, :section, :room_number, :room_type, :status, :employee_id, :capacity)";
    
    $room_stmt = $pdo->prepare($room_sql);
    $room_stmt->execute($room_data);
    
    // Get the inserted room ID
    $room_id = $pdo->lastInsertId();
    
    // Prepare data for room_full_details table
    $details_data = [
        'room_id' => $room_id,
        'price_per_night' => !empty($input['price_per_night']) ? (float)$input['price_per_night'] : 0.00,
        'floor' => !empty($input['floor']) ? (int)$input['floor'] : 1,
        'view' => $input['view'] ?? 'City',
        'features' => $input['features'] ?? '[]'
    ];
    
    // If features is a string (comma-separated), convert to JSON array
    if (is_string($details_data['features']) && !empty($details_data['features'])) {
        // Split by comma and create JSON array
        $features_array = array_map('trim', explode(',', $details_data['features']));
        $details_data['features'] = json_encode($features_array);
    } elseif (is_array($details_data['features'])) {
        $details_data['features'] = json_encode($details_data['features']);
    } elseif (empty($details_data['features'])) {
        $details_data['features'] = '[]';
    }
    
    // Insert into room_full_details table
    $details_sql = "INSERT INTO room_full_details (room_id, price_per_night, floor, view, features) 
                    VALUES (:room_id, :price_per_night, :floor, :view, :features)";
    
    $details_stmt = $pdo->prepare($details_sql);
    $details_stmt->execute($details_data);
    
    // Commit transaction
    $pdo->commit();
    
    // Return success response
    echo json_encode([
        'success' => true,
        'message' => 'Room added successfully to both tables',
        'room_id' => $room_id,
        'data' => [
            'rooms_table' => $room_data,
            'room_full_details_table' => $details_data
        ]
    ]);
    
} catch (PDOException $e) {
    // Rollback transaction on database error
    if ($pdo->inTransaction()) {
        $pdo->rollback();
    }
    
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ]);
    
} catch (Exception $e) {
    // Rollback transaction on general error
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollback();
    }
    
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage()
    ]);
}
?>
