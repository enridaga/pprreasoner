#!/bin/bash

function monitor {
        sleep 0.1
	process=$(ps -a -o pid,command|sed 1d|grep java|grep experiment|cut -d " " -f 1)
	if [ ! -z "$process" ]
	then
	  while kill -0 $process 2> /dev/null; do
		ps -p $process -o pid,%cpu,%mem,vsz,rss|sed 1d
		sleep 0.01
	  done
	fi
}

suite=${1:-./suites/full.txt}
times=${2:-1}

result=results/$(basename "$suite")
rm $result 2> /dev/null
rm -f $result.monitor.* 2> /dev/null

count=0
while IFS= read -r args
do
  count=$((count+1))
  if [ ! -z "$args" ]; then
  for a in `seq $times`
  do
    exec 1<&-
    exec 1<>$result.monitor.$count.$a
    echo "#$count #$a - $args"
    ./experiment.sh $args >> $result & 
    monitor
  done
  fi
done < "$suite"
