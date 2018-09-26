import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Collections;
import java.util.AbstractMap;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A class for Search words in the web page.
 */
public class Search {

  /**
   * File name and path for storing the web page.
   */
  private static final String TEMP_FILE = "book.html";

  /**
   * total links in that web page.
   */
  private ArrayList<String> links = new ArrayList<>();
  /**
   * total words in that web page.
   */
  private HashMap<String, LinkedList<Node>> words = new HashMap<>();
  /**
   * An alphabetical order index of words.
   */
  private ArrayList<String> keywordIndex = null;
  /**
   * A frequency order index of words.
   */
  private ArrayList<Map.Entry<String, Integer>> wordFrequencyIndex = null;

  /**
   * An inner class for storing information of each found word.
   */
  private final class Node {
    /**
     * A flag of is the word appeared capitalized.
     */
    private boolean isCapitalized;
    /**
     * The position of the word in the body of the web page.
     */
    private int position;

    /**
     * Node class constructor.
     * @param b value for isCapitalized
     * @param i value for position
     */
    private Node(final boolean b, final int i) {
      isCapitalized = b;
      position = i;
    }

  }

  /**
   * A function to get the html content from a url.
   * Store the html content to a local file to avoid hammering
   *
   * @param urlString a string of url
   * @return the html content as a String
   * @throws IOException if reading file fail or accessing web page fail
   */
  public String getHTMLString(final String urlString) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    // try to open the local file first
    File newFile = new File(TEMP_FILE);
    if (newFile.exists()) {
      // if the file exists, read the content from the file
      try (Stream<String> stream =
             Files.lines(Paths.get(TEMP_FILE), StandardCharsets.UTF_8)) {
        stream.forEach(s -> stringBuilder.append(s).append("\n"));
      }
    } else {
      // if the file not exist, fetch the content from the web page
      URL url = new URL(urlString);
      try (BufferedReader bufferedReader
             = new BufferedReader(new InputStreamReader(url.openStream()))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          stringBuilder.append(line).append("\n");
        }
        // store the content to the local file
        Files.write(Paths.get(TEMP_FILE), stringBuilder.toString().getBytes());
      }
    }
    return stringBuilder.toString();
  }

  /**
   * A function to parse the html content with Jsoup.
   * Store the parsing result into data members links and words
   *
   * @param htmlString a string of url
   */
  public void getLinksAndBodyText(final String htmlString) {
    Document doc = Jsoup.parse(htmlString);
    Elements hrefs = doc.select("a[href]");
    // parse the absolute links
    for (Element href : hrefs) {
      if (!href.attr("abs:href").toString().trim().isEmpty()) {
        links.add(href.attr("abs:href"));
      }
    }
    // parse all the words
    String[] splits = doc.body().text().split("\\s+");
    int positionCount = 0; // position counter
    for (String s : splits) {
      // get rid of punctuation immediately before and after words
      String keyword = s.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
      if (!keyword.isEmpty() && keyword.matches("[a-zA-Z].*")) {
        if (words.containsKey(keyword.toLowerCase())) {
          words.get(keyword.toLowerCase())
            .add(new Node(keyword.matches("[A-Z].*"), positionCount));
        } else {
          LinkedList<Node> newList = new LinkedList<>();
          newList.add(new Node(keyword.matches("[A-Z].*"), positionCount));
          words.put(keyword.toLowerCase(), newList);
        }
      }
      positionCount++;
    }
  }

  /**
   * A function to sort the words in alphabetical order.
   * Store sorted index in data member keywordIndex
   */
  public void sortKeywordIndex() {
    keywordIndex = new ArrayList<>(words.keySet());
    Collections.sort(keywordIndex);
  }

  /**
   * A function to sort the words in frequency order.
   * Store sorted index in data member wordFrequencyIndex
   */
  public void sortWordFrequencyIndex() {
    wordFrequencyIndex = new ArrayList<>();
    for (String key : words.keySet()) {
      wordFrequencyIndex
        .add(new AbstractMap.SimpleEntry(key, words.get(key).size()));
    }
    Collections.sort(wordFrequencyIndex, (m1, m2) -> {
      if (m1.getValue() == m2.getValue()) {
        return 0;
      } else if (m1.getValue() < m2.getValue()) {
        return 1;
      } else {
        return -1;
      }
    });
  }

  /**
   * A function to put all information related to a word.
   * Including position info and isCapitalized info.
   * put those information in a nice format.
   *
   * @param nodeList a list of node
   * @return a formatted string
   */
  private String printNodeInfo(final LinkedList<Node> nodeList) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Node n : nodeList) {
      sb.append("(");
      sb.append(n.position);
      sb.append(",");
      if (n.isCapitalized) {
        sb.append("Y");
      } else {
        sb.append("N");
      }
      sb.append("),");
    }
    sb.setLength(Math.max(sb.length() - 1, 0));
    sb.append("]");
    return sb.toString();
  }

  /**
   * A function to print out the search results.
   *
   * @param printNumber a integer to indicate how many top records to print
   */
  public void printResult(final int printNumber) {
    int count;
    System.out.println("\nLinks: " + links.size());
    for (String link : links) {
      System.out.println(link);
    }
    System.out.printf("\nTop %d words sorted in alphabetical order, "
      + "format:[(position, isCapitalized)] :\n", printNumber);
    count = 0;
    for (String keyword : keywordIndex) {
      count++;
      if (count > printNumber) {
        break;
      }
      System.out.printf("%s : %s \n", keyword,
        printNodeInfo(words.get(keyword)));
    }
    System.out.printf("\nTop %d words sortecd in frequency order: \n",
      printNumber);
    count = 0;
    for (Map.Entry entry : wordFrequencyIndex) {
      count++;
      if (count > printNumber) {
        break;
      }
      System.out.printf("%s : %s \n", entry.getKey(),
        words.get(entry.getKey()).size());
    }
  }

  /**
   * Main function of the class.
   *
   * @param args the URL 'parameter' and the 'print' parameter
   *             should be received from the command line
   */
  public static void main(final String[] args) {
    if (args.length < 2 || args[0].equals("--help")) {
      System.out.println("Usage: 'java Search URL print',"
        + " the URL 'parameter' should be a link to search,"
        + " and the 'print' parameter should be top number of list to print.");
      return;
    }
    int printNumber = 0;
    try {
      printNumber = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.out.println("Print Number parameter Error: " + e.getMessage());
      System.exit(1);
    }
    Search newSearch = new Search();
    String html = "";
    try {
      html = newSearch.getHTMLString(args[0]);
    } catch (IOException e) {
      System.out.println("Reading URL Error: " + e.getMessage());
      System.exit(2);
    }

    newSearch.getLinksAndBodyText(html);
    newSearch.sortKeywordIndex();
    newSearch.sortWordFrequencyIndex();
    newSearch.printResult(printNumber);
  }
}
