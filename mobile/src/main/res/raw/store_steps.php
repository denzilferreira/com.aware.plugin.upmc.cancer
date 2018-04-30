<?php
$body = file_get_contents('php://input');
$dataArray = array();
$dataArray = json_decode($body, true);
$timeStamp = $dataArray['timeStamp'];
$timeData = $dataArray['timeCounter'];
$stepsData = $dataArray['diff'];
$hrData = $dataArray['hrData'];
$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
$result = $conn->exec("INSERT INTO StepCount(unixTime,minute,steps,heartRate) VALUES('$timeStamp','$timeData','$stepsData','$hrData')");
$conn = null;
echo $body;
?>