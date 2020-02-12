<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$prompt = $dataArray['prompt'];
$sessionId = $dataArray['sessionId'];
$response = array();
try{
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = $conn->exec("INSERT INTO PromptFromWatch(message,session_id) VALUES('$prompt','$sessionId')");
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