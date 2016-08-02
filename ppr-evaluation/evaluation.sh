#!/bin/bash


function errcho {
    >&2 echo "$@"
}
function experiment {
        java -jar -Dlog4j.configuration="file:./experiment-log4j.properties" target/experiment-0.0.1-SNAPSHOT.jar "$@"
}

function monitor {
        sleep 0.1
	process=$(pgrep -P $1)      #$1 #$(ps -a -o pid,command|sed 1d|grep java|grep experiment|cut -d " " -f 1)
        MS=$2
	errcho "Monitoring $process (will interrupt in $MS seconds)"
	trap "kill $process $1" EXIT
	if [ ! -z "$process" ]
	then
	  SECONDS=0
	  while kill -0 $process 2> /dev/null; do
		[ "$SECONDS" -gt "$MS" ] && break || errcho -n "."
		ps -p $process -o pid,%cpu,%mem,vsz,rss|sed 1d
		sleep 0.2
	  done
	  [ "$SECONDS" -gt "$MS" ] && kill $process && errcho " Interrupted." || errcho " Done."
	fi
}

suite=${1:-./suites/use-cases.txt}
times=${2:-1}
interrupt=${3:-300} # almost 5 minutes
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
    sleep 5
    exec 1<&-
    exec 1<>$result.monitor.$count.$a
    errcho "$count $a - $args"
    echo "#$count #$a - $args"
    experiment $args >> $result &
    pro=$!
    monitor $pro $interrupt
  done
  fi
done < "$suite"
