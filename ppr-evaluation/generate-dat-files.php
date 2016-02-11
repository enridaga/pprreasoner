<?php

define(TARGET,"/Users/ed4565/Dropbox/PhD/Propagation-SWJ/src/experiments/data/");

function avg($arr)
{
   if (!is_array($arr)) return false;

   return array_sum($arr)/count($arr);
}
function deviations($arr, $ref){
	$dev = array();
	foreach($arr as $i => $x){
		$dev[$i] = ($x-$ref);
	}
	return $dev;
}
function variance($arr, $avg){
	$dev = deviations($arr, $avg);
	$sum = 0;
	foreach($dev as $i => $x){
		$sum += ($x*$x);
	}
	$variance = $sum/(count($arr)-1);
	return $variance;
}

function column($arrOfarr,$subkey){
	$values = array();
	foreach($arrOfarr as $arr){
		array_push($values, $arr[$subkey]);
	}
	return $values;
}
function columnStdDeviation($arrOfarr, $columnKey){
	// coefficient of variation (CV), also known as relative standard deviation (RSD)
	$column = column($arrOfarr, $columnKey);
	$avg = avg($column);
	$stdDev = sqrt(variance($column, $avg));
	$coeff = $stdDev/$avg;
	return array('avg'=> $avg, 'coeff' => $coeff, 'deviation'=> $stdDev, 'values' => $column, '');
}
function select($from,$select=array(),$where=array()){
	$columns = array_shift($from);
	$res = array();
	$headers = (count($select) > 0) ? $select : $columns;
	$equals = array();
	foreach($where as $cond){
		$cc = explode('=',$cond);
		if(count($cc) == 2){
			// var_dump($columns);die;
			if(($index = array_search($cc[0], $columns)) === false) {
				throw new RuntimeException('Column "' . $cc[0] . '" of expression "' . $cond . '" does not exist.');
			}
			$cc[0] = $index;
			array_push($equals, $cc);
		}
	}
	$headersWithRowId = array('i');
	foreach($headers as $h) array_push($headersWithRowId, $h);
	
	array_push($res, $headersWithRowId);
	$rowId = 0;
	foreach($from as $row){
		$include = true;
		foreach($equals as $cc){
			if(($row[$cc[0]] != $cc[1])){
				$include = false;
				break;
			}
		}
		if($include){
			$rowId++;
			$toadd = array($rowId);
			foreach($headers as $col){
				$index = array_search($col,$columns);
				array_push($toadd, $row[$index]);
			}
			array_push($res,$toadd);
		}
	}
	return $res;
}
function renderTable($data, $del= ","){
	ob_start();
	// Print table
	foreach($data as $row){
		print implode($del, $row) . "\n";
	}
	return ob_get_clean();
}

function writeFile($fname, $data, $prefix= '', $postfix = '.dat'){
	file_put_contents($prefix . $fname . $postfix, $data);
}

function generate($data,$cond,$cols=array()){
	$res = select($data, $cols, $cond);
	$t = renderTable($res, "\t");
	sort($cond);
	$fname = str_replace('=','is',implode('-', $cond));
	writeFile(TARGET . $fname, $t);
}
function loadData($file){
	$lines = explode("\n",file_get_contents($file));
	$executions = array();
	$ecounter=0;
	$eecounter=0;
	$monitors = array();
	foreach($lines as $line){
		if(!trim($line)){
			continue;
		}
		$arr = explode("\t",$line);
		if(!isset($executions[$arr[0]])){
			$ecounter++;
			$eecounter=0;
			$executions[$arr[0]] = array();
			$executions[$arr[0]]['executions'] = array();
		}
		$eecounter++;
		$arr2 = array_combine(array('name','total','setup','load','query','size', 'result' ), $arr);
		$info = explode(' ', preg_replace('/\s\s+/', ' ',$arr2['name']));
		$executions[$arr[0]]['files'] = explode(',',preg_replace('/[\[|\]]/','',$info[0]));
		$executions[$arr[0]]['reasoner'] = $info[1];
		$executions[$arr[0]]['compressed'] = $info[3] == "COMPRESSED";
		$arr2['monitor'] = $file . '.monitor.' . $ecounter . '.' . $eecounter;
		array_push($executions[$arr[0]]['executions'], $arr2);
	}

	$experiments = array();

	// var_dump(columnVariance(array(array(0),array(1),array(2),array(3),array(4)), 0));
	// die;
	foreach($executions as $expId => $exData){
	//	$experiment = $exData;
		// Inherit
		foreach(array('files','reasoner','compressed') as $k){
			$experiment[$k] = $exData[$k];
		}
		// Calculate
		$columns = array('total','setup','load','query','size');
		foreach($columns as $col){
			$experiment[$col] = columnStdDeviation($exData['executions'], $col);
		}
		array_push($experiments, $experiment);
	}

	$byReasoner = array();
	foreach($experiments as $ex){

		$reasoner = $ex['reasoner'];
		$files = $ex['files'];
		$key = array();
		foreach($ex['files'] as $f){
			array_push($key, basename($f));
		}
		$key = implode(',', $key);
		if(!isset($byReasoner[$reasoner])){
			$byReasoner[$reasoner] = array();
		}
		if(!isset($byReasoner[$reasoner][$key])){
			$byReasoner[$reasoner][$key] = array();
		}
		if($ex['compressed']){
			$byReasoner[$reasoner][$key]['compressed'] = $ex;
		}else{
			$byReasoner[$reasoner][$key]['full'] = $ex;
		}
	}
	// var_dump($byReasoner);die;
	// Prolog
	$table = array();
	$headers = array('R','E','C','T','T_c','S','S_c','L','L_c','Q','Q_c','K','K_c');
	array_push($table, $headers);
	foreach($byReasoner as $reason => $reas){
		foreach($reas as $input => $exp){
			foreach($exp as $compressed => $ex){
				$row = array();
				array_push($row,$reason);
				array_push($row,$input);
				array_push($row,($compressed=="full")?'N':'Y');
				foreach(array('total','setup','load','query','size') as $k){
					array_push($row,$ex[$k]['avg']);
					array_push($row,round($ex[$k]['coeff'],2));		
				}
				array_push($table, $row);
			}
		}
	}
	return $table;
}
///////////////////////////////////////////////////////

$file = isset($argv[1]) ? $argv[1] : "use-cases.txt";
$file = "results/".$file;
$data = loadData($file);
generate($data, array('R=Prolog','C=Y'),array('E','T','S','L','Q','K') );
generate($data, array('R=Prolog','C=N'),array('E','T','S','L','Q','K') );
generate($data, array('R=SPIN','C=Y'),array('E','T','S','L','Q','K') );
generate($data, array('R=SPIN','C=N'),array('E','T','S','L','Q','K') );
