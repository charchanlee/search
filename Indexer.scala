package sol

import src.{FileIO, PorterStemmer, StopWords}
import scala.collection.mutable.HashMap
import scala.util.matching.Regex
import scala.xml.{Node, NodeSeq}

/**
 * Provides an XML indexer, produces files for a querier
 *
 * @param inputFile - the filename of the XML wiki to be indexed
 */
class Indexer(val inputFile: String) {

  val root: Node = xml.XML.loadFile(inputFile)
  // this will go into titles.txt
  var titles: HashMap[Int, String] = new HashMap()
  // intermediate hashmap to link titles to their corresponding ids
  var ids: HashMap[String, Int] = new HashMap()
  // this will go into words.txt
  var map: HashMap[String, HashMap[Int, Double]] = new HashMap()
    // intermediate data structure -> will help us calculate page rank (essentially a graph)
  var links: HashMap[Int, List[Int]] = new HashMap()
  val pageSeq: NodeSeq = root \ "page"

  // keeps track of the most frequently seen term in each document
  var freqTerms: HashMap[Int, Double] = new HashMap()

  // The hashmap for docs.txt
  val pageRanks: HashMap[Int, Double] = new HashMap()

  // helper function to transform word term counts into word relevance scores
  def wordRel(): Unit =  {
    // loop through each word
    for (word <- map.keys) {
      // loop through each document where that word is found
      for (document <- map(word).keys) {
        // update that value in map (previously count) to tf*idf score
        map(word).update(document, map(word)(document) / freqTerms(document) * Math.log(titles.size.toDouble / map(word).size.toDouble))
      }
    }
  }


  for (page <- pageSeq) {
    // gives the title in string form
    val title: String = (page \ "title").text.trim
    // gives the ID in int form
    val id: Int = (page \ "id").text.trim.toInt
    // Adds the id and title to the titles hashmap
    titles.put(id, title)
    ids.put(title, id)
  }

  for (page <- pageSeq) {
    // regex expression that looks for links, words with and without apostrophes
    val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")
    // Find all matches in the page text plus the title text
    val matchesIterator = regex.findAllMatchIn((page \ "title").text + " " + (page \ "text").text)
    // Turn the matches into an iterable list
    val matchesList = matchesIterator.toList.map { aMatch => aMatch.matched }

    // The id of the current page
    val id: Int = (page \ "id").text.trim.toInt

    // Initialize variable to hold the most popular term's frequency in this document
    var freqTermCount: Int = 0


    for (term <- matchesList) {

      // Initialize the word variable to be used later
      var word: String = null

      // If the term is a link, deal with it accordingly
      if (term.matches("""\[\[[^\[]+?\]\]""")) {
        var linkTitle: String = null
        // determine the id of the destination page -> use a regex expression to extract the text inside brackets
        if (term.matches("""\[\[Category:\w+[\s\w+]*\]\]""")) { // Matches [[Category:Computer Science]]
          linkTitle = term.replaceAll("""\[\[Category:""", "").replaceAll("""\]\]""", "")
          word = linkTitle
        } else if (term.matches("""\[\[\w+[\s\w+]*\|\w+[\s\w+]*\]\]""")) { // Matches [[Presidents|Washington]]
          linkTitle = term.replaceAll("""\[\[""", "").replaceAll("""\|\w+[\s\w+]*\]\]""", "")
          word = term.replaceAll("""\[\[\w+[\s\w+]*\|""","").replaceAll("""\]\]""","")
        } else { // Matches [[Hammer]]
          linkTitle = term.replaceAll("""\[\[""", "").replaceAll("""\]\]""", "")
          word = linkTitle
        }

        // MAINTAINING THE LINKS HASHMAP
        if (ids.contains(linkTitle) || ids.contains(linkTitle.toLowerCase())) {
          val linkedID: Int = ids(linkTitle) // linked title - title of the page being linked to
          // if this doc has been linked to before, add it
          if (links.contains(id)) {
            // if this document has NOT been linked to before specifically by the current page
            if (!links(id).contains(linkedID) && id != linkedID) {
              links(id) = links(id).:+(linkedID)
            }
          } else if (id != linkedID) {
            // add a binding for this doc in the hashmap and add the linkedID to its list
            links.put(id, List(linkedID))
          }
        }
      } else {
        // If term is NOT a link, set it equal to word
        word = term
      }

      // MAINTAINING THE MAP HASHMAP

      // Split up word if it matched a phrase instead of a single term
      for (t <- word.split(" ")) {
        // Make sure the term is lower case
        word = t.toLowerCase()

        // we will only work on non-stop words on the page
        if (!StopWords.isStopWord(word)) {
          // stemming
          word = PorterStemmer.stem(word)
          if (map.contains(word)) {
            if (map(word).contains(id)) {
              // If the map contains the word, update the count
              val count: Double = map(word)(id)
              map(word).update(id, count + 1)
              // If this term's frequency is higher than the max, update the max
              if ((count + 1) > freqTermCount) {
                freqTermCount = (count + 1).toInt
              }
            } else {
              map(word).put(id, 1)
              if (freqTermCount == 0) {
                freqTermCount = 1
              }
            }
          } else {
            // If it doesn't, put it in the hashmap and update its count to 1
            map.put(word, new HashMap[Int, Double])
            map(word).update(id, 1)
            if (freqTermCount == 0) {
              freqTermCount = 1
            }
          }
        }
      }
    }
    // at the end of looking at each document, record the count for most frequently occurring term in that document
    freqTerms.put(id, freqTermCount.toDouble)

  }

