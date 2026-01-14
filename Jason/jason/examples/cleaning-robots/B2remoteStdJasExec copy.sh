#!/bin/bash
echo "Running Standard REMOTE Jason "
# Check if the parameter is provided
if [ $# -ne 1 ]; then
  echo "at least one <integer> param is required"
  exit 1
fi
./gradlew clean
clear
echo "Running Standard REMOTE Jason "
# Store the input parameter
count=$1
count=$((count - 1))

for ((k=1; k<=1; k++)) #k<=5 for complete test
do
	runId="ls$k"
	cp runs/$runId ./marsPrjStd.mas2j
	runId="rs$k"
	echo "  "
	echo "Begin '$runId' "
	for ((i=0; i<=count; i++))
	do
		echo "RUN $runId - $i/$count"
		start_time=$(date +%s)
		./gradlew run -q --console=plain # --no-daemon
		end_time=$(date +%s)
		elapsed_time=$((end_time - start_time))
		echo "Elapsed time: $elapsed_time s"
		echo "Elapsed time (in seconds): $elapsed_time " >> reacTimes.log
		mv reacTimes.log $runId-$i.reacTimes.log.txt
	done
		#./gradlew run -q --console=plain > /dev/null #> $runId-$i.tmp   
		#./gradlew run -q --console=plain #> /dev/null
		#nice -n 0 ./gradlew run -q --console=plain 
		#jason marsPrjCritical.mas2j > $runId-$i.tmp 
		#echo " "
		#more reacTimes.log
		#mv mas-0.log mas-0.log.$i
		#echo " "
		#pkill -f java
		#sleep 1
		#echo 1 | sudo tee /proc/sys/vm/drop_caches > /dev/null
		#sleep 1
		#echo 1 | sudo tee /proc/sys/vm/drop_caches > /dev/null
#	python3 parseStdJ.py $count > $runId.log.txt
#	more $runId.log.txt
#	tar czvf $runId.tar.gz mas-* 
	rm mas-*
	echo "End '$runId' "
done
mv rs* results
echo "Experiment FINISHED"
#pkill -f java