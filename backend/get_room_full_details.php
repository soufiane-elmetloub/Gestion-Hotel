<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

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
    // Read input room IDs from GET (?room_ids=81,82) or JSON body {"room_ids":[81,82]}
    $roomIds = [];
    if (isset($_GET['room_ids'])) {
        $raw = $_GET['room_ids'];
        if (is_string($raw)) {
            foreach (explode(',', $raw) as $v) {
                $v = trim($v);
                if ($v !== '' && ctype_digit($v)) {
                    $roomIds[] = (int)$v;
                }
            }
        }
    } else {
        $body = file_get_contents('php://input');
        if ($body) {
            $json = json_decode($body, true);
            if (json_last_error() === JSON_ERROR_NONE && isset($json['room_ids']) && is_array($json['room_ids'])) {
                foreach ($json['room_ids'] as $v) {
                    if (is_numeric($v)) {
                        $roomIds[] = (int)$v;
                    }
                }
            }
        }
    }

    if (empty($roomIds)) {
        // If none provided, return all details
        $modeAll = true;
    } else {
        $modeAll = false;
        // Deduplicate
        $roomIds = array_values(array_unique($roomIds));
    }

    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $pdo->exec("SET NAMES utf8mb4");

    // Detect if rooms table exists and which columns are available
    $hasRooms = false;
    $roomCols = [];
    try {
        $chk = $pdo->query("SHOW TABLES LIKE 'rooms'");
        $hasRooms = $chk && $chk->rowCount() > 0;
        if ($hasRooms) {
            $colsStmt = $pdo->query("SHOW COLUMNS FROM rooms");
            foreach ($colsStmt->fetchAll() as $c) {
                if (isset($c['Field'])) $roomCols[$c['Field']] = true;
            }
        }
    } catch (Throwable $e) {
        $hasRooms = false;
    }

    // Build SELECT list safely based on available columns
    $select = [
        'd.id', 'd.room_id', 'd.price_per_night', 'd.floor', 'd.view', 'd.features'
    ];
    $join = '';
    $roomIdCol = null;
    if ($hasRooms) {
        // Detect PK column to join on
        if (isset($roomCols['id'])) $roomIdCol = 'id';
        elseif (isset($roomCols['room_id'])) $roomIdCol = 'room_id';

        $join = $roomIdCol ? (' LEFT JOIN rooms r ON r.' . $roomIdCol . ' = d.room_id') : '';
        if (isset($roomCols['number'])) $select[] = 'r.number AS number_std';
        if (isset($roomCols['room_number'])) $select[] = 'r.room_number AS number_alt';
        if (isset($roomCols['type'])) $select[] = 'r.type AS type_std';
        if (isset($roomCols['room_type'])) $select[] = 'r.room_type AS type_alt';
        if (isset($roomCols['capacity'])) $select[] = 'r.capacity AS capacity_std';
        if (isset($roomCols['bed_type'])) $select[] = 'r.bed_type AS capacity_alt';
        if (isset($roomCols['status'])) $select[] = 'r.status AS status_std';
        if (isset($roomCols['availability'])) $select[] = 'r.availability AS status_alt';
        if (isset($roomCols['is_occupied'])) $select[] = 'r.is_occupied AS is_occupied';
    }

    $baseSql = 'SELECT ' . implode(', ', $select) . ' FROM room_full_details d' . $join;

    if ($modeAll) {
        $sql = $baseSql . " ORDER BY d.room_id ASC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute();
    } else {
        // Prepare IN clause safely
        $placeholders = implode(',', array_fill(0, count($roomIds), '?'));
        $sql = $baseSql . " WHERE d.room_id IN ($placeholders) ORDER BY d.room_id ASC";
        $stmt = $pdo->prepare($sql);
        foreach ($roomIds as $i => $rid) {
            $stmt->bindValue($i + 1, $rid, PDO::PARAM_INT);
        }
        $stmt->execute();
    }

    $rows = $stmt->fetchAll();

    // Fallback: if selecting specific ids and some are missing from room_full_details,
    // fetch them from rooms table (if available) so the app can still display them minimally
    if (!$modeAll && !empty($roomIds)) {
        $gotIds = [];
        foreach ($rows as $r) {
            if (isset($r['room_id'])) $gotIds[(int)$r['room_id']] = true;
        }
        $missing = [];
        foreach ($roomIds as $rid) {
            if (!isset($gotIds[(int)$rid])) $missing[] = (int)$rid;
        }
        if (!empty($missing) && $hasRooms) {
            // Build rooms-only SELECT
            $rSelect = [];
            if (isset($roomCols['id'])) $rSelect[] = 'r.id';
            if (isset($roomCols['room_id'])) $rSelect[] = 'r.room_id';
            if (isset($roomCols['number'])) $rSelect[] = 'r.number AS number_std';
            if (isset($roomCols['room_number'])) $rSelect[] = 'r.room_number AS number_alt';
            if (isset($roomCols['type'])) $rSelect[] = 'r.type AS type_std';
            if (isset($roomCols['room_type'])) $rSelect[] = 'r.room_type AS type_alt';
            if (isset($roomCols['capacity'])) $rSelect[] = 'r.capacity AS capacity_std';
            if (isset($roomCols['bed_type'])) $rSelect[] = 'r.bed_type AS capacity_alt';
            if (isset($roomCols['status'])) $rSelect[] = 'r.status AS status_std';
            if (isset($roomCols['availability'])) $rSelect[] = 'r.availability AS status_alt';
            if (isset($roomCols['is_occupied'])) $rSelect[] = 'r.is_occupied AS is_occupied';
            if (!empty($rSelect)) {
                $ph = implode(',', array_fill(0, count($missing), '?'));
                // use detected PK for filtering
                $filterCol = $roomIdCol ?: (isset($roomCols['id']) ? 'id' : (isset($roomCols['room_id']) ? 'room_id' : 'id'));
                $sqlR = 'SELECT ' . implode(', ', $rSelect) . ' FROM rooms r WHERE r.' . $filterCol . ' IN (' . $ph . ')';
                $stR = $pdo->prepare($sqlR);
                foreach ($missing as $i => $rid) $stR->bindValue($i + 1, $rid, PDO::PARAM_INT);
                $stR->execute();
                $rRows = $stR->fetchAll();
                // Convert to room_full_details-like rows with defaults
                foreach ($rRows as $rr) {
                    // determine room_id value from rooms row
                    $ridVal = 0;
                    if ($roomIdCol && isset($rr[$roomIdCol])) {
                        $ridVal = (int)$rr[$roomIdCol];
                    } elseif (isset($rr['id'])) {
                        $ridVal = (int)$rr['id'];
                    } elseif (isset($rr['room_id'])) {
                        $ridVal = (int)$rr['room_id'];
                    }
                    $rows[] = array_merge([
                        'id' => 0,
                        'room_id' => $ridVal,
                        'price_per_night' => 0,
                        'floor' => 0,
                        'view' => '',
                        'features' => null,
                    ], $rr);
                }
            }
        }
    }

    $roomsOut = [];
    foreach ($rows as $row) {
        // Map number, type, capacity, status using multiple possible columns
        $number = isset($row['number_std']) ? $row['number_std'] : null;
        if ($number === null || $number === '') $number = isset($row['number_alt']) ? $row['number_alt'] : '';

        $type = isset($row['type_std']) ? $row['type_std'] : null;
        if ($type === null || $type === '') $type = isset($row['type_alt']) ? $row['type_alt'] : '';

        $capacity = isset($row['capacity_std']) ? $row['capacity_std'] : null;
        if ($capacity === null || $capacity === '') $capacity = isset($row['capacity_alt']) ? $row['capacity_alt'] : '';

        $status = isset($row['status_std']) ? $row['status_std'] : null;
        if ($status === null || $status === '') $status = isset($row['status_alt']) ? $row['status_alt'] : '';

        // Derive status from is_occupied if needed
        if (($status === null || $status === '') && array_key_exists('is_occupied', $row)) {
            $status = ((int)$row['is_occupied'] === 1) ? 'Occupied' : 'Available';
        }
        $status = (string)($status ?? '');

        // Availability and status color (match logic in get_rooms.php)
        $low = mb_strtolower(trim($status));
        $isAvailable = in_array($low, ['0','available','متاحة','فارغة','empty'], true) || $low === '';
        $statusColor = $isAvailable ? '#E8F5E8' : '#FFE8E8';

        // Card color based on view
        $view = (string)$row['view'];
        $viewLow = mb_strtolower($view);
        $cardColor = '#F5F5F5'; // default
        if (strpos($viewLow, 'sea') !== false || strpos($viewLow, 'بحر') !== false) {
            $cardColor = '#E3F2FD'; // light blue
        } elseif (strpos($viewLow, 'garden') !== false || strpos($viewLow, 'حديقة') !== false) {
            $cardColor = '#E8F5E9'; // light green
        } elseif (strpos($viewLow, 'city') !== false || strpos($viewLow, 'مدينة') !== false) {
            $cardColor = '#ECEFF1'; // light grey
        }

        // Decode features JSON safely
        $features = [];
        if (isset($row['features']) && $row['features'] !== '') {
            $decoded = json_decode($row['features'], true);
            if (json_last_error() === JSON_ERROR_NONE && is_array($decoded)) {
                $features = $decoded;
            }
        }

        // Max occupancy heuristic from capacity text
        $capText = strtolower(trim((string)$capacity));
        $maxOcc = 1;
        if (in_array($capText, ['double','2','زوجي','مزدوج'], true)) {
            $maxOcc = 2;
        } elseif (in_array($capText, ['triple','3','ثلاثي'], true)) {
            $maxOcc = 3;
        } elseif (in_array($capText, ['quad','4','رباعي'], true)) {
            $maxOcc = 4;
        }

        $roomsOut[] = [
            'id' => (int)$row['id'],
            'room_id' => (int)$row['room_id'],
            'room_number' => (string)$number,
            'room_type' => (string)$type,
            'floor' => (int)$row['floor'],
            'view' => $view,
            'price_per_night' => (float)$row['price_per_night'],
            'features' => $features,
            'capacity' => (string)$capacity,
            'max_occupancy' => $maxOcc,
            'status' => $status,
            'status_color' => $statusColor,
            'is_available' => $isAvailable,
            'card_color' => $cardColor
        ];
    }

    $response['success'] = true;
    $response['rooms'] = $roomsOut;
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();

} catch (Throwable $e) {
    http_response_code(500);
    $response['error'] = $e->getMessage();
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();
}
