# Cochrane-Library-Crawler

### Overview

This program crawls the Cochrane Library reviews for a user-specified topic and outputs the metadata for each review into a text file. Upon startup, the user enters the topic
they would like to search. If the topic can be found, the user enters the name of the text file to which they would like to output results. If given a valid filename, the
program begins to crawl the topic and output the results. Once the topic is fully crawled, the user can choose another topic to crawl or exit the program. For reference, 
the topics can be found at the following URL: https://www.cochranelibrary.com/cdsr/reviews/topics.

### Design Reasoning

The crawler's logic can be divided into two main parts; accessing the topics page to get the reviews for the specified topic, and then accessing the review page, from which the 
data is scraped. Using Apache HTTPClient, a client is opened that will execute HTTP methods in order to get the page's HTML contents. In the first stage (accessing the topics page),
the client sends a GET request, passing the URL of the topics page. The server sends back a response, which is handled by a ResponseHandler object that returns the HTML content
in a string if the request is successful. Then, using the jsoup library, a DOM document is created from the HTML content. DOM methods can then be called on that object to find
all the links in the topic list. These links are iterated over until the specified topic is found. The href attribute of this link is then stored.

In the second stage (scraping the topic reviews), another GET request is made, using the stored topic URL. The response handler then returns the HTML content of the reviews page.
Similar to the first stage, a document object is made and parsed, getting the URL, title, author, topic, and date of the first review. These values are then sent to a method that 
writes the information to the text file. This is repeated until the last review visible on the page is scraped. The link for the 'next page' button is then accessed and a GET request 
is sent using that URL. The parsing method is then called recursively, passing in the HTML content of the next page. Once the last page is accessed and parsed, the crawler stops. 
Errors in getting the HTTP responses are handled, as well as with the file writing.

### Suggestions

This application could be expanded to retrieve more information from the Cochrane Library site. For example, each review's URL could be accessed, and information from the abstract
could be stored. 
