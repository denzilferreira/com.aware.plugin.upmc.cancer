<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body, true);
$timeStamp = $dataArray['timeStamp'];
$sessionId = $dataArray['sessionId'];
$type = $dataArray['type'];
$sensorData = $dataArray['sensorData'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
$result = $conn->exec("INSERT INTO SensorData(timestamp, session_id, type,data) VALUES('$timeStamp','$sessionId','$type','$sensorData')");
$conn = null;
echo $body;
?>