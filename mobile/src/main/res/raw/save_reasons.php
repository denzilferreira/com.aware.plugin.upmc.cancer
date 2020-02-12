<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$unixTime = $dataArray['timeStamp'];
$sessionId = $dataArray['sessionId'];
$reasons = $dataArray['reasons'];
$busy = substr($reasons,-5,1);
$pain = substr($reasons,-4,1);
$nausea = substr($reasons,-3,1);
$tired = substr($reasons,-2,1);
$other = substr($reasons,-1,1);
$response = array();
try{
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,busy,pain,nausea,tired,other) VALUES('$unixTime','$sessionId','$busy','$pain','$nausea','$tired','$other')");
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