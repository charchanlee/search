package sol
import tester.Tester

class Testing {

  def testPageRank(t: Tester): Unit = {
    val test1: Indexer = new Indexer ("wikis/SmallWiki.xml")
    val test2: Indexer = new Indexer ("wikis/MedWiki.xml")
    val test3: Indexer = new Indexer ("wikis/BigWiki.xml")
    val test4: Indexer = new Indexer ("wikis/PageRankWiki.xml")
     //Test to see if pageRank sums equals to 1
    val sumSmallWiki: Double = test1.pageRanks.values.foldLeft(0.0)((a, b) => a + b)
    val sumMedWiki: Double = test2.pageRanks.values.foldLeft(0.0)((a, b) => a + b)
    val sumBigWiki: Double = test3.pageRanks.values.foldLeft(0.0)((a, b) => a + b)
    t.checkExpect(Math.abs(sumSmallWiki - 1) < 0.00001, true)
    t.checkExpect(Math.abs(sumMedWiki - 1) < 0.00001, true)
    t.checkExpect(Math.abs(sumBigWiki - 1) < 0.00001, true)

  }
  def testTfIdf(t: Tester): Unit = {
    val test7: Indexer = new Indexer("wikis/TestWiki.xml")

    /* For TestWiki.xml:
        -term frequency: 1 -> 5 B's, 3 C's, 2 D's
                         2 -> 3 B's, 5 C's, 3 D's
                         3 -> 1 D's
        -normalized: 1 -> 5/5 B's, 3/5 C's, 2/5 D's
                     2 -> 3/5 B's, 5/5 C's, 3/5 D's
                     3 -> 1/1 D's
        -idf: B: log(3/2)
              C: log(3/2)
              D: log(3/3)
        -tf*idf: 1 -> 1*0.4055 B's, 0.6*0.4055 C's, 0.4*0 D's
                 2 -> 0.6*0.4055 B's, 1*0.4055 C's, 0.6*0 D's
                 3 -> 1*0 D's
        -tf*idf: 1 -> 0.4055 B's, 0.2433 C's, 0.0 D's
                 2 -> 0.2433 B's, 0.4055 C's, 0.0 D's
                 3 -> 0.0 D's
    */

    t.checkExpect(Math.abs(test7.map("b")(1) - 0.4055) < 0.0001)
    t.checkExpect(Math.abs(test7.map("b")(2) - 0.2433) < 0.0001)
    t.checkExpect(Math.abs(test7.map("c")(1) - 0.2433) < 0.0001)
    t.checkExpect(Math.abs(test7.map("c")(2) - 0.4055) < 0.0001)
    t.checkExpect(Math.abs(test7.map("d")(1) - 0.0) < 0.0001)
    t.checkExpect(Math.abs(test7.map("d")(2) - 0.0) < 0.0001)
    t.checkExpect(Math.abs(test7.map("d")(3) - 0.0) < 0.0001)
  }

}
object main extends App {
  Tester.run(new Testing())
}