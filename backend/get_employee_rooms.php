<?php
require_once __DIR__ . '/config.php';

try {
    $database = new Database();
    $db = $database->getConnection();
    if (!$db) {
        sendResponse(false, 'Database connection failed.');
    }

    // Get employee_id from GET or JSON body
    $employeeId = null;
    if (isset($_GET['employee_id']) && $_GET['employee_id'] !== '') {
        $employeeId = (int)$_GET['employee_id'];
    } else {
        $raw = file_get_contents('php://input');
        if ($raw) {
            $payload = json_decode($raw, true);
            if (is_array($payload) && isset($payload['employee_id'])) {
                $employeeId = (int)$payload['employee_id'];
            }
        }
    }

    if (!$employeeId) {
        sendResponse(false, 'Missing or invalid employee_id.');
    }

    // Detect schema columns
    $cols = [];
    $stmtCols = $db->query('SHOW COLUMNS FROM rooms');
    foreach ($stmtCols->fetchAll() as $c) {
        if (isset($c['Field'])) { $cols[$c['Field']] = true; }
    }

    // Determine column names
    $idCol = isset($cols['id']) ? 'id' : (isset($cols['room_id']) ? 'room_id' : 'id');
    $numCol = isset($cols['room_number']) ? 'room_number' : (isset($cols['number']) ? 'number' : (isset($cols['room_no']) ? 'room_no' : null));
    $typeCol = isset($cols['room_type']) ? 'room_type' : (isset($cols['type']) ? 'type' : null);
    $codeCol = isset($cols['room_code']) ? 'room_code' : (isset($cols['code']) ? 'code' : null);
    $assignCol = isset($cols['assigned_employee_id']) ? 'assigned_employee_id'
               : (isset($cols['employee_id']) ? 'employee_id'
               : (isset($cols['assigned_to']) ? 'assigned_to' : null));
    $capacityCol = isset($cols['capacity']) ? 'capacity' : (isset($cols['bed_type']) ? 'bed_type' : null);
    $sectionCol = isset($cols['section']) ? 'section' : (isset($cols['assigned_section']) ? 'assigned_section' : null);
    $statusCol = isset($cols['status']) ? 'status' : (isset($cols['availability']) ? 'availability' : null);

    if ($assignCol === null) {
        sendResponse(true, 'No assignment column found on rooms table.', ['rooms' => [], 'count' => 0]);
    }

    // Build SELECT with safe aliases
    $selectParts = [];
    $selectParts[] = "r.`$idCol` AS id";
    if ($numCol) { $selectParts[] = "r.`$numCol` AS room_number"; }
    if ($typeCol) { $selectParts[] = "r.`$typeCol` AS room_type"; }
    if ($codeCol) { $selectParts[] = "r.`$codeCol` AS room_code"; }
    if ($capacityCol) { $selectParts[] = "r.`$capacityCol` AS capacity"; }
    if ($sectionCol) { $selectParts[] = "r.`$sectionCol` AS section"; }
    if ($statusCol) { $selectParts[] = "r.`$statusCol` AS status"; }
    $selectParts[] = "r.`$assignCol` AS assigned_employee_id";

    $orderCol = $numCol ? $numCol : $idCol;
    $sql = 'SELECT ' . implode(', ', $selectParts) . " FROM rooms r WHERE r.`$assignCol` = :eid ORDER BY r.`$orderCol` ASC";
    $stmt = $db->prepare($sql);
    $stmt->bindValue(':eid', $employeeId, PDO::PARAM_INT);
    $stmt->execute();
    $rooms = $stmt->fetchAll();

    sendResponse(true, 'Rooms fetched successfully.', ['rooms' => $rooms, 'count' => count($rooms)]);
} catch (Throwable $e) {
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
