<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$unixTime = $dataArray['timeStamp'];
$sessionId = $dataArray['sessionId'];
$reasons = $dataArray['reasons'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');

$result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,busy,pain,nausea,tired,other) VALUES('$unixTime','$sessionId',$reasons{0},$reasons{1},$reasons{2},$reasons{3},$reasons{4})");
if($result) {
	echo $body;
}
$conn = null;
?>