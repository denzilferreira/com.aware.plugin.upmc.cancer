<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body, true);
$timeStamp = $dataArray['timeStamp'];
$type = $dataArray['type'];
$sensorData = $dataArray['sensorData'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
$result = $conn->exec("INSERT INTO SensorData(unixTime,type,data) VALUES('$timeStamp','$type','$sensorData')");
$conn = null;
echo $body;
?>