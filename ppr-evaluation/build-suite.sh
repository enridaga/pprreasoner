#!/bin/bash

for f in $(ls use-cases/*.ttl); do
 DIR="$( cd "$( dirname $f )" && pwd )"
 fname=$(basename $f)
 fname="${fname%.*}"
 dataflow=$DIR/$fname
 testname=$(echo "$fname" | tr - /) 
 ns="http://purl.org/datanode/ex/0.2/"$testname"#"
 echo $ns
done


