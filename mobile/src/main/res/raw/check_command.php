<?php
try {
	$conn = new PDO('mysql:dbname=UPMC;host=127.0.0.1','root','');
	$result = new \stdClass();
	$q = $conn->query("SELECT id FROM CommandFromPhone ORDER BY id DESC LIMIT 1");
	$result->id = $q->fetchColumn();
	$q = $conn->query("SELECT command FROM CommandFromPhone ORDER BY id DESC LIMIT 1");
	$result->command = $q->fetchColumn();
	echo json_encode($result);
}
catch(PODException $e) {
	echo $sql . "<br>" . $e->getMessage();
}
$conn = null;
?>