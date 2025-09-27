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

    $id = (int)($payload['id'] ?? 0);
    if ($id <= 0) {
        sendResponse(false, 'Valid room ID is required');
    }

    // Discover columns in rooms table
    $colsStmt = $pdo->query('SHOW COLUMNS FROM rooms');
    $available = [];
    while ($r = $colsStmt->fetch()) { $available[$r['Field']] = true; }

    // Determine PK and common column names
    $pkCol     = isset($available['id']) ? 'id' : (isset($available['room_id']) ? 'room_id' : null);
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
    $assignCol = isset($available['assigned_employee_id']) ? 'assigned_employee_id'
               : (isset($available['employee_id']) ? 'employee_id'
               : (isset($available['assigned_to']) ? 'assigned_to' : null));

    if ($pkCol === null) {
        sendResponse(false, 'Primary key column not found (expected id or room_id)');
    }

    // Check room exists
    $chk = $pdo->prepare("SELECT COUNT(*) c FROM rooms WHERE $pkCol = ?");
    $chk->execute([$id]);
    if ((int)$chk->fetch()['c'] === 0) {
        sendResponse(false, 'Room not found');
    }

    $setParts = [];
    $values = [];
    $add = function(string $col, $val, $required = false) use (&$setParts, &$values) {
        if ($col !== null && ($val !== null && $val !== '' || $required)) {
            $setParts[] = $col . ' = ?';
            $values[] = $val !== null && $val !== '' ? $val : '';
        }
    };

    $add($numberCol, trim((string)($payload['room_number'] ?? $payload['number'] ?? '')));
    $add($typeCol, trim((string)($payload['room_type'] ?? $payload['type'] ?? '')));
    $add($capCol, trim((string)($payload['capacity'] ?? '')));
    $add($priceCol, trim((string)($payload['price_per_night'] ?? $payload['price'] ?? '')));
    $add($statusCol, trim((string)($payload['status'] ?? '')));
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

    if (empty($setParts)) {
        sendResponse(false, 'No valid fields to update');
    }

    $sql = 'UPDATE rooms SET ' . implode(', ', $setParts) . ' WHERE ' . $pkCol . ' = ?';
    $values[] = $id;

    $stmt = $pdo->prepare($sql);
    $stmt->execute($values);

    sendResponse(true, 'Room updated successfully');

} catch (Throwable $e) {
    sendResponse(false, 'Error: ' . $e->getMessage());
}
