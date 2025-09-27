<?php
// Fix employees table structure by adding missing columns
$servername = "localhost";
$username = "Unknown";
$password = "5L7Fqp9GG-@r7trj";
$dbname = "smart-hotel";

try {
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }
    
    echo "<h2>Fixing Employees Table Structure</h2>";
    
    // Check current table structure
    $result = $conn->query("SHOW COLUMNS FROM employees");
    $existing_columns = array();
    
    echo "<h3>Current Table Structure:</h3>";
    while($row = $result->fetch_assoc()) {
        $existing_columns[] = $row['Field'];
        echo "- " . $row['Field'] . " (" . $row['Type'] . ")<br>";
    }
    
    // Add missing columns
    $columns_to_add = array(
        'first_name' => "VARCHAR(50)",
        'last_name' => "VARCHAR(50)", 
        'phone' => "VARCHAR(20)",
        'role' => "VARCHAR(50) DEFAULT 'employee'",
        'status' => "VARCHAR(20) DEFAULT 'active'"
    );
    
    echo "<h3>Adding Missing Columns:</h3>";
    
    foreach ($columns_to_add as $column => $definition) {
        if (!in_array($column, $existing_columns)) {
            $sql = "ALTER TABLE employees ADD COLUMN $column $definition";
            if ($conn->query($sql) === TRUE) {
                echo "<p style='color: green;'>✓ Added column: $column</p>";
            } else {
                echo "<p style='color: red;'>✗ Error adding $column: " . $conn->error . "</p>";
            }
        } else {
            echo "<p style='color: blue;'>- Column $column already exists</p>";
        }
    }
    
    // Update existing records with sample data if they don't have names
    echo "<h3>Updating Existing Records:</h3>";
    
    $result = $conn->query("SELECT id, username, first_name FROM employees WHERE first_name IS NULL OR first_name = ''");
    
    if ($result->num_rows > 0) {
        $updates = array(
            'soufiane' => array('first_name' => 'Soufiane', 'last_name' => 'Admin', 'role' => 'manager', 'phone' => '+966 50 123 4567'),
            'ahmed_r' => array('first_name' => 'Ahmed', 'last_name' => 'Receptionist', 'role' => 'receptionist', 'phone' => '+966 55 987 6543'),
        );
        
        while($row = $result->fetch_assoc()) {
            $username = $row['username'];
            if (isset($updates[$username])) {
                $data = $updates[$username];
                $update_sql = "UPDATE employees SET 
                    first_name = '{$data['first_name']}', 
                    last_name = '{$data['last_name']}', 
                    role = '{$data['role']}', 
                    phone = '{$data['phone']}',
                    status = 'active'
                    WHERE username = '$username'";
                
                if ($conn->query($update_sql) === TRUE) {
                    echo "<p style='color: green;'>✓ Updated user: $username</p>";
                } else {
                    echo "<p style='color: red;'>✗ Error updating $username: " . $conn->error . "</p>";
                }
            } else {
                // Generic update for unknown users
                $generic_sql = "UPDATE employees SET 
                    first_name = COALESCE(first_name, 'Employee'), 
                    last_name = COALESCE(last_name, 'User'), 
                    role = COALESCE(role, 'employee'), 
                    phone = COALESCE(phone, 'N/A'),
                    status = COALESCE(status, 'active')
                    WHERE username = '$username'";
                
                if ($conn->query($generic_sql) === TRUE) {
                    echo "<p style='color: green;'>✓ Updated user with generic data: $username</p>";
                }
            }
        }
    } else {
        echo "<p>All records already have names assigned.</p>";
    }
    
    // Show final table structure
    echo "<h3>Final Table Structure:</h3>";
    $result = $conn->query("SHOW COLUMNS FROM employees");
    while($row = $result->fetch_assoc()) {
        echo "- " . $row['Field'] . " (" . $row['Type'] . ")<br>";
    }
    
    // Show sample data
    echo "<h3>Sample Data:</h3>";
    $result = $conn->query("SELECT id, username, first_name, last_name, role, phone, status FROM employees LIMIT 5");
    if ($result->num_rows > 0) {
        echo "<table border='1' style='border-collapse: collapse;'>";
        echo "<tr><th>ID</th><th>Username</th><th>First Name</th><th>Last Name</th><th>Role</th><th>Phone</th><th>Status</th></tr>";
        while($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>" . $row['id'] . "</td>";
            echo "<td>" . $row['username'] . "</td>";
            echo "<td>" . $row['first_name'] . "</td>";
            echo "<td>" . $row['last_name'] . "</td>";
            echo "<td>" . $row['role'] . "</td>";
            echo "<td>" . $row['phone'] . "</td>";
            echo "<td>" . $row['status'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
    }
    
    $conn->close();
    
    echo "<hr>";
    echo "<p style='color: green; font-weight: bold;'>✓ Table structure fixed! You can now test the API.</p>";
    echo "<p><a href='backend/get_employees.php' target='_blank'>Test API: backend/get_employees.php</a></p>";
    echo "<p><a href='Web/dashboard_v2.html' target='_blank'>Go to Dashboard</a></p>";
    
} catch (Exception $e) {
    echo "<p style='color: red;'>Error: " . $e->getMessage() . "</p>";
}
?>
