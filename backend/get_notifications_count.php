<?php
require_once __DIR__ . '/config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { exit(0); }

try{
    $db = new Database();
    $pdo = $db->getConnection();
    if (!$pdo) { echo json_encode(['success'=>false,'message'=>'DB error']); exit; }

    $stmt = $pdo->query("SELECT COUNT(*) AS cnt FROM notifications WHERE is_read = 0");
    $row = $stmt->fetch();
    $count = (int)($row['cnt'] ?? 0);

    echo json_encode(['success'=>true,'count'=>$count]);
    exit;
}catch(Throwable $e){
    http_response_code(500);
    echo json_encode(['success'=>false,'message'=>'Server error: '.$e->getMessage()]);
    exit;
}
