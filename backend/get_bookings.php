<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once __DIR__ . '/config.php';

try {
    // Create database connection
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Database connection failed');
    }

    // Optional filters
    $activeOnly = isset($_GET['active_only']) && (int)$_GET['active_only'] === 1; // not ended
    $endedOnly  = isset($_GET['ended_only']) && (int)$_GET['ended_only'] === 1;   // already ended
    $checkInDate = isset($_GET['check_in_date']) ? trim($_GET['check_in_date']) : '';
    $searchName = isset($_GET['search_name']) ? trim($_GET['search_name']) : '';
    $paymentFilter = isset($_GET['payment_filter']) ? trim($_GET['payment_filter']) : '';
    $employeeId = isset($_GET['employee_id']) ? (int)$_GET['employee_id'] : 0; // filter by assigned employee rooms
    $params = [];

    // Detect if rooms.assigned_employee_id column exists (for robust filtering)
    $hasAssignedColumn = false;
    try {
        $colStmt = $pdo->prepare("SELECT COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rooms' AND COLUMN_NAME = 'assigned_employee_id'");
        $colStmt->execute();
        $hasAssignedColumn = (int)$colStmt->fetchColumn() > 0;
    } catch (Exception $ignored) { $hasAssignedColumn = false; }

    // Build dynamic WHERE conditions
    $conditions = [];
    if ($endedOnly) {
        $conditions[] = 'r.check_out < CURDATE()';
    } elseif ($activeOnly) {
        $conditions[] = 'r.check_out >= CURDATE()';
    }
    if ($checkInDate !== '' && preg_match('/^\\d{4}-\\d{2}-\\d{2}$/', $checkInDate)) {
        $conditions[] = 'DATE(r.check_in) = :check_in_date';
        $params[':check_in_date'] = $checkInDate;
    }
    if ($searchName !== '') {
        $conditions[] = '(c.first_name LIKE :search_name OR c.last_name LIKE :search_name OR CONCAT(c.first_name, " ", c.last_name) LIKE :search_name)';
        $params[':search_name'] = '%' . $searchName . '%';
    }
    if ($paymentFilter !== '' && $paymentFilter !== 'ALL') {
        $conditions[] = 'r.payment_status = :payment_filter';
        $params[':payment_filter'] = strtolower($paymentFilter);
    }
    // If employee_id is provided, restrict to that employee's scope
    if ($employeeId > 0) {
        if ($hasAssignedColumn) {
            // Preferred: by rooms assignment
            $conditions[] = 'rm.assigned_employee_id = :employee_id';
        } else {
            // Fallback: by reservation creator/owner
            $conditions[] = 'r.employee_id = :employee_id';
        }
        $params[':employee_id'] = $employeeId;
    }

    // Fetch reservations joined with clients, room details, and employee who created the booking
    $sql = "SELECT r.id, r.room_id, r.client_id, r.number_of_guests, r.check_in, r.check_out,
                   r.price_per_night, r.total_amount, r.employee_id, r.status, r.payment_status, r.created_at,
                   c.first_name, c.last_name, rm.room_number, rm.room_code,
                   e.first_name AS emp_first_name, e.last_name AS emp_last_name, e.username AS emp_username
            FROM reservations r
            LEFT JOIN clients c ON c.id = r.client_id
            LEFT JOIN rooms rm ON rm.id = r.room_id
            LEFT JOIN employees e ON e.id = r.employee_id";
    if (!empty($conditions)) {
        $sql .= ' WHERE ' . implode(' AND ', $conditions);
    }
    $sql .= ' ORDER BY r.created_at DESC';

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $rows = $stmt->fetchAll();

    $data = [];
    foreach ($rows as $row) {
        // Use actual room_number if available, otherwise fallback to room_id
        $roomDisplay = $row['room_number'] ? (string)$row['room_number'] : (string)$row['room_id'];
        // Build employee display name (First Last) or fallback to username or employee_id
        $empFirst = $row['emp_first_name'] ?? '';
        $empLast = $row['emp_last_name'] ?? '';
        $empUser = $row['emp_username'] ?? '';
        $empName = trim($empFirst . ' ' . $empLast);
        if ($empName === '' && $empUser !== '') $empName = (string)$empUser;
        if ($empName === '' && isset($row['employee_id'])) $empName = (string)$row['employee_id'];
        
        $data[] = [
            'id' => (int)$row['id'],
            'first_name' => $row['first_name'] ?? '',
            'last_name' => $row['last_name'] ?? '',
            'check_in' => $row['check_in'],
            'check_out' => $row['check_out'],
            'room_number' => $roomDisplay,
            'number_of_guests' => (int)$row['number_of_guests'],
            'status' => $row['status'],
            'payment_status' => $row['payment_status'],
            'total_amount' => (string)$row['total_amount'],
            'employee_name' => $empName,
            'employee_id' => isset($row['employee_id']) ? (int)$row['employee_id'] : null
        ];
    }

    echo json_encode([
        'success' => true,
        'message' => 'Bookings fetched successfully',
        'data' => $data
    ]);

} catch (Exception $e) {
    error_log('get_bookings error: ' . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage(),
        'data' => []
    ]);
}
