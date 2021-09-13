package sol

import src.{FileIO, IQuerier, PorterStemmer, StopWords}
import java.io.{BufferedReader, InputStreamReader}
import scala.collection.mutable.HashMap

/**
 * Class for a querier REPL that uses index files built from a corpus.
 *
 * @param titleIndex    - the filename of the title index
 * @param documentIndex - the filename of the document index
 * @param wordIndex     - the filename of the word index
 * @param usePageRank   - true if PageRank is to be used to rank results
 */
class Querier(titleIndex: String, documentIndex: String, wordIndex: String,
              usePageRank: Boolean) extends IQuerier {
  // Create titles.txt hashmap
  val titles: HashMap[Int, String] = new HashMap()
  FileIO.readTitles(titleIndex,titles)
  // Create docs.txt hashmap
  val docs: HashMap[Int, Double] = new HashMap()
  FileIO.readDocsFile(documentIndex,docs)
  // Create words.txt hashmap
  val map: HashMap[String, HashMap[Int, Double]] = new HashMap()
  FileIO.readWordsFile(wordIndex,map)

  @ Override
  override def getResults(query: String): List[Int] = {

    // Splits the query into an array of keywords
    val keywords: Array[String] = query.split(" ")
    // Data structure to keep track of total document scores across all keywords (id -> score)
    val docScores: HashMap[Int, Double] = new HashMap()
    // Array to keep track of the top ten highest scoring documents
    var topTen: Array[Int] = new Array(10)
    // Variable to keep track of the next open space in the array
    var openSpace: Int = 0

    // Method to edit scores based on pageRank
    def getPageRank(id: Int): Double = {
      // If using pageRank, we will multiply term-relevance scores
      //   with the pageRank score. Otherwise, multiply them by 1
      if (usePageRank) docs(id) else 0
    }

    // Method to compare document scores
    def compareDocScores(a: Int, b: Int): Boolean = {
      if (docScores.contains(a) && docScores.contains(b)) {
        docScores(a) > docScores(b)
      } else if (docScores.contains(a)) {
        true
      } else {
        false
      }
    }

    // Iterate over the keywords in the query
    for (term <- keywords) {
      // make the term lower case
      var word = term.toLowerCase
      // Check whether it is a stop word
      if (!StopWords.isStopWord(word)) {
        // If so, stem the word
        word = PorterStemmer.stem(word)

        // Check whether the stemmed word is in our Hashmap
        if (map.contains(word)) {

          // Iterate over the document id's associated with the word
          for (id <- map(word).keys) {
            // Checks if we already encountered the document id
            if (docScores.contains(id)) {
              // Calculates and updates the new score for the document
              val newScore: Double = docScores(id) + (map(word)(id) + getPageRank(id))
              docScores.update(id, newScore)

              // Checks if the document is in the top ten
              if (topTen.contains(id)) {
                // If it is, re-sort the top ten now that it has an updated score
                topTen = topTen.sortWith((a,b) => compareDocScores(a,b))

                // Checks if the document's score is higher than the 10th highest score
              } else if (docScores(id) > docScores(topTen(9))) {
                // If so, replace the 10th highest and re-sort the top ten
                topTen(9) = id
                topTen = topTen.sortWith((a,b) => compareDocScores(a,b))
              }
              // If we haven't already encountered the document id, then:
            } else {
              // Add the document and its score to docScores
              docScores.put(id, map(word)(id) + getPageRank(id))

              // Checks if the top ten has less than 10 elements
              if (openSpace != 10) {
                // If so, add the document to the next open space
                topTen(openSpace) = id

                // Re-sort top ten
                topTen = topTen.sortWith((a,b) => compareDocScores(a,b))

                // Update the next openSpace
                openSpace = openSpace + 1

                // Checks if the document's score is higher than the 10th highest score
              } else if (docScores(id) > docScores(topTen(9))) {
                // If so, replace the 10th highest and re-sort the top ten
                topTen(9) = id
                topTen = topTen.sortWith((a,b) => compareDocScores(a,b))
              }
            }
          }
        }
      }
    }
    if (openSpace < 10) {
      if (openSpace == 0) {
        return List()
      } else {
        for (i <- openSpace until topTen.length) {
          topTen(i) = -1
        }
      }
    }
    // Return the top ten
    topTen.toList
  }

  @ Override
  override def runRepl(): Unit = {
    // Create BufferedReader to read input stream
    val breader = new BufferedReader(new InputStreamReader(System.in))

    // Print the prompt
    System.out.print("search>")

    // Initialize line variable
    var line = breader.readLine()

    // Loop until the user inputs null (^D or ":quit")
    while (line != null) {
      if (line.equals(":quit")) {
        line = null
      } else {
        // Stores results of getResults in a variable
        val results: List[Int] = getResults(line)

        // Checks if there are no results
        if (results.isEmpty) {
          // Print to console
          System.out.println("No search results found")

          // Print the prompt
          System.out.print("search>")
          // Re-prompt user for input
          line = breader.readLine()

          // If there are results, then:
        } else {
          // Loop over the indices of the results
          for (i <- results.indices) {
            // Variable to keep track of document rank (1-10 instead of 0-9)
            val num: Int = i + 1
            if (results(i) >= 0) {
              // Print to console
              System.out.println(num + ". " + titles(results(i)))
            }
          }

          // Print the prompt
          System.out.print("search>")
          // Re-prompt user for input
          line = breader.readLine()
        }
      }
    }
    // Closes the BufferedReader
    breader.close()
  }
}

object Querier {
  /**
   * Runs the querier REPL.
   * @param args args of the form [--pageRank (optional), titlesIndex, docsIndex,
   *             wordsIndex]
   */
  def main(args: Array[String]): Unit = {
    // Checks whether to use pageRank or not
    if (args(0) == "--pagerank") {
      val querier = new Querier(args(1),args(2),args(3),true)
      querier.runRepl()
    } else {
      val querier = new Querier(args(0),args(1),args(2),false)
      querier.runRepl()
    }
  }
}
