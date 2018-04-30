<?php
try{
	$body = file_get_contents('php://input');
	$dataArray = array();
	$dataArray = json_decode($body,true);
	$unixTime = $dataArray['timeStamp'];
	$notif = $dataArray['notif'];
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = $conn->exec("INSERT INTO Notification(unixTime,message) VALUES('$unixTime','$notif')");
	if($result) {
		echo $body;
	}
}
catch(PODException $e) {
	echo $sql . "<br>" . $e->getMessage();
}
$conn = null;
?>