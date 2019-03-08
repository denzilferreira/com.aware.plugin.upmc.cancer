<?php
try{
	$body = file_get_contents('php://input');
	$dataArray = array();
	$dataArray = json_decode($body,true);
	$unixTime = $dataArray['timeStamp'];
	$sessionId = $dataArray['sessionId'];
	$notif = $dataArray['notif'];
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = $conn->exec("INSERT INTO interventions_watch(timestamp, session_id, notif_type) VALUES('$unixTime','$sessionId','$notif')");
	if($result) {
		echo $body;
	}
}
catch(PODException $e) {
	echo $sql . "<br>" . $e->getMessage();
}
$conn = null;
?>