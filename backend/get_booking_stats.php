<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

// Include database configuration
require_once 'config.php';

try {
    // Créer une connexion à la base de données via la classe Database
    $database = new Database();
    $pdo = $database->getConnection();
    if (!$pdo) {
        throw new PDOException('Échec de la connexion à la base de données');
    }
    
    // Nombre total de réservations
    $totalQuery = "SELECT COUNT(*) as total_bookings FROM reservations";
    $totalStmt = $pdo->prepare($totalQuery);
    $totalStmt->execute();
    $totalResult = $totalStmt->fetch(PDO::FETCH_ASSOC);
    $totalBookings = $totalResult['total_bookings'];
    
    // Nombre de clients ayant quitté (checked_out)
    $checkedOutQuery = "SELECT COUNT(*) as checked_out_guests FROM reservations WHERE status = 'checked_out'";
    $checkedOutStmt = $pdo->prepare($checkedOutQuery);
    $checkedOutStmt->execute();
    $checkedOutResult = $checkedOutStmt->fetch(PDO::FETCH_ASSOC);
    $checkedOutGuests = $checkedOutResult['checked_out_guests'];
    
    // Nombre de chambres occupées (reserved ou checked_in)
    $occupiedQuery = "SELECT COUNT(*) as occupied_rooms FROM reservations WHERE status IN ('reserved', 'checked_in')";
    $occupiedStmt = $pdo->prepare($occupiedQuery);
    $occupiedStmt->execute();
    $occupiedResult = $occupiedStmt->fetch(PDO::FETCH_ASSOC);
    $occupiedRooms = $occupiedResult['occupied_rooms'];
    
    // Statistiques additionnelles utiles
    $cancelledQuery = "SELECT COUNT(*) as cancelled_bookings FROM reservations WHERE status = 'cancelled'";
    $cancelledStmt = $pdo->prepare($cancelledQuery);
    $cancelledStmt->execute();
    $cancelledResult = $cancelledStmt->fetch(PDO::FETCH_ASSOC);
    $cancelledBookings = $cancelledResult['cancelled_bookings'];
    
    // Montant total encaissé
    $totalAmountQuery = "SELECT SUM(total_amount) as total_revenue FROM reservations WHERE status != 'cancelled'";
    $totalAmountStmt = $pdo->prepare($totalAmountQuery);
    $totalAmountStmt->execute();
    $totalAmountResult = $totalAmountStmt->fetch(PDO::FETCH_ASSOC);
    $totalRevenue = $totalAmountResult['total_revenue'] ?? 0;
    
    // Retourner les résultats en JSON
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
        'message' => 'Statistiques des réservations récupérées avec succès'
    );
    
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    
} catch(PDOException $e) {
    // En cas d'erreur de base de données
    $response = array(
        'success' => false,
        'error' => 'Erreur de base de données: ' . $e->getMessage(),
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
