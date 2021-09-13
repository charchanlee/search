Adam Bredvik and Charlotte Lee 
instructions describing how a user would interact with your program.
a brief overview of your design, including how the pieces of your program fit together
Indexer: 
There are four main hashmaps:
"titles" with page IDs as keys and title of the page as values 
"ids" which is the same as "titles" with the key and values reversed 
"map" which puts in each unique term in the corpus as a key, with a sub-hashmap as the value 
"links" which has the destination page ID as the key and a list of all IDs of pages that link
to that page as the value.
We take in an XML file as a node, and then looks through all of the children in the node 
sequence that are under the page tag. It goes through all pages and loads the information into 
the "titles" and "ids" hashmaps. 
Then we go through every page and remove the whitespace in the body text, inputting it into an 
array. we check to see if any of these terms match any of the three link forms (using RegEx). 
If we find a link, we add that tally to the links hashmap. 
Even if we have found a link, part of the link may still need to be treated as a word term, 
like a hyperlink. 
If it is not a link or it is a link that must be treated like a word term, we proceed to 
check if the word term has already been seen so far in the corpus, as well as whether that word
has occurred already in that document. We either create a new instance in the map(word) sub hashmap, 
or we update the count by 1. For each word in the document, we keep note of the frequency 
of the most frequently occuring word, and ensure that this is updated every time we see a new 
word. At the end of the document, we enter this maximum frequency into a hashmap (keys are 
document IDs, and values are the frequency of the most frequent term).
We have two main helper functions to help us calculate pageRank and word relevance according
to the equations on the project handout. All of the numbers are available in the intermediate
data structures (hashmaps) that we have created previously. We reused the "map" hashmap in order
to save space - even though this hashmap technically only requires integers, we realized that 
we would need the same hashmap to calculate word relevance, so we decided that the values would
store doubles instead, and we would erase and replace the values with the word relevance scores. 
Our querier works similarly to how our indexer works - we split the query into an array of keywords, 
and then iterate over all of them to make them lowercase, remove stop words, and stem them.
We read the files into the hashmaps to ensure that they are in the same format as how we sent them 
out of the indexer. We also have a hashmap, docScores, that keeps track of the document IDs (keys) 
and the scores (values). If "map" contains the stemmed word, then we iterate over the document IDs 
associated with that word (the sub hashmap). We check to see if we already have a score for that document
in docScores, and if so, we update that score based off of the updated information regarding the relevance 
of the document to the current query term. if the document has not been recorded in docScores, then weput 
a score in for it instead. In both situations, we check to see if the document score is high enough to be in 
the top 10 scores, which is storred in a sorted array with each element symbolizing the document ID, and the 
index representing the ranking. At the end of all of this work, we should return a list of 
the document IDs that are most relevant to the search terms, in order. 
In the REPL, we ask the user to input search terms until they quit. We take 
the results list, and extract the title of that document from the "titles" hashmap. In the REPL, we have a
helper function that helps us adjust our scores based on whether or not pageRank is being used. 


a description of any known bugs in your program
If the same term shows up in every document then total number of documents = n, total number of docs with the 
word = n, so log(n/n) = log(1) = 0 so you can’t search for terms that show up in everything because they will 
all have a score of 0
a description of how you tested your program.
We tested pageRank to make sure that all of the pageRank scores were summing up to 1 in all three sizes of wikis.
We also tested the td-idf score by creating our own xml file, which is included in our submission, and then calculated
these scores by hand (work is shown in the comments) and then wrote check expects to compare those. 
Aside from our tests in the testing file, we used the search engine provided to check our results. We searched up 
5 different terms without and without pageRank, and had a majority of the suggestions on the list (around 6-9) 
of the ones on the answer key list. 