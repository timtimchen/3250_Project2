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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Search {

  private static final String TEMP_FILE = "book.html";

  private ArrayList<String> links = new ArrayList<>();
  private HashMap<String, LinkedList<Node>> words = new HashMap<>();

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
      String keyword = s.replaceAll("\"|\\p{Punct}+$", "");
      if (!keyword.isEmpty()) {
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

  public void printResult() {
    System.out.println("\nLinks: " + links.size());
    for (String link : links) {
      System.out.println(link);
    }
    System.out.println("\nword count: ");
    for (String keyword : words.keySet()) {
      System.out.printf("%s : %s \n", keyword, words.get(keyword).size());
    }
  }

  public static void main(String[] args){
    if (args.length < 1 || args[0].equals("--help")) {
      System.out.println("Usage: 'java Search URI', the URI parameter should be a URL or file name to search.");
      return;
    }
    Search newSearch = new Search();
    String html = "";
    try {
      html = newSearch.getHTMLString(args[0]);
    }
    catch (IOException e) {
      System.out.println("Reading URL Error: " + e.getMessage());
    }

    newSearch.getLinksAndBodyText(html);
    newSearch.printResult();
  }
}