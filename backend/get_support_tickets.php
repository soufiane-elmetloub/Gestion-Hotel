<?php
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-cache, must-revalidate');

require_once __DIR__ . '/config.php';

$response = [ 'success' => false, 'data' => [], 'error' => null ];

try {
    // Use PDO connection from Database class
    $db = (new Database())->getConnection();
    if (!$db) {
        throw new Exception('Database connection not initialized');
    }

    // Optional filters
    $status = isset($_GET['status']) && $_GET['status'] !== '' ? $_GET['status'] : null;
    $q = isset($_GET['q']) && $_GET['q'] !== '' ? trim($_GET['q']) : null;
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 200;
    if ($limit < 1 || $limit > 1000) $limit = 200;

    $where = [];
    $params = [];

    if ($status) {
        $where[] = 'status = :status';
        $params[':status'] = $status;
    }
    if ($q) {
        // Search only in universally expected columns
        $where[] = '(subject LIKE :q OR message LIKE :q)';
        $params[':q'] = '%' . $q . '%';
    }

    $whereSql = count($where) ? ('WHERE ' . implode(' AND ', $where)) : '';

    $sql = "SELECT 
                st.id,
                COALESCE(st.employee_id, 0) AS employee_id,
                '' AS email,
                '' AS section,
                COALESCE(st.subject, '') AS subject,
                COALESCE(st.message, '') AS message,
                COALESCE(st.status, 'open') AS status,
                COALESCE(st.created_at, NOW()) AS created_at,
                TRIM(COALESCE(NULLIF(CONCAT(e.first_name,' ', e.last_name),' '),
                    CASE WHEN st.employee_id IS NOT NULL THEN CONCAT('Emp #', st.employee_id) ELSE NULL END,
                    'Unknown')) AS display_name
            FROM support_tickets st
            LEFT JOIN employees e ON e.id = st.employee_id
            $whereSql
            ORDER BY st.created_at DESC, st.id DESC
            LIMIT :limit";

    $stmt = $db->prepare($sql);
    foreach ($params as $k => $v) {
        if ($k === ':limit') continue; // just in case
        if ($k === ':status' || $k === ':q') {
            $stmt->bindValue($k, $v, PDO::PARAM_STR);
        }
    }
    $stmt->bindValue(':limit', (int)$limit, PDO::PARAM_INT);

    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $response['success'] = true;
    $response['data'] = $rows ?: [];
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
} catch (Throwable $e) {
    http_response_code(500);
    $response['error'] = $e->getMessage();
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}
