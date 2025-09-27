<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once __DIR__ . '/config.php';

$response = [
    'success' => false,
    'rooms' => [],
    'error' => null
];

try {
    // Create database connection
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Database connection failed');
    }

    // Optional filter by type (e.g., ?type=Standard)
    $typeFilter = isset($_GET['type']) ? trim($_GET['type']) : '';
    $debugMode = isset($_GET['debug']) && $_GET['debug'] == '1';

    // We'll try a sequence of queries to adapt to different possible column names
    $queries = [];
    
    // If a view/table `room_full_details` exists (from your SQL), prefer fetching from it to include price directly
    try {
        $rfColsStmt = $pdo->query('DESCRIBE room_full_details');
        $rfCols = [];
        foreach ($rfColsStmt->fetchAll() as $c) { if (isset($c['Field'])) $rfCols[$c['Field']] = true; }
        // room_full_details exists, add high-priority queries that include price
        $pref = [];
        // Common mapping variants
        $pref[] = [ 'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status, price_per_night AS price FROM room_full_details', 'params' => [] ];
        $pref[] = [ 'sql' => 'SELECT room_no AS number, room_type AS type, capacity, status, price_per_night AS price FROM room_full_details', 'params' => [] ];
        $pref[] = [ 'sql' => 'SELECT number, type, capacity, status, price_per_night AS price FROM room_full_details', 'params' => [] ];
        $pref[] = [ 'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status, price AS price FROM room_full_details', 'params' => [] ];
        $pref[] = [ 'sql' => 'SELECT number, type, capacity, status, price AS price FROM room_full_details', 'params' => [] ];
        $queries = array_merge($pref, $queries);

        // Prepare direct price lookup by room_id from room_full_details if columns exist
        $rfPkCol = isset($rfCols['room_id']) ? 'room_id' : (isset($rfCols['id']) ? 'id' : null);
        $rfPriceCol = isset($rfCols['price_per_night']) ? 'price_per_night' : (isset($rfCols['price']) ? 'price' : null);
        $stmtPriceByPk = null;
        if ($rfPkCol && $rfPriceCol) {
            $sqlRF = 'SELECT ' . $rfPriceCol . ' AS price FROM room_full_details WHERE ' . $rfPkCol . ' = :pk LIMIT 1';
            try { $stmtPriceByPk = $pdo->prepare($sqlRF); } catch (Throwable $e) { $stmtPriceByPk = null; }
        }
    } catch (Throwable $e) {
        // table/view does not exist; continue with default rooms queries
    }

    // 1) Standard naming
    $queries[] = [
        'sql' => 'SELECT number, type, capacity, status FROM rooms',
        'params' => []
    ];

    // 1.b) Standard naming + price_per_night
    $queries[] = [
        'sql' => 'SELECT number, type, capacity, status, price_per_night AS price FROM rooms',
        'params' => []
    ];

    // 1.c) Standard naming + price
    $queries[] = [
        'sql' => 'SELECT number, type, capacity, status, price AS price FROM rooms',
        'params' => []
    ];

    // 2) Common alt naming (room_* columns)
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status FROM rooms',
        'params' => []
    ];

    // 2.b) Alt naming + price_per_night
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status, price_per_night AS price FROM rooms',
        'params' => []
    ];

    // 2.c) Alt naming + price
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status, price AS price FROM rooms',
        'params' => []
    ];

    // 2.d) Alt naming + room_price
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status, room_price AS price FROM rooms',
        'params' => []
    ];

    // 2.e) Alt naming + nightly_rate
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, capacity, status, nightly_rate AS price FROM rooms',
        'params' => []
    ];

    // 3) Alternative capacity/status columns
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, bed_type AS capacity, availability AS status FROM rooms',
        'params' => []
    ];

    // 3.b) Alternative capacity/status + price variations
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, bed_type AS capacity, availability AS status, price_per_night AS price FROM rooms',
        'params' => []
    ];
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, bed_type AS capacity, availability AS status, price AS price FROM rooms',
        'params' => []
    ];
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, bed_type AS capacity, availability AS status, room_price AS price FROM rooms',
        'params' => []
    ];
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, bed_type AS capacity, availability AS status, nightly_rate AS price FROM rooms',
        'params' => []
    ];

    // 4) Derive status from boolean is_occupied
    $queries[] = [
        'sql' => 'SELECT room_number AS number, room_type AS type, bed_type AS capacity, CASE WHEN is_occupied=1 THEN "Occupied" ELSE "Available" END AS status FROM rooms',
        'params' => []
    ];

    $data = null;
    $lastError = null;

    foreach ($queries as $q) {
        try {
            $sql = $q['sql'];
            // Apply optional type filter if present
            if ($typeFilter !== '') {
                // try to filter by the alias `type`
                $sql = "SELECT * FROM (" . $sql . ") t WHERE t.type = :type";
                $stmt = $pdo->prepare($sql);
                $stmt->bindValue(':type', $typeFilter);
                $stmt->execute();
            } else {
                $stmt = $pdo->query($sql);
            }
            $rows = $stmt->fetchAll();
            // Ensure required keys exist
            if (!empty($rows) && array_key_exists('number', $rows[0]) && array_key_exists('type', $rows[0]) && array_key_exists('capacity', $rows[0]) && array_key_exists('status', $rows[0])) {
                $data = $rows;
                break;
            }
        } catch (Throwable $e) {
            $lastError = $e->getMessage();
            // Try next query
        }
    }

    if ($data === null) {
        throw new Exception('Could not map columns. Please ensure table `rooms` has (number/type/capacity/status) or compatible aliases. Last error: ' . ($lastError ?? '')); 
    }

    // Discover available PK and number columns in rooms for reliable ID mapping
    $roomCols = [];
    try {
        $colsStmt = $pdo->query("SHOW COLUMNS FROM rooms");
        foreach ($colsStmt->fetchAll() as $c) {
            if (isset($c['Field'])) $roomCols[$c['Field']] = true;
        }
    } catch (Throwable $e) {
        // ignore, we'll fallback
    }
    $pkCol = isset($roomCols['id']) ? 'id' : (isset($roomCols['room_id']) ? 'room_id' : null);
    $numCol1 = isset($roomCols['number']) ? 'number' : null;
    $numCol2 = isset($roomCols['room_number']) ? 'room_number' : null;
    $codeCol = isset($roomCols['room_code']) ? 'room_code' : (isset($roomCols['code']) ? 'code' : null);
    $sectionCol = isset($roomCols['section']) ? 'section' : (isset($roomCols['assigned_section']) ? 'assigned_section' : null);
    // Detect price column
    $priceCol = isset($roomCols['price_per_night']) ? 'price_per_night'
              : (isset($roomCols['price']) ? 'price'
              : (isset($roomCols['room_price']) ? 'room_price'
              : (isset($roomCols['nightly_rate']) ? 'nightly_rate' : null)));
    $assignCol = isset($roomCols['assigned_employee_id']) ? 'assigned_employee_id'
                : (isset($roomCols['employee_id']) ? 'employee_id'
                : (isset($roomCols['assigned_to']) ? 'assigned_to' : null));

    // Prepare lookup statements if possible
    $stmtByNumber = null;
    $stmtExtrasByPk = null;
    $stmtExtrasByNum = null;
    // room_full_details direct price by pk (if prepared above)
    if (!isset($stmtPriceByPk)) { $stmtPriceByPk = null; }
    if ($pkCol && ($numCol1 || $numCol2)) {
        $whereNumber = [];
        if ($numCol1) $whereNumber[] = 'rooms.' . $numCol1 . ' = :num';
        if ($numCol2) $whereNumber[] = 'rooms.' . $numCol2 . ' = :num';
        $sqlFind = 'SELECT ' . $pkCol . ' AS pk FROM rooms WHERE ' . implode(' OR ' , $whereNumber) . ' LIMIT 1';
        try { $stmtByNumber = $pdo->prepare($sqlFind); } catch (Throwable $e) { $stmtByNumber = null; }
    }

    // Prepare extra fields fetchers (room_code, section, employee assignment, price)
    if ($pkCol && ($codeCol || $sectionCol || $assignCol || $priceCol)) {
        $selects = [];
        if ($codeCol) $selects[] = $codeCol . ' AS room_code_ex';
        if ($sectionCol) $selects[] = $sectionCol . ' AS section_ex';
        if ($assignCol) $selects[] = $assignCol . ' AS assign_ex';
        if ($priceCol) $selects[] = $priceCol . ' AS price_ex';
        if (!empty($selects)) {
            $sqlEx = 'SELECT ' . implode(', ', $selects) . ' FROM rooms WHERE ' . $pkCol . ' = :pk LIMIT 1';
            try { $stmtExtrasByPk = $pdo->prepare($sqlEx); } catch (Throwable $e) { $stmtExtrasByPk = null; }
        }
    } elseif (($numCol1 || $numCol2) && ($codeCol || $sectionCol || $assignCol || $priceCol)) {
        $selects = [];
        if ($codeCol) $selects[] = $codeCol . ' AS room_code_ex';
        if ($sectionCol) $selects[] = $sectionCol . ' AS section_ex';
        if ($assignCol) $selects[] = $assignCol . ' AS assign_ex';
        if ($priceCol) $selects[] = $priceCol . ' AS price_ex';
        if (!empty($selects)) {
            $whereNumber = [];
            if ($numCol1) $whereNumber[] = $numCol1 . ' = :num';
            if ($numCol2) $whereNumber[] = $numCol2 . ' = :num';
            $sqlExNum = 'SELECT ' . implode(', ', $selects) . ' FROM rooms WHERE ' . implode(' OR ', $whereNumber) . ' LIMIT 1';
            try { $stmtExtrasByNum = $pdo->prepare($sqlExNum); } catch (Throwable $e) { $stmtExtrasByNum = null; }
        }
    }

    // Normalize values
    $normalized = [];
    foreach ($data as $r) {
        $num = isset($r['number']) ? (string)$r['number'] : '';
        $type = isset($r['type']) ? (string)$r['type'] : '';
        $cap = isset($r['capacity']) ? (string)$r['capacity'] : '';
        $statusRaw = isset($r['status']) ? trim((string)$r['status']) : '';
        $priceFromQuery = isset($r['price']) ? (string)$r['price'] : '';

        // Keep original status as stored in DB (do not normalize display text)
        $status = $statusRaw !== '' ? $statusRaw : 'Available';

        $normalized[] = [
            'number' => $num,
            'type' => $type,
            'capacity' => $cap,
            'status' => $status,
            'price' => $priceFromQuery,
        ];
    }

    // Map to the structure expected by Android ui.rooms.RoomsFragment
    $roomsOut = [];
    foreach ($normalized as $idx => $r) {
        // Determine availability for UI color/flag, but keep original status text
        $low = mb_strtolower((string)$r['status']);
        $isAvailable = in_array($low, ['0','available','متاحة','فارغة','empty'], true);
        // pick background color similar to the app design
        $statusColor = $isAvailable ? '#E8F5E8' : '#FFE8E8';

        // derive occupancy from capacity text when possible
        $capText = strtolower(trim($r['capacity']));
        $maxOcc = 1;
        if ($capText === 'double' || $capText === '2' || $capText === 'زوجي' || $capText === 'مزدوج') {
            $maxOcc = 2;
        }

        // Resolve real PK by room number if possible
        $realId = $idx + 1; // fallback sequential
        if ($stmtByNumber && $r['number'] !== '') {
            try {
                $stmtByNumber->bindValue(':num', $r['number']);
                $stmtByNumber->execute();
                $pkRow = $stmtByNumber->fetch();
                if ($pkRow && isset($pkRow['pk'])) {
                    $realId = (int)$pkRow['pk'];
                }
            } catch (Throwable $e) { /* ignore */ }
        }

        // Fetch extra fields if possible
        $roomCodeEx = '';
        $sectionEx = '';
        $assignEx = '';
        $priceEx = '';
        if ($stmtExtrasByPk) {
            try { $stmtExtrasByPk->bindValue(':pk', $realId, PDO::PARAM_INT); $stmtExtrasByPk->execute(); $ex = $stmtExtrasByPk->fetch();
                if ($ex) { $roomCodeEx = isset($ex['room_code_ex']) ? (string)$ex['room_code_ex'] : ''; $sectionEx = isset($ex['section_ex']) ? (string)$ex['section_ex'] : ''; $assignEx = isset($ex['assign_ex']) ? (string)$ex['assign_ex'] : ''; $priceEx = isset($ex['price_ex']) ? (string)$ex['price_ex'] : ''; }
            } catch (Throwable $e) { /* ignore */ }
        } elseif ($stmtExtrasByNum && $r['number'] !== '') {
            try { $stmtExtrasByNum->bindValue(':num', $r['number']); $stmtExtrasByNum->execute(); $ex = $stmtExtrasByNum->fetch();
                if ($ex) { $roomCodeEx = isset($ex['room_code_ex']) ? (string)$ex['room_code_ex'] : ''; $sectionEx = isset($ex['section_ex']) ? (string)$ex['section_ex'] : ''; $assignEx = isset($ex['assign_ex']) ? (string)$ex['assign_ex'] : ''; $priceEx = isset($ex['price_ex']) ? (string)$ex['price_ex'] : ''; }
            } catch (Throwable $e) { /* ignore */ }
        }

        // Choose price from (1) room_full_details by room_id, (2) extras, (3) base query, else 0
        $priceNumeric = 0.0;
        $priceSource = '';
        if ($stmtPriceByPk && $realId) {
            try {
                $stmtPriceByPk->bindValue(':pk', $realId, PDO::PARAM_INT);
                $stmtPriceByPk->execute();
                $rfRow = $stmtPriceByPk->fetch();
                if ($rfRow && isset($rfRow['price']) && $rfRow['price'] !== null && $rfRow['price'] !== '') {
                    $priceSource = (string)$rfRow['price'];
                }
            } catch (Throwable $e) { /* ignore */ }
        }
        if ($priceSource === '') {
            $priceSource = $priceEx !== '' ? $priceEx : (isset($r['price']) ? $r['price'] : '');
        }
        if (is_numeric($priceSource)) {
            $priceNumeric = (float)$priceSource;
        } elseif (is_string($priceSource)) {
            $clean = preg_replace('/[^0-9.\-]/', '', $priceSource);
            if ($clean !== '' && is_numeric($clean)) $priceNumeric = (float)$clean;
        }

        $roomsOut[] = [
            'id' => $realId,
            'room_number' => (string)$r['number'],
            'room_type' => (string)$r['type'],
            'floor' => 0,
            'price_per_night' => $priceNumeric,
            'max_occupancy' => $maxOcc,
            'capacity' => (string)$r['capacity'],
            'room_code' => $roomCodeEx,
            'section' => $sectionEx,
            'assigned_employee_id' => $assignEx,
            'description_ar' => '',
            // Keep status exactly as stored in DB
            'status' => (string)$r['status'],
            'status_color' => $statusColor,
            'is_available' => $isAvailable
        ];
    }

    // Optional filtering by employee_id and/or section so each employee only sees their rooms
    $employeeIdParam = isset($_GET['employee_id']) ? trim((string)$_GET['employee_id'])
                     : (isset($_GET['emp_id']) ? trim((string)$_GET['emp_id']) : '');
    $sectionParam = isset($_GET['section']) ? trim((string)$_GET['section']) : '';

    $roomsFiltered = $roomsOut;
    if ($employeeIdParam !== '' || $sectionParam !== '') {
        $roomsFiltered = array_values(array_filter($roomsOut, function($r) use ($employeeIdParam, $sectionParam) {
            $ok = true;
            if ($employeeIdParam !== '') {
                // Accept both numeric and string ids stored in DB
                $assignedRaw = isset($r['assigned_employee_id']) ? $r['assigned_employee_id'] : '';
                $assigned = trim((string)$assignedRaw);
                $emp = trim((string)$employeeIdParam);
                $match = false;
                if ($assigned !== '' && $assigned === $emp) {
                    $match = true;
                } else if (is_numeric($assigned) && is_numeric($emp)) {
                    $match = ((int)$assigned === (int)$emp);
                }
                $ok = $ok && $match;
            }
            if ($sectionParam !== '') {
                $sec = isset($r['section']) ? trim((string)$r['section']) : '';
                $ok = $ok && ($sec !== '' && strcasecmp($sec, $sectionParam) === 0);
            }
            return $ok;
        }));
    }

    $response['success'] = true;
    $response['rooms'] = $roomsFiltered;
    if ($debugMode) {
        $response['debug'] = [
            'price_column_detected' => $priceCol ?? null,
            'used_view_room_full_details' => isset($pref),
            'sample_price_values' => array_slice(array_map(function($r){ return $r['price_per_night']; }, $roomsFiltered), 0, 5),
            'room_full_details_lookup' => [
                'rf_pk_used' => isset($rfPkCol) ? $rfPkCol : null,
                'rf_price_col' => isset($rfPriceCol) ? $rfPriceCol : null,
                'rf_lookup_enabled' => (bool)$stmtPriceByPk,
            ],
            'filter' => [
                'employee_id' => $employeeIdParam,
                'section' => $sectionParam,
                'total_before' => count($roomsOut),
                'total_after' => count($roomsFiltered)
            ]
        ];
    }
    // Compatibility alias for UIs expecting `data`
    $response['data'] = $roomsFiltered;
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();

} catch (Throwable $e) {
    http_response_code(500);
    $response['error'] = $e->getMessage();
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();
}
