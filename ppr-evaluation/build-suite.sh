#!/bin/bash

for f in $(ls use-cases/*.ttl); do
 DIR="$( cd "$( dirname $f )" && pwd )"
 fname=$(basename $f)
 fname="${fname%.*}"
 output=$(head -n 1 $f | tr '#' ' ' | tr -d '[[:space:]]')
 dataflow=$DIR/$fname
 if [ -e "$dataflow.policies.ttl" ]; then
   testname=$(echo "$fname" | tr - /) 
   ns="http://purl.org/datanode/ex/0.2/"$testname"#"
   for r in Prolog SPIN ; do
	for c in 0 1; do
	   echo -c $c -r $r -k EVOLVED -d $dataflow -q $ns$output
	done
   done
 fi
done


