<?php
require_once 'config.php';

header('Content-Type: application/json; charset=utf-8');
// Ensure errors don't break JSON response
ini_set('display_errors', 0);
ini_set('display_startup_errors', 0);
error_reporting(E_ALL);
ob_start();

$response = ['status' => 'error', 'message' => 'An unknown error occurred.'];

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $employeeId = isset($_POST['employee_id']) ? $_POST['employee_id'] : null;
    $employeeName = isset($_POST['employee_name']) ? $_POST['employee_name'] : null;
    $employeeSection = isset($_POST['employee_section']) ? $_POST['employee_section'] : null;
    $subject = isset($_POST['subject']) ? $_POST['subject'] : null;
    $message = isset($_POST['message']) ? $_POST['message'] : null;

    // Only strictly require fields that are guaranteed to exist in both schemas
    if (empty($employeeId) || empty($subject) || empty($message)) {
        $response['message'] = 'Missing required fields.';
    } else {
        $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

        if ($conn->connect_error) {
            $response['message'] = 'Database connection failed: ' . $conn->connect_error;
        } else {
            $conn->set_charset("utf8mb4");

            // Detect if denormalized columns exist (employee_name, employee_section) without using get_result()
            $hasExtraColumns = false;
            $checkSql = "SELECT COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'support_tickets' AND COLUMN_NAME IN ('employee_name','employee_section')";
            if ($checkStmt = $conn->prepare($checkSql)) {
                $dbName = DB_NAME; // bind_param requires variables by reference
                $checkStmt->bind_param("s", $dbName);
                if ($checkStmt->execute()) {
                    $checkStmt->bind_result($cnt);
                    if ($checkStmt->fetch()) {
                        $hasExtraColumns = ((int)$cnt === 2); // both columns exist
                    }
                }
                $checkStmt->close();
            }

            if ($hasExtraColumns) {
                // Insert including name and section when columns exist
                $stmt = $conn->prepare("INSERT INTO support_tickets (employee_id, employee_name, employee_section, subject, message) VALUES (?, ?, ?, ?, ?)");
                $stmt->bind_param("issss", $employeeId, $employeeName, $employeeSection, $subject, $message);
            } else {
                // Insert minimal schema only
                $stmt = $conn->prepare("INSERT INTO support_tickets (employee_id, subject, message) VALUES (?, ?, ?)");
                $stmt->bind_param("iss", $employeeId, $subject, $message);
            }

            if ($stmt) {
                if ($stmt->execute()) {
                    $response['status'] = 'success';
                    $response['message'] = 'Support request submitted successfully.';
                } else {
                    $response['message'] = 'Failed to submit request: ' . $stmt->error;
                }
                $stmt->close();
            } else {
                $response['message'] = 'Failed to prepare statement.';
            }

            $conn->close();
        }
    }
} else {
    $response['message'] = 'Invalid request method.';
}

// Clean any buffered output (e.g., notices) to ensure valid JSON
ob_clean();
echo json_encode($response);
exit;
?>
