<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$unixTime = $dataArray['timeStamp'];
$status = $dataArray['status'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
$result = $conn->exec("INSERT INTO Connection(timestamp, status) VALUES('$unixTime','$status')");
if($result) {
	echo $body;
}
$conn = null;
?>