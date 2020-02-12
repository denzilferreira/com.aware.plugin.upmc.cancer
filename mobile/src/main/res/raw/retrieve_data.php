<?php
try{
    $conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
    $q = $conn->query("SELECT result FROM PatientSurvey ORDER BY id DESC LIMIT 1");
	$lastInserted = $q->fetchColumn();
    echo $lastInserted;
}
catch(PODException $e)
	{
	echo $sql . "<br>" . $e->getMessage();
    }
$conn = null;
?>