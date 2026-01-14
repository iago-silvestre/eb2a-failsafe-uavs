#!/bin/bash
echo "Running Standard REMOTE Jason "
if [ $# -ne 1 ]; then
  echo "at least one <integer> param is required"
  exit 1
fi
./gradlew clean
clear
echo "Running Standard REMOTE Jason "
count=$1
count=$((count - 1))

for ((k=5; k<=5; k++)) #k<=5 for complete test
do
	cp runs/$runId ./marsPrjStd.mas2j
	runId="rs$k"
	echo "Begin '$runId' "
	for ((i=0; i<=count; i++))
	do
		echo "RUN $runId - $i/$count"
		sleep 1
		start_time=$(date +%s)
		./gradlew run -q --console=plain # --no-daemon
		#jason marsPrjStd.mas2j > /dev/null
		end_time=$(date +%s)
		elapsed_time=$((end_time - start_time))
		echo "Elapsed time: $elapsed_time s"
		echo "Elapsed time (in seconds): $elapsed_time " >> reacTimes.log
		mv reacTimes.log $runId-$i.reacTimes.log.txt
		sleep 1
	done
	rm mas-*
	echo "End '$runId' "
done
mv rs* results
echo "Experiment FINISHED"