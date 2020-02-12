<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$unixTime = $dataArray['timeStamp'];
$status = $dataArray['status'];
$response = array();
try{
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = $conn->exec("INSERT INTO Connection(timestamp, status) VALUES('$unixTime','$status')");
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