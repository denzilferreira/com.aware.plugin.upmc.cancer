<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body,true);
$m = $dataArray['message'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
$result = $conn->exec("INSERT INTO PromptFromWatch(message) VALUES('$m')");
if($result) {
	echo $body;
}
$conn = null;
?>