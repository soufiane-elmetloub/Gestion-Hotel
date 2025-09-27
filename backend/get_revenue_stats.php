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
        echo json_encode([
            'success' => false,
            'error' => 'Database connection failed'
        ]);
        exit;
    }

    $today = date('Y-m-d');

    // Latest monthly_total
    $stmt = $pdo->query("SELECT monthly_total FROM revenue_logs ORDER BY log_date DESC, created_at DESC LIMIT 1");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    $latest_monthly_total = $row && isset($row['monthly_total']) ? (float)$row['monthly_total'] : 0.0;

    // Daily average for current month
    $stmt = $pdo->prepare("SELECT AVG(daily_total) AS avg_daily FROM revenue_logs WHERE YEAR(log_date) = YEAR(CURRENT_DATE()) AND MONTH(log_date) = MONTH(CURRENT_DATE())");
    $stmt->execute();
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    $daily_average = $row && isset($row['avg_daily']) ? (float)$row['avg_daily'] : 0.0;

    // Highest daily total overall
    $stmt = $pdo->query("SELECT MAX(daily_total) AS max_daily FROM revenue_logs");
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    $highest_daily = $row && isset($row['max_daily']) ? (float)$row['max_daily'] : 0.0;

    // Today's daily total (fallback to most recent if none today)
    $stmt = $pdo->prepare("SELECT daily_total, log_date FROM revenue_logs WHERE log_date = ? ORDER BY created_at DESC LIMIT 1");
    $stmt->execute([$today]);
    $todayRow = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$todayRow) {
        $stmt = $pdo->query("SELECT daily_total, log_date FROM revenue_logs ORDER BY log_date DESC, created_at DESC LIMIT 1");
        $todayRow = $stmt->fetch(PDO::FETCH_ASSOC);
    }
    $today_total = $todayRow && isset($todayRow['daily_total']) ? (float)$todayRow['daily_total'] : 0.0;
    $today_date = $todayRow && isset($todayRow['log_date']) ? $todayRow['log_date'] : $today;

    echo json_encode([
        'success' => true,
        'monthly_total' => round($latest_monthly_total, 2),
        'daily_average' => round($daily_average, 2),
        'highest_daily' => round($highest_daily, 2),
        'today_total' => round($today_total, 2),
        'today_date' => $today_date,
        'formatted' => [
            'monthly_total' => number_format($latest_monthly_total, 2),
            'daily_average' => number_format($daily_average, 2),
            'highest_daily' => number_format($highest_daily, 2),
            'today_total' => number_format($today_total, 2)
        ]
    ]);

} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ]);
}
