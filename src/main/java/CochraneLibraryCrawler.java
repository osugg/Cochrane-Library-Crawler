import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class CochraneLibraryCrawler {

    private static File outputFile;

    public static void main(String[] args) {
        String baseUrl = "https://www.cochranelibrary.com/cdsr/reviews/topics";

        Scanner sc = new Scanner(System.in);
        System.out.println("\nEnter the name of a topic you would like to parse (case sensitive)" +
                ", or enter 'exit' to stop the program.");
        String topic = sc.nextLine();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36")
                .build();
        ResponseHandler<String> responseHandler = new MyResponseHandler();

        while(!topic.equals("exit")){

            //make GET request to topics page
            HttpGet topicsGet = new HttpGet(baseUrl);
            try{
                //parse topics page and get URL of specified topic
                String topicsResponse = client.execute(topicsGet, responseHandler);
                String topicSearchUrl = parseTopicsPage(topicsResponse, topic);
                if(topicSearchUrl == null){
                    System.out.println("Topic could not be found.");
                } else{
                    System.out.println("Enter the name of the .txt file you would like to use for output " +
                            "(overwrites existing files)");
                    String outputFilename = sc.nextLine();
                    while(!outputFilename.endsWith(".txt")){
                        System.out.println("Please enter a file with a .txt extension. (eg. output.txt)");
                        outputFilename = sc.nextLine();
                    }

                    outputFile = new File(outputFilename);
                    if(outputFile.exists()){
                        outputFile.delete();
                        outputFile = new File(outputFilename);
                    }

                    System.out.println("Parsing reviews...");

                    //get HTTP document of topic page
                    HttpGet topicGet = new HttpGet(topicSearchUrl);
                    String topicResponse = client.execute(topicGet, responseHandler);

                    //parse all pages of topic
                    parseReviews(topicResponse, topic, client, responseHandler);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("\nEnter the name of a topic you would like to parse (case sensitive), " +
                    "or enter 'exit' to stop the program.");
            topic = sc.nextLine();
        }
        try{
            client.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Gets the search URL for a given topic (from the main topics page), returns null if topic is not found
     * @param response HTTP document for the topics page
     * @param topic    name of the topic to be crawled
     * @return         string containing the search url for the given topic
     */
    private static String parseTopicsPage(String response, String topic){
        Document doc = Jsoup.parse(response);

        Element content = doc.getElementById("content");
        Elements links = content.getElementsByTag("a");
        for(Element link : links) {
            Elements buttons = link.getElementsByTag("button");
            String topicName;
            for(Element button : buttons){
                topicName = button.text();
                if(topicName.equals(topic)){
                    return link.attr("href");
                }
            }
        }
        return null;
    }

    /**
     * Recursively parses all pages of reviews under the specified topic; calls writeOutput() to write each review
     * @param response          the HTML content of the page
     * @param topic             the specified topic
     * @param client            the HTTP client, used to make GET request to next page of reviews
     * @param responseHandler   ResponseHandler object, used to handle response of GET requests
     */
    private static void parseReviews(String response, String topic, HttpClient client,
                                     ResponseHandler<String> responseHandler){
        Document doc = Jsoup.parse(response);

        //information to be passed to file writer (along with @param topic)
        String title, url, author, date;

        //get content div, find all item body elements
        Element content = doc.getElementById("content");
        Elements entries = content.getElementsByClass("search-results-item-body");
        for(Element entry : entries){
            //get article url and title
            Element titleElement = entry.getElementsByClass("result-title").get(0);
            url = titleElement.getElementsByTag("a").get(0).attr("href");
            title = titleElement.getElementsByTag("a").get(0).text();

            //get authors
            Element authorElement = entry.getElementsByClass("search-result-authors").get(0).child(0);
            author = authorElement.text();

            //get date
            Element metadataElement = entry.getElementsByClass("search-result-metadata-block").get(0);
            Element dateElement = metadataElement.getElementsByClass("search-result-date").get(0).child(0);
            date = dateElement.text();

            writeOutput(url, topic, title, author, date);
        }

        String nextPageUrl = getNextPage(content);
        if(nextPageUrl == null) {
            System.out.println("Finished parsing reviews");
            return;
        }

        try{
            HttpGet nextPageGet = new HttpGet(nextPageUrl);
            String nextPageResponse = client.execute(nextPageGet, responseHandler);
            parseReviews(nextPageResponse, topic, client, responseHandler);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Gets URL of next page of reviews
     * @param content   the content div of the current page
     * @return          string containing the url, or null if there is no next page
     */
    private static String getNextPage(Element content){
        Element nextButton = content.getElementsByClass("pagination-next-link").get(0);
        if(nextButton.childrenSize() > 0){
            return nextButton.child(0).attr("href");
        }
        return null;
    }

    /**
     * Writes a single entry to the specified file, with the data delimited by "|"
     * @param url       URL of review
     * @param topic     topic the review is found under
     * @param title     title of the review
     * @param author    author/authors of the review
     * @param date      date the review was published
     */
    private static void writeOutput(String url, String topic, String title, String author, String date) {
        try{
            FileWriter writer = new FileWriter(outputFile, true);
            writer.write(url + "|" + topic + "|" + title + "|" + author + "|" + date + "\n");
            writer.flush();
            writer.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
