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
echo "Reasons are: " . $reasons . "<br>";
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
try{
    $result = $conn->exec("INSERT INTO responses_watch(timestamp,session_id,busy,pain,nausea,tired,other) VALUES('$unixTime','$sessionId','$busy','$pain','$nausea','$tired','$other')");
	echo "Result is: " .$result . "<br>";
} catch (Exception $e) {
    echo $sql . "<br>" . $e->getMessage();
}
$conn = null;
?>