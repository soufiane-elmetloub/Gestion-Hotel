<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

require_once 'config.php';

// Start session and check authentication
session_start();
if (!isset($_SESSION['super_admin_logged_in']) || $_SESSION['super_admin_logged_in'] !== true) {
    echo json_encode([
        'success' => false,
        'message' => 'غير مصرح بالوصول'
    ]);
    exit;
}

try {
    // Create database connection
    $database = new Database();
    $pdo = $database->getConnection();
    
    if (!$pdo) {
        throw new Exception('Database connection failed');
    }
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $stats = [];
    
    // Get total rooms (assuming you have a rooms table)
    try {
        $stmt = $pdo->query("SELECT COUNT(*) as total FROM rooms");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $stats['total_rooms'] = $result['total'];
    } catch (Exception $e) {
        $stats['total_rooms'] = 0;
    }
    
    // Get total bookings/reservations
    try {
        $stmt = $pdo->query("SELECT COUNT(*) as total FROM reservations");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $stats['total_bookings'] = $result['total'];
    } catch (Exception $e) {
        $stats['total_bookings'] = 0;
    }
    
    // Get total clients
    try {
        $stmt = $pdo->query("SELECT COUNT(*) as total FROM clients");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $stats['total_clients'] = $result['total'];
    } catch (Exception $e) {
        $stats['total_clients'] = 0;
    }
    
    // Get total revenue
    try {
        $stmt = $pdo->query("SELECT SUM(total_amount) as total FROM reservations WHERE status != 'cancelled'");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $stats['total_revenue'] = number_format($result['total'] ?? 0, 2);
    } catch (Exception $e) {
        $stats['total_revenue'] = '0.00';
    }
    
    // Get total employees
    try {
        $stmt = $pdo->query("SELECT COUNT(*) as total FROM employees");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $stats['total_employees'] = $result['total'];
    } catch (Exception $e) {
        $stats['total_employees'] = 0;
    }
    
    echo json_encode([
        'success' => true,
        'stats' => $stats
    ]);
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'خطأ في قاعدة البيانات',
        'stats' => [
            'total_rooms' => 0,
            'total_bookings' => 0,
            'total_clients' => 0,
            'total_revenue' => '0.00',
            'total_employees' => 0
        ]
    ]);
} catch (Exception $e) {
    error_log("General error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'حدث خطأ غير متوقع',
        'stats' => [
            'total_rooms' => 0,
            'total_bookings' => 0,
            'total_clients' => 0,
            'total_revenue' => '0.00',
            'total_employees' => 0
        ]
    ]);
}
?>
