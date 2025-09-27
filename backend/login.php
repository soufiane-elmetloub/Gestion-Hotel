<?php
require_once 'config.php';

// Add CORS headers
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json; charset=UTF-8");

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}


// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// Also check for form data
if (!$input) {
    $input = $_POST;
}

// Validate input
if (!isset($input['username']) || !isset($input['password'])) {
    sendResponse(false, 'Username and password are required');
}

$username = trim($input['username']);
$password = trim($input['password']);

if (empty($username) || empty($password)) {
    sendResponse(false, 'Please enter username and password');
}

try {
    // Get database connection
    $database = new Database();
    $conn = $database->getConnection();
    
    if (!$conn) {
        sendResponse(false, 'Database connection failed');
    }
    
    // Prepare and execute query (map 'phone' column as 'phone_number' for compatibility)
    $stmt = $conn->prepare("SELECT id, username, password, first_name, last_name, email, phone AS phone_number, assigned_section FROM employees WHERE username = :username");
    $stmt->bindParam(':username', $username);
    $stmt->execute();
    
    if ($stmt->rowCount() > 0) {
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        // Verify password
        if (password_verify($password, $user['password'])) {
            // Return user data (without password)
            $response = [
                'success' => true,
                'message' => 'Login successful',
                'user' => [
                    'id' => $user['id'],
                    'username' => $user['username'],
                    'first_name' => $user['first_name'],
                    'last_name' => $user['last_name'],
                    'email' => $user['email'],
                    'phone_number' => $user['phone_number'],
                    'assigned_section' => $user['assigned_section']
                ]
            ];
            
            echo json_encode($response);
        } else {
            sendResponse(false, 'Invalid password');
        }
    } else {
        sendResponse(false, 'Username not found');
    }
    
} catch(PDOException $exception) {
    sendResponse(false, 'Database error: ' . $exception->getMessage());
} catch(Exception $e) {
    sendResponse(false, 'Server error: ' . $e->getMessage());
}
?>
