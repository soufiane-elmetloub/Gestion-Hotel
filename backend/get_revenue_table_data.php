<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

try {
    $db = new Database();
    $pdo = $db->getConnection();

    if (!$pdo) {
        echo json_encode(['success' => false, 'error' => 'Database connection failed']);
        exit;
    }

    // Optional filters
    $limit = isset($_GET['limit']) ? max(1, min(200, intval($_GET['limit']))) : 30; // only used when no date filter
    $dateFilter = isset($_GET['date']) ? trim($_GET['date']) : null; // expected format YYYY-MM-DD

    if ($dateFilter && preg_match('/^\d{4}-\d{2}-\d{2}$/', $dateFilter)) {
        // Exact-day filter
        $stmt = $pdo->prepare("SELECT log_date, daily_total, weekly_total, monthly_total
                               FROM revenue_logs
                               WHERE log_date = :d
                               ORDER BY created_at ASC");
        $stmt->bindValue(':d', $dateFilter, PDO::PARAM_STR);
        $stmt->execute();
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    } else {
        // Fetch last N rows ordered by log_date desc, then reverse for ascending display
        $stmt = $pdo->prepare("SELECT log_date, daily_total, weekly_total, monthly_total FROM revenue_logs ORDER BY log_date DESC, created_at DESC LIMIT :lim");
        $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
        $stmt->execute();
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        // Reverse to ascending by date for display (Date header typically left-to-right by time)
        $rows = array_reverse($rows);
    }

    $out = [];
    foreach ($rows as $r) {
        $date = $r['log_date'];
        // Format date to DD/MM
        $ts = strtotime($date);
        $date_dm = $ts ? date('d/m', $ts) : $date;
        $out[] = [
            'date' => $date,
            'date_dm' => '│' . $date_dm,
            'daily_total' => (float)$r['daily_total'],
            'weekly_total' => (float)$r['weekly_total'],
            'monthly_total' => (float)$r['monthly_total'],
            'formatted' => [
                'daily_total' => number_format((float)$r['daily_total'], 2),
                'weekly_total' => number_format((float)$r['weekly_total'], 2),
                'monthly_total' => number_format((float)$r['monthly_total'], 2)
            ]
        ];
    }

    echo json_encode([
        'success' => true,
        'count' => count($out),
        'rows' => $out
    ]);

} catch (Exception $e) {
    echo json_encode(['success' => false, 'error' => 'Database error: ' . $e->getMessage()]);
}
