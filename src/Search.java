import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Search {

  private static final String TEMP_FILE = "book.html";

  private ArrayList<String> links = new ArrayList<>();
  private HashMap<String, LinkedList<Node>> words = new HashMap<>();
  private ArrayList<String> keywordIndex = null;
  private ArrayList<Map.Entry<String, Integer>> wordFrequencyIndex = null;

  private class Node {
    boolean isCapitalized;
    int position;

    private Node(boolean b, int i) {
      isCapitalized = b;
      position = i;
    }
  }

  public String getHTMLString(String urlString) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    File newFile = new File(TEMP_FILE);
    if (newFile.exists()) {
      try (Stream<String> stream = Files.lines(Paths.get(TEMP_FILE), StandardCharsets.UTF_8))
      {
        stream.forEach(s -> stringBuilder.append(s).append("\n"));
      }
    } else {
      URL url = new URL(urlString);
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          stringBuilder.append(line).append("\n");
        }
        Files.write(Paths.get(TEMP_FILE), stringBuilder.toString().getBytes());
      }
    }
    return stringBuilder.toString();
  }

  public void getLinksAndBodyText(String htmlString) {
    Document doc = Jsoup.parse(htmlString);
    Elements hrefs = doc.select("a[href]");
    for (Element href : hrefs) {
      if (!href.attr("abs:href").toString().trim().isEmpty()) {
        links.add(href.attr("abs:href"));
      }
    }
    String[] splits = doc.body().text().split("\\s+");
    int positionCount = 0;
    for (String s : splits) {
      String keyword = s.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "");
      if (!keyword.isEmpty() && keyword.matches("[a-zA-Z].*")) {
        if (words.containsKey(keyword.toLowerCase())) {
          words.get(keyword.toLowerCase()).add(new Node(keyword.matches("[A-Z].*"), positionCount));
        } else {
          LinkedList<Node> newList = new LinkedList<>();
          newList.add(new Node(keyword.matches("[A-Z].*"), positionCount));
          words.put(keyword.toLowerCase(), newList);
        }
      }
      positionCount++;
    }
  }

  public void sortKeywordIndex() {
    keywordIndex = new ArrayList<>(words.keySet());
    Collections.sort(keywordIndex);
  }

  public void sortWordFrequencyIndex() {
    wordFrequencyIndex = new ArrayList<>();
    for (String key : words.keySet()) {
      wordFrequencyIndex.add(new AbstractMap.SimpleEntry(key, words.get(key).size()));
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

  private String printNodeInfo(LinkedList<Node> nodeList) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Node n : nodeList) {
      sb.append("(");
      sb.append(n.position);
      sb.append(",");
      sb.append(n.isCapitalized ? "Y" : "N");
      sb.append("),");
    }
    sb.setLength(Math.max(sb.length() - 1, 0));
    sb.append("]");
    return sb.toString();
  }

  public void printResult(int printNumber) {
    int count;
    System.out.println("\nLinks: " + links.size());
    for (String link : links) {
      System.out.println(link);
    }
    System.out.printf("\nTop %d words sorted in alphabetical order, format:[(position, isCapitalized)] :\n", printNumber);
    count = 0;
    for (String keyword : keywordIndex) {
      count++;
      if (count > printNumber) {
        break;
      }
      System.out.printf("%s : %s \n", keyword, printNodeInfo(words.get(keyword)));
    }
    System.out.printf("\nTop %d words sortecd in frequency order: \n", printNumber);
    count = 0;
    for (Map.Entry entry : wordFrequencyIndex) {
      count++;
      if (count > printNumber) {
        break;
      }
      System.out.printf("%s : %s \n", entry.getKey(), words.get(entry.getKey()).size());
    }
  }

  public static void main(String[] args){
    if (args.length < 2 || args[0].equals("--help")) {
      System.out.println("Usage: 'java Search URL print',"
        + " the URL 'parameter' should be a link to search,"
        + " and the 'print' parameter should be top number of list to print.");
      return;
    }
    int printNumber = 0;
    try {
      printNumber = Integer.parseInt(args[1]);
    }
    catch (NumberFormatException e) {
      System.out.println("Print Number parameter Error: " + e.getMessage());
      System.exit(1);
    }
    Search newSearch = new Search();
    String html = "";
    try {
      html = newSearch.getHTMLString(args[0]);
    }
    catch (IOException e) {
      System.out.println("Reading URL Error: " + e.getMessage());
      System.exit(2);
    }

    newSearch.getLinksAndBodyText(html);
    newSearch.sortKeywordIndex();
    newSearch.sortWordFrequencyIndex();
    newSearch.printResult(printNumber);
  }
}