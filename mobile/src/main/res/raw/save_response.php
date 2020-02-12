<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$unixTime = $dataArray['timeStamp'];
$sessionId = $dataArray['sessionId'];
$reaction = $dataArray['response'];
$response = array();
try{
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	if($reaction === 'Okay'){
    	$result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,ok) VALUES('$unixTime','$sessionId',1)");
	}else if($reaction === 'No'){
    	$result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,no) VALUES('$unixTime','$sessionId',1)");
	}else if($reaction === 'Snooze'){
    	$result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,snooze) VALUES('$unixTime','$sessionId',1)");
	}
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


