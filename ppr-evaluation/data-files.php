<?php

define(TARGET,"/Users/ed4565/Dropbox/PhD/Propagation-SWJ/src/inc/experiments/data/");

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
function processMonitor($file){
    $lines = explode("\n",file_get_contents($file));
	array_shift($lines);
//		var_dump($lines);die;
	$cpu = array();
	$mem = array();
	$rss = array();
	foreach($lines as $row){
		$cells = explode(',',preg_replace('/\s+/',',',$row));
		array_push($cpu,$cells[1] + 0);
		array_push($mem,$cells[2] + 0);
		array_push($rss,$cells[4] + 0);
	}
	return array(
		'max_cpu' => max($cpu),
		'min_cpu' => min($cpu),
		'avg_cpu' => avg($cpu),
		'max_mem' => max($mem),
		'min_mem' => min($mem),
		'avg_mem' => avg($mem),
		'max_rss' => max($rss),
		'min_rss' => min($rss),
		'avg_rss' => avg($rss)
	);
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

function generate($data,$cond,$cols=array(),$fname = NULL){
	$res = select($data, $cols, $cond);
	$t = renderTable($res, "\t");
	sort($cond);
	if($fname == NULL){
		$fname = str_replace('=','is',implode('-', $cond));		
	}
	if(!$fname){
		throw new RuntimeException('Fname is null');
	}
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
		$monitor = $file . '.monitor.' . $ecounter . '.' . $eecounter;
		// Monitoring data processing here
		$monitorData = processMonitor($monitor);
		$arr2 = array_merge($arr2,$monitorData);
		//var_dump($arr2); die;
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
		$columns = array('total','setup','load','query','size','max_cpu','avg_cpu','max_rss');
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
	$headers = array('R','E','C','T','T_c','S','S_c','L','L_c','Q','Q_c','K','K_c','P','P_c','Pa','Pa_c','M','M_c','F');
	array_push($table, $headers);
	foreach($byReasoner as $reason => $reas){
		foreach($reas as $input => $exp){
			foreach($exp as $compressed => $ex){
				$row = array();
				array_push($row,$reason);
				array_push($row,$input);
				array_push($row,($compressed=="full")?'N':'Y');
				
				foreach(array('total','setup','load','query','size','max_cpu','avg_cpu','max_rss') as $k){
					array_push($row,$ex[$k]['avg']);
					array_push($row,round($ex[$k]['coeff'], 2));
				}
				
				array_push($row, implode('|',$ex['files']));
				array_push($table, $row);
			}
		}
	}
	return $table;
}
function parseProlog($file){
	$x = array();
	$o = explode("\n", file_get_contents($file));
//	var_dump($o);die;
	foreach($o as $it){
		$it = trim($it);
		if($it){
//			print "$it\n";
			$p = substr($it, 0, strpos($it,'('));
			$args = explode(',',substr($it, strpos($it,'(')+1, strpos($it,')') - strpos($it,'(') - 1 ));
			array_push($x,array($p,$args));
		}
	}
//	die;
	return $x;
}
function useCases($data) {
	$result = select($data, array('E','F'), array('C=Y'));
	array_shift($result);
	$useCases = array(
		array('E','N_P','N_R','R','R_C','D','D_C','P','P_C')
	);
	$rc = 0;
	foreach($result as $row){
		$rc++;
		$useCase = $row[1];
		$files = $row[2];
		$fl = explode('|', $files);
		$arr = array();
		$arr['has_policy'] = 0;
		$arr['has_relation'] = 0;
		$arr['relations'] = array();
		$arr['nodes'] = array();
		$arr['policies'] = array();
		foreach($fl as $f){
			$pl = $f . '.pl';
			$ppl = $f . '.policies.pl';
			$dataflow = parseProlog($pl);
			$policies = parseProlog($ppl);
			$all = array_merge($dataflow, $policies);
			foreach($all as $a){
				$arr[$a[0]]++;
				if($a[0] == 'has_relation'){
					array_push($arr['nodes'],$a[1][0]);
					array_push($arr['nodes'],$a[1][1]);
					array_push($arr['relations'],$a[1][2]);
				}
				if($a[0] == 'has_policy'){
					array_push($arr['nodes'],$a[1][0]);
					array_push($arr['policies'],$a[1][1]);
				}
			}
			$arr['relations'] = array_unique($arr['relations'], SORT_STRING);
			$arr['nodes'] = array_unique($arr['nodes'], SORT_STRING);
			$arr['policies'] = array_unique($arr['policies'], SORT_STRING);
		}
		array_push($useCases,
				array(	
					$useCase,
					$arr['has_policy'],
					$arr['has_relation'],
					implode(',', $arr['relations']),
					count($arr['relations']),
					implode(',', $arr['nodes']),
					count($arr['nodes']),
					implode(',', $arr['policies']),
					count($arr['policies'])));
	}
	return $useCases;
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

$file = isset($argv[1]) ? $argv[1] : "use-cases.txt";
$file = "results/".$file;
$data = loadData($file);
$uc = useCases($data);

// print renderTable($uc,"\t");
// die;
// die;
generate($uc, array(), array('E','N_P','N_R','R_C','D_C','P_C') ,'UC');
generate($data, array('R=Prolog','C=Y'),array('E','T','S','L','Q','K','Pa','M') );
generate($data, array('R=Prolog','C=N'),array('E','T','S','L','Q','K','Pa','M') );
generate($data, array('R=SPIN','C=Y'),array('E','T','S','L','Q','K','Pa','M') );
generate($data, array('R=SPIN','C=N'),array('E','T','S','L','Q','K','Pa','M') );
