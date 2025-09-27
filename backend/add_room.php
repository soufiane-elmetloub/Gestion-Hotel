<?php
require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Method not allowed');
}

try {
    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) {
        sendResponse(false, 'Database connection failed');
    }

    $raw = file_get_contents('php://input');
    $payload = json_decode($raw, true);
    if (!is_array($payload)) {
        sendResponse(false, 'Invalid JSON payload');
    }

    $roomNumber = trim((string)($payload['room_number'] ?? $payload['number'] ?? ''));
    if ($roomNumber === '') {
        sendResponse(false, 'Room number is required');
    }

    // Discover available columns
    $colsStmt = $pdo->query('SHOW COLUMNS FROM rooms');
    $available = [];
    while ($r = $colsStmt->fetch()) { $available[$r['Field']] = true; }

    // Determine primary key column for potential uniqueness checks
    $pkCol = isset($available['id']) ? 'id' : (isset($available['room_id']) ? 'room_id' : null);

    // Determine number/type/capacity/price/status common column names
    $numberCol = isset($available['number']) ? 'number' : (isset($available['room_number']) ? 'room_number' : null);
    $typeCol   = isset($available['type']) ? 'type' : (isset($available['room_type']) ? 'room_type' : null);
    $capCol    = isset($available['capacity']) ? 'capacity' : (isset($available['bed_type']) ? 'bed_type' : null);
    $priceCol  = isset($available['price_per_night']) ? 'price_per_night' : (isset($available['price']) ? 'price' : null);
    $statusCol = isset($available['status']) ? 'status' : (isset($available['availability']) ? 'availability' : null);
    $amenCol   = isset($available['amenities']) ? 'amenities' : null;
    $featuresCol = isset($available['features']) ? 'features' : null;
    $floorCol  = isset($available['floor']) ? 'floor' : (isset($available['level']) ? 'level' : (isset($available['room_floor']) ? 'room_floor' : null));
    $viewCol   = isset($available['view']) ? 'view' : (isset($available['room_view']) ? 'room_view' : (isset($available['view_type']) ? 'view_type' : null));
    $codeCol   = isset($available['room_code']) ? 'room_code' : (isset($available['code']) ? 'code' : null);
    $sectionCol= isset($available['section']) ? 'section' : (isset($available['assigned_section']) ? 'assigned_section' : null);
    // Possible employee assignment columns
    $assignCol = isset($available['assigned_employee_id']) ? 'assigned_employee_id'
               : (isset($available['employee_id']) ? 'employee_id'
               : (isset($available['assigned_to']) ? 'assigned_to' : null));

    if ($numberCol === null) {
        sendResponse(false, 'No room number column found (expected number or room_number)');
    }

    // Optional uniqueness check on room number
    try {
        $dupStmt = $pdo->prepare("SELECT COUNT(*) c FROM rooms WHERE $numberCol = ?");
        $dupStmt->execute([$roomNumber]);
        if ((int)$dupStmt->fetch()['c'] > 0) {
            sendResponse(false, 'Room number already exists');
        }
    } catch (Throwable $e) { /* ignore, proceed without strict check */ }

    $columns = [];
    $placeholders = [];
    $values = [];

    // Helper - modified to handle NOT NULL columns
    $add = function(string $col, $val, $required = false) use (&$columns, &$placeholders, &$values) {
        if ($col !== null && ($val !== null && $val !== '' || $required)) {
            $columns[] = $col;
            $placeholders[] = '?';
            $values[] = $val !== null && $val !== '' ? $val : '';
        }
    };

    // required
    $add($numberCol, $roomNumber);
    // optional columns only if they exist
    if ($typeCol)   $add($typeCol, trim((string)($payload['room_type'] ?? $payload['type'] ?? '')));
    if ($capCol)    $add($capCol, trim((string)($payload['capacity'] ?? '')));
    if ($priceCol)  $add($priceCol, trim((string)($payload['price_per_night'] ?? $payload['price'] ?? '')));
    if ($statusCol) $add($statusCol, trim((string)($payload['status'] ?? 'available')));
    if ($amenCol) $add($amenCol, trim((string)($payload['amenities'] ?? '')));
    if ($featuresCol) {
        $features = trim((string)($payload['features'] ?? ''));
        if ($features !== '') $add($featuresCol, $features);
    }
    if ($floorCol) {
        $floor = trim((string)($payload['floor'] ?? ''));
        if ($floor !== '') $add($floorCol, $floor);
    }
    if ($viewCol) {
        $view = trim((string)($payload['view'] ?? ''));
        if ($view !== '') $add($viewCol, $view);
    }
    // Force room_code and section since they're NOT NULL in your schema
    if ($codeCol) $add($codeCol, trim((string)($payload['room_code'] ?? $payload['code'] ?? '')), true);
    if ($sectionCol) $add($sectionCol, trim((string)($payload['section'] ?? $payload['assigned_section'] ?? '')), true);
    
    // Handle employee_id (your schema uses employee_id, not assigned_employee_id)
    $empCol = isset($available['employee_id']) ? 'employee_id' : $assignCol;
    if ($empCol) {
        $emp = trim((string)($payload['assigned_employee_id'] ?? $payload['employee_id'] ?? ''));
        if ($emp !== '') $add($empCol, $emp);
    }

    if (isset($available['created_at'])) {
        $columns[] = 'created_at';
        $placeholders[] = 'NOW()';
    }

    if (empty($columns)) {
        sendResponse(false, 'No valid columns to insert');
    }

    $sql = 'INSERT INTO rooms (' . implode(', ', $columns) . ') VALUES (' . implode(', ', $placeholders) . ')';
    $stmt = $pdo->prepare($sql);
    $stmt->execute($values);

    $newId = $pdo->lastInsertId();
    sendResponse(true, 'Room added successfully', ['id' => $newId]);

} catch (Throwable $e) {
    sendResponse(false, 'Error: ' . $e->getMessage());
}
