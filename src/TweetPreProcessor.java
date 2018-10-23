import java.io.File;
import java.text.SimpleDateFormat;

import org.json.*;

import com.vdurmont.emoji.EmojiParser;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class TweetPreProcessor {
	
	public static void main(String arg[]) throws Exception {
		
		String srcDir = "C:\\Users\\Linus-PC\\Desktop\\IR Project 1\\";
		final int maxRetweetCount = 5000;
		final String TWITTER_DATE_FORMAT="EEE MMM dd HH:mm:ss ZZZZZ yyyy";
		String pattern = "yyyy-MM-dd'T'HH:mm:ssZ";
		
		Map<String, String> dirToTopicMapping = new HashMap<String, String>();
		dirToTopicMapping.put("environment", "environment");
		dirToTopicMapping.put("politics", "politics");
		dirToTopicMapping.put("infrastructure", "infra");
		dirToTopicMapping.put("crime", "crime");
		dirToTopicMapping.put("social_unrest", "social unrest");
		
		Map<String, String> cityMapping = new HashMap<String, String>();
		cityMapping.put("NYC", "nyc");
		cityMapping.put("Delhi", "delhi");
		cityMapping.put("Bangkok", "bangkok");
		cityMapping.put("Paris", "paris");
		cityMapping.put("Mexico", "mexico city");
			
		String extensions[] = {"json"};
		long tweets_count= 0L;
		int retweetCount= 0;
		for(String dirName : dirToTopicMapping.keySet()) {
			System.out.println("Accessing directory " + dirName );
			List<File> dirFiles = (List<File>)FileUtils.listFiles(new File(srcDir+dirName),extensions , true);

			for(Object fileObj : dirFiles.toArray()) {
				if(fileObj instanceof File) {
					File file = (File)fileObj;
					//System.out.println(file.getPath());
					String fileName = file.getName();
					int end = fileName.lastIndexOf("_");
					int start = fileName.lastIndexOf("_", end-1);
					String city = fileName.substring(start+1, end);
					String language = fileName.substring(end+1, end+3);
					
					String strJsonObj = FileUtils.readFileToString(file, "UTF-8");
					//System.out.println(strJsonObj);
					
					if(strJsonObj.startsWith("{") && strJsonObj.endsWith("}")) {
						JSONObject jsonObj = new JSONObject(strJsonObj);
						if(jsonObj.has("statuses")) {
							JSONArray tweetsArray =jsonObj.getJSONArray("statuses");
							if(tweetsArray.length() > 0) {
								//processing geocode
								JSONObject search_md = (JSONObject)jsonObj.get("search_metadata");
								String queryUrl = search_md.getString("refresh_url");
								int geoStart = queryUrl.indexOf("geocode=");
								int geoEnd = queryUrl.lastIndexOf("%2C");
								String coordinates = queryUrl.substring(geoStart+8, geoEnd);
								coordinates = coordinates.replace("%2C", ",");
								
								for(int i=0; i < tweetsArray.length(); i++) {
									JSONObject tweet = tweetsArray.getJSONObject(i);
									tweet.put("city", cityMapping.get(city));
									tweet.put("topic", dirToTopicMapping.get(dirName));
									tweet.put("tweet_lang", language);
									String tweet_text = tweet.getString("text");
									
									if(tweet_text.startsWith("RT @")) {
										retweetCount++;
										if(retweetCount > maxRetweetCount) {
											tweet_text = tweet_text.substring(3);
											tweet.remove("retweeted_status");
										}
									}
									tweet.put("tweet_text", tweet_text);
									List<String> emojis = EmojiParser.extractEmojis(tweet_text);
									if(!emojis.isEmpty()) {
										String extracted_emojis = "";
										for(String emoji : emojis) {
											extracted_emojis += emoji + " " ;
										}
										tweet.put("tweet_emoticons", extracted_emojis);
									}
									
									JSONObject entities = (JSONObject)tweet.get("entities");
									JSONArray mentions = entities.getJSONArray("user_mentions");
									if(mentions.length() > 0) {
										tweet_text = removeOccurrences(mentions, tweet_text, "mentions");
									}
									
									JSONArray urls = entities.getJSONArray("urls");
									if( urls.length() > 0) {
										tweet_text = removeOccurrences(urls, tweet_text, "urls");
									}
									
									JSONArray hashtags = entities.getJSONArray("hashtags");
									if(hashtags.length() > 0) {
										tweet_text = removeOccurrences(hashtags, tweet_text, "hashtags");
									}
									
									//double check
									tweet_text = tweet_text.replaceAll("http.*?\\s", "");    //removing all urls
									tweet_text = tweet_text.replaceAll("#[^\\\\s]*","");    //removing all hashtags
									tweet_text = tweet_text.replaceAll("@[^\\\\s]*","");    //removing all user mentions
									
									tweet_text= EmojiParser.removeAllEmojis(tweet_text);    //removing all emojis
									
									tweet.put("text_"+language, tweet_text);
									tweet.put("tweet_loc",coordinates);
									
									//parsing and formatting the date for Solr
									String tweet_created_at = tweet.getString("created_at");
									SimpleDateFormat sf = new SimpleDateFormat(TWITTER_DATE_FORMAT);
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
									Date d= sf.parse(tweet_created_at);
									String date = sdf.format(d);
									date = date.substring(0,date.indexOf(":"));
									String processed_date= date + ":00:00Z";
									tweet.put("tweet_date", processed_date);
									
									String processedJSON = tweet.toString();
									FileUtils.writeStringToFile(new File(srcDir+"processed1\\"+dirName+"\\"+fileName), processedJSON+"\n", "UTF-8", true);
									tweets_count++;
									
									if(tweets_count%1000 == 0)
										System.out.println(tweets_count+" tweets processed");
									//System.out.println(tweet.toString());
								}
								
							}
						}
					}
				}
			}
			
			System.out.println("Completed processing "+dirName);
		}
		
		System.out.print("Pre-processing completed!");
		
		
	}    //main()
	
	public static String removeOccurrences(JSONArray list, String tweet_text, String type) throws Exception {
		String preappend = "";
		if(type.equals("hashtags"))
			preappend = "#";
		if(type.equals("mentions"))
			preappend = "@"; 
		
		String replaceText = "";
		for(int i= 0; i<list.length(); i++) {
			JSONObject temp = list.getJSONObject(i);
			if(type.equals("hashtags"))
				replaceText = temp.getString("text");
			if(type.equals("urls"))
				replaceText = temp.getString("url");
			if(type.equals("mentions"))
				replaceText = temp.getString("screen_name");
			tweet_text = tweet_text.replaceAll(preappend+replaceText, "");
		}
		
		return tweet_text;
	}    //removeOccurrences
	
}    //class
