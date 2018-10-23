#Twitter Search API extractor for Information Retrieval Project 1

#NYC: 40.712776,-74.005974
#Delhi: 28.704060,77.102493
#Bangkok: 13.756331,100.501762
#Mexico: 23.634501,-102.552788
#Paris: 23.634501,-102.552788

echo "Starting Twitter search script"
basePath=/home/castelinolinus/Desktop/Project1_Task1_Tweets

tweetRequestsCount=1
inputCountry=$1
inputLang=$2
latlongcords=0

searchFunc(){
	echo -e "Querying $1-$2 in $latlongcords \t\t Tweet requests count - $tweetRequestsCount"
	timestamp=`date +"%Y%m%d%H%M%S"`
	query="/1.1/search/tweets.json?q=$2&geocode=${latlongcords},10mi&count=100&lang=${inputLang}"
	twurl "$query" > ${basePath}/$1/${2// /_}_${timestamp}_$3_${inputLang}.json
	tweetRequestsCount=$(($tweetRequestsCount + 1))
}

if [ "$inputCountry" == "NYC" ]; then
	latlongcords="40.712776,-74.005974"
elif [ "$inputCountry" == "Delhi" ]; then
	latlongcords="28.704060,77.102493"
elif [ "$inputCountry" == "Bangkok" ]; then
	latlongcords="13.756331,100.501762"
elif [ "$inputCountry" == "Mexico" ]; then
	latlongcords="23.634501,-102.552788"
elif [ "$inputCountry" == "Paris" ]; then
	latlongcords="23.634501,-102.552788"
fi
echo "Set latlongcords to $latlongcords"



sleepTimer=0
while true
do
	for i in "environment" "crime" "politics" "infrastructure" "social_unrest"
	do
		if [ "$i" == "environment" ]; then
			for k in "environment" "pollution" "air quality" "storm" "drought" "solar flare" "smog" "global warming" "landslide" "acid rain" "urban sprawl" "recycle" "waste disposal" "ozone layer depletion" "water pollution" "climate change"
			do
				searchFunc "$i" "$k" "$1"
			done
		elif [ "$i" == "crime" ]; then
			for k in "crime" "assault" "robbery" "murder" "homicide" "burglary" "theft" "terrorism" "larceny" "racist attack"
			do
				searchFunc "$i" "$k" "$1"
			done
		elif [ "$i" == "politics" ]; then
			for k in "politics" "diplomat" "government" "president" "POTUS" "Trump" "Narendra Modi" "Rahul Gandhi" "political campaign" "political turmoil"
			do
				searchFunc "$i" "$k" "$1"
			done
		elif [ "$i" == "infrastructure" ]; then
			for k in "infrastructure" "rail construction" "road construction" "architecture" "monuments" "power generation" "sanitation" "water supply" "electricity" "ration"
	 		do
				searchFunc "$i" "$k" "$1"
			done
		elif [ "$i" == "social_unrest" ]; then
			for k in "social unrest" "strike" "protest" "epidemic" "riot" "civic emergency" "civil disturbance"
	 		do
				searchFunc "$i" "$k" "$1"
			done
		fi
	done

	sleepTimer=$(($sleepTimer + 1))
	if [ $sleepTimer == 3 ]; then
		echo "Sleeping for 15 minutes now"
		sleep 900
		sleepTimer=0		
		echo "Recovered from sleep. Sleep timer set to $sleepTimer"
	fi
done 

echo "Completed executing script - $tweetRequestsCount tweets collected"