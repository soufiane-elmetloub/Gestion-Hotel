<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

try {
    // Use instance method as defined in config.php
    $db = new Database();
    $pdo = $db->getConnection();
    
    // Get today's date
    $today = date('Y-m-d');
    
    // Try to get today's daily_total first
    $stmt = $pdo->prepare("SELECT daily_total, log_date FROM revenue_logs WHERE log_date = ? ORDER BY created_at DESC LIMIT 1");
    $stmt->execute([$today]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$result) {
        // Fallback: get the most recent record if today has no entry
        $stmt = $pdo->query("SELECT daily_total, log_date FROM revenue_logs ORDER BY log_date DESC, created_at DESC LIMIT 1");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
    }

    // Keep raw DB string to display exactly as stored (avoid double formatting)
    $daily_total_str = $result && isset($result['daily_total']) ? (string)$result['daily_total'] : "0.00";
    // Also provide numeric for consumers that need it
    $daily_total = is_numeric($daily_total_str) ? (float)$daily_total_str : 0.00;
    $used_date = $result ? $result['log_date'] : $today;
    
    // Return the daily total
    echo json_encode([
        'success' => true,
        // Numeric version (for calculations)
        'daily_total' => $daily_total,
        // Raw string from DB to display exactly as stored
        'daily_total_str' => $daily_total_str,
        // Optional formatted version
        'formatted_total' => number_format($daily_total, 2),
        'date' => $used_date,
        'is_today' => ($used_date === $today)
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage(),
        'daily_total' => 0.00
    ]);
}
?>