  // Method to rank the pages and populate the pageRank hashmap
  def pageRank(): Unit = {
    // n = total number of documents
    val n: Double = titles.size.toDouble
    // r0 = initial array to hold previous iteration values
    val r0: Array[Double] = Array.fill(n.toInt) {0}
    // r1 = initial array to hold current iteration values
    val r1: Array[Double] = Array.fill(n.toInt) {1 / n}

    // Method to find euclidean distance
    def euclideanDistance(r0: Array[Double], r1: Array[Double]): Double = {
      var sum: Double = 0
      for (i <- r0.indices) {
        sum = sum + (r1(i) - r0(i)) * (r1(i) - r0(i))
      }
      Math.sqrt(sum)
    }

    // Method to find the weight that page k gives to page j
    def weight(e: Double, k: Int, j: Int): Double = {
      if (!links.contains(k) && k != j) {
        (e / n) + (1 - e) * (1 / (n - 1))
      } else if (k != j && links(k).contains(j)) {
        (e / n) + (1 - e) * (1 / links(k).size.toDouble)
      } else {
        e / n
      }
    }

    // Array of document ids who's indices will correspond with those of r0 and r1
    val keys: Array[Int] = titles.keys.toArray

    // Loop until the euclidean distance between iterations is greater than 0.001
    while (euclideanDistance(r0, r1) > 0.001) {
      // Set every element of r0 = every element of r1
      for (i <- r0.indices) {
        r0(i) = r1(i) // r0 <- r1
      }
      // For every document id that corresponds to index j
      for (j <- keys.indices) {
        // Reset the score for document j to 0
        r1(j) = 0
        // For every document id that corresponds to index k
        for (k <- keys.indices) {
          // Re-score document j's authority
          r1(j) = r1(j) + weight(0.15, keys(k), keys(j)) * r0(k)
        }
      }
    }

    // Place all documents ids and their corresponding pagerank score in the hashmap
    for (i <- keys.indices) {
      pageRanks.put(keys(i), r1(i))
    }
  }

  // call pageRank to populate the hashmap for docs.txt
  pageRank()
  // call wordRel function to transform counts -> score (void helper function, see line 40)
  wordRel()
}


object Indexer {
  /**
   * Processes a corpus and writes to index files.
   * @param args args of the form [WikiFile, titlesIndex, docsIndex, wordsIndex]
   */
  def main(args: Array[String]): Unit = {
    // Instantiate new Indexer for the WikiFile
    val indexer = new Indexer(args(0))
    // Write to titles.txt
    FileIO.writeTitlesFile(args(1), indexer.titles)
    // Write to docs.txt
    FileIO.writeDocsFile(args(2), indexer.pageRanks)
    // Write to words.txt
    FileIO.writeWordsFile(args(3), indexer.map)
  }
}
