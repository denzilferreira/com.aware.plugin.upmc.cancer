<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$prompt = $dataArray['prompt'];
$sessionId = $dataArray['sessionId'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
$result = $conn->exec("INSERT INTO PromptFromWatch(message,session_id) VALUES('$prompt','$sessionId')");
if($result) {
	echo $body;
}
$conn = null;
?>