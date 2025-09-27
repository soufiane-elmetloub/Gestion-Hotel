<?php
require_once __DIR__ . '/config.php';

$db = new Database();
$conn = $db->getConnection();
if (!$conn) {
    sendResponse(false, 'Database connection failed');
}

// Expecting application/x-www-form-urlencoded POST from Android Volley
$room_id = isset($_POST['room_id']) ? (int)$_POST['room_id'] : 0;
$client_id = isset($_POST['client_id']) ? (int)$_POST['client_id'] : 0;
$number_of_guests = isset($_POST['number_of_guests']) ? (int)$_POST['number_of_guests'] : 0;
$check_in = isset($_POST['check_in']) ? trim($_POST['check_in']) : '';
$check_out = isset($_POST['check_out']) ? trim($_POST['check_out']) : '';
$price_per_night = isset($_POST['price_per_night']) ? (float)$_POST['price_per_night'] : 0.0;
$total_amount = isset($_POST['total_amount']) ? (float)$_POST['total_amount'] : 0.0;
$employee_id = isset($_POST['employee_id']) ? (int)$_POST['employee_id'] : 0;
$status = isset($_POST['status']) && $_POST['status'] !== '' ? $_POST['status'] : 'reserved';
$payment_status = isset($_POST['payment_status']) && $_POST['payment_status'] !== '' ? $_POST['payment_status'] : 'pending';

// Basic validation
// Allow zero guests (only disallow negative)
if ($room_id <= 0 || $client_id <= 0 || $number_of_guests < 0 || empty($check_in) || empty($check_out)
    || $price_per_night <= 0 || $total_amount <= 0 || $employee_id <= 0) {
    sendResponse(false, 'Missing or invalid required fields');
}

try {
    $sql = "INSERT INTO reservations (room_id, client_id, number_of_guests, check_in, check_out, price_per_night, total_amount, employee_id, status, payment_status)
            VALUES (:room_id, :client_id, :number_of_guests, :check_in, :check_out, :price_per_night, :total_amount, :employee_id, :status, :payment_status)";
    $stmt = $conn->prepare($sql);
    $stmt->bindParam(':room_id', $room_id, PDO::PARAM_INT);
    $stmt->bindParam(':client_id', $client_id, PDO::PARAM_INT);
    $stmt->bindParam(':number_of_guests', $number_of_guests, PDO::PARAM_INT);
    $stmt->bindParam(':check_in', $check_in);
    $stmt->bindParam(':check_out', $check_out);
    $stmt->bindParam(':price_per_night', $price_per_night);
    $stmt->bindParam(':total_amount', $total_amount);
    $stmt->bindParam(':employee_id', $employee_id, PDO::PARAM_INT);
    $stmt->bindParam(':status', $status);
    $stmt->bindParam(':payment_status', $payment_status);
    $stmt->execute();

    $newId = $conn->lastInsertId();
    sendResponse(true, 'Reservation created successfully', ['reservation_id' => $newId]);
} catch (PDOException $e) {
    error_log('Insert reservation error: ' . $e->getMessage());
    sendResponse(false, 'Failed to create reservation');
}
