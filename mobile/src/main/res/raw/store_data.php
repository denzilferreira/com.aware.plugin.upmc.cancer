<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body, true);
$timeStamp = $dataArray['timeStamp'];
$sessionId = $dataArray['sessionId'];
$type = $dataArray['type'];
$sensorData = $dataArray['sensorData'];
$response = array();
try{
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = $conn->exec("INSERT INTO SensorData(timestamp, session_id, type,data) VALUES('$timeStamp','$sessionId','$type','$sensorData')");
	if($result) {
		$response["error"] = false;
		$response["message"] = "Success";
	}else{
		$response["error"] = true;
		$response["message"] = "Insertion query failed";
	}
} catch(Exception $e){
	$response["error"] = true;
	$response["message"] = $e->getMessage();
}
echo json_encode($response);
$conn = null;
?>