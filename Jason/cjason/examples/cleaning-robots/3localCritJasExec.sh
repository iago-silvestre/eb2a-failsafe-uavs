#!/bin/bash
clear
# Check if the parameter is provided
if [ $# -ne 1 ]; then
  echo "at least one <integer> param is required"
  exit 1
fi
# Store the input parameter
count=$1
count=$((count - 1))
cp MarsEnvCritical.jaBAA MarsEnvCritical.java

for ((k=5; k<=5; k++)) #k<=5 for complete test
do
	runId="lc$k"
	cp runs/$runId ./marsPrjCritical.mas2j
	echo "Begin '$runId' "
	for ((i=0; i<=count; i++))
	do
		echo "RUN $runId - $i/$count"
		sleep 1
		start_time=$(date +%s)
		jason marsPrjCritical.mas2j > /dev/null
		end_time=$(date +%s)
		elapsed_time=$((end_time - start_time))
		echo "Elapsed time: $elapsed_time s"
		echo "Elapsed time (in seconds): $elapsed_time " >> reacTimes.log
		mv reacTimes.log EB2A-$runId-$i.reacTimes.log.txt
		sleep 1
	done
	rm mas-*
	echo "End '$runId' "
done
mv EB2A-lc* results
rm MarsEnvCritical.java
echo "Experiment FINISHED"