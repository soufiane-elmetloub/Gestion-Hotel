<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

// Include database configuration
require_once 'config.php';

try {
    // إنشاء الاتصال بقاعدة البيانات
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // إجمالي عدد الحجوزات
    $totalQuery = "SELECT COUNT(*) as total_bookings FROM reservations";
    $totalStmt = $pdo->prepare($totalQuery);
    $totalStmt->execute();
    $totalResult = $totalStmt->fetch(PDO::FETCH_ASSOC);
    $totalBookings = $totalResult['total_bookings'];
    
    // عدد الأشخاص الذين غادروا (checked_out)
    $checkedOutQuery = "SELECT COUNT(*) as checked_out_guests FROM reservations WHERE status = 'checked_out'";
    $checkedOutStmt = $pdo->prepare($checkedOutQuery);
    $checkedOutStmt->execute();
    $checkedOutResult = $checkedOutStmt->fetch(PDO::FETCH_ASSOC);
    $checkedOutGuests = $checkedOutResult['checked_out_guests'];
    
    // عدد الغرف المشغولة (reserved أو checked_in)
    $occupiedQuery = "SELECT COUNT(*) as occupied_rooms FROM reservations WHERE status IN ('reserved', 'checked_in')";
    $occupiedStmt = $pdo->prepare($occupiedQuery);
    $occupiedStmt->execute();
    $occupiedResult = $occupiedStmt->fetch(PDO::FETCH_ASSOC);
    $occupiedRooms = $occupiedResult['occupied_rooms'];
    
    // إحصائيات إضافية مفيدة
    $cancelledQuery = "SELECT COUNT(*) as cancelled_bookings FROM reservations WHERE status = 'cancelled'";
    $cancelledStmt = $pdo->prepare($cancelledQuery);
    $cancelledStmt->execute();
    $cancelledResult = $cancelledStmt->fetch(PDO::FETCH_ASSOC);
    $cancelledBookings = $cancelledResult['cancelled_bookings'];
    
    // إجمالي المبلغ المحصل
    $totalAmountQuery = "SELECT SUM(total_amount) as total_revenue FROM reservations WHERE status != 'cancelled'";
    $totalAmountStmt = $pdo->prepare($totalAmountQuery);
    $totalAmountStmt->execute();
    $totalAmountResult = $totalAmountStmt->fetch(PDO::FETCH_ASSOC);
    $totalRevenue = $totalAmountResult['total_revenue'] ?? 0;
    
    // إرجاع النتائج بصيغة JSON
    $response = array(
        'success' => true,
        'data' => array(
            'total_bookings' => (int)$totalBookings,
            'checked_out_guests' => (int)$checkedOutGuests,
            'occupied_rooms' => (int)$occupiedRooms,
            'cancelled_bookings' => (int)$cancelledBookings,
            'total_revenue' => (float)$totalRevenue,
            'active_bookings' => (int)($totalBookings - $checkedOutGuests - $cancelledBookings)
        ),
        'message' => 'تم جلب إحصائيات الحجوزات بنجاح'
    );
    
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    
} catch(PDOException $e) {
    // في حالة حدوث خطأ في قاعدة البيانات
    $response = array(
        'success' => false,
        'error' => 'خطأ في قاعدة البيانات: ' . $e->getMessage(),
        'data' => array(
            'total_bookings' => 0,
            'checked_out_guests' => 0,
            'occupied_rooms' => 0,
            'cancelled_bookings' => 0,
            'total_revenue' => 0,
            'active_bookings' => 0
        )
    );
    
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
}
?>
