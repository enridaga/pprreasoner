#!/bin/bash

function errcho {
    >&2 echo "$@"
}
function experiment {
        java -jar -Dlog4j.configuration="file:./experiment-log4j.properties" target/experiment-0.0.1-SNAPSHOT.jar "$@"
}

function monitor {
        sleep 0.1
        errcho "Monitoring $1"
	process=$1 #$(ps -a -o pid,command|sed 1d|grep java|grep experiment|cut -d " " -f 1)
	if [ ! -z "$process" ]
	then
	  while kill -0 $process 2> /dev/null; do
		ps -p $process -o pid,%cpu,%mem,vsz,rss|sed 1d
		errcho -n "."
		sleep 0.1
	  done
	  errcho " Done."
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
    sleep 1
    exec 1<&-
    exec 1<>$result.monitor.$count.$a
    errcho "$count $a - $args"
    echo "#$count #$a - $args"
    #./experiment.sh $args >> $result & 
    experiment $args >> $result &
    pro=$!
    monitor $pro
  done
  fi
done < "$suite"
