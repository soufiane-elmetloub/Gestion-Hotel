<?php
require_once __DIR__ . '/config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { exit(0); }

try{
    $employeeId = isset($_GET['employee_id']) ? (int)$_GET['employee_id'] : 0;
    if ($employeeId <= 0) {
        http_response_code(400);
        sendResponse(false, 'employee_id is required');
    }
    $onlyUnread = isset($_GET['only_unread']) && ($_GET['only_unread'] === '1' || strtolower($_GET['only_unread'])==='true');
    $limit = isset($_GET['limit']) ? max(1, min(500, (int)$_GET['limit'])) : 200;

    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) { sendResponse(false, 'Database connection failed'); }

    $sql = "SELECT n.id, n.super_admin_id, n.employee_id, n.title, n.message, n.is_read, n.created_at,
                   sa.username AS super_admin_username,
                   e.first_name, e.last_name
            FROM notifications n
            LEFT JOIN super_admin sa ON sa.id = n.super_admin_id
            LEFT JOIN employees e ON e.id = n.employee_id
            WHERE n.employee_id = :eid" . ($onlyUnread ? " AND n.is_read = 0" : "") . "
            ORDER BY n.is_read ASC, n.created_at DESC
            LIMIT :lim";
    $stmt = $pdo->prepare($sql);
    $stmt->bindValue(':eid', $employeeId, PDO::PARAM_INT);
    $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll();

    foreach($rows as &$r){
        $name = trim(($r['first_name'] ?? '') . ' ' . ($r['last_name'] ?? ''));
        $r['employee_name'] = $name !== '' ? $name : ('Emp #' . $r['employee_id']);
        unset($r['first_name'], $r['last_name']);
    }

    echo json_encode(['success'=>true,'data'=>$rows]);
    exit;
}catch(Throwable $e){
    http_response_code(500);
    echo json_encode(['success'=>false,'message'=>'Server error: '.$e->getMessage()]);
    exit;
}
