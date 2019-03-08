<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$unixTime = $dataArray['timeStamp'];
$sessionId = $dataArray['sessionId'];
$response = $dataArray['response'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
if($response === 'Okay'){
    $result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,ok) VALUES('$unixTime','$sessionId',1)");
}else if($response === 'No'){
    $result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,no) VALUES('$unixTime','$sessionId',1)");
}else if($response === 'Snooze'){
    $result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,snooze) VALUES('$unixTime','$sessionId',1)");
}
if($result) {
	echo $body;
}
$conn = null;
?>