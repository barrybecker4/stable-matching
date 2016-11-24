package com.barrybecker4.matching.stableroommate

import scala.collection.immutable.TreeMap
import scala.collection.mutable

/**
  * Find stable roommate paring based on supplied preferences if possible.
  * There must be an even number of people, and no cycles that would prevent a stable pairing.
  * Code derived from https://en.wikipedia.org/wiki/Stable_roommates_problem
  */
class StableRoommateFinder {

  /** @return proposed pairings found by stable roommate algorithm */
  def findMatches(preferences: RoommatePreferences): Map[String, String] = {

    // Phase 1
    val reducedPrefs = findInitialPairings(preferences)
    println("\nreduced = \n" + reducedPrefs.map(x => x._1 + " -> " + x._2.mkString(", ")).mkString("\n")) // correct

    // Phase 3
    reduceBasedOnCycles(reducedPrefs)
    println("\nfinalReducedPrefs = \n" + reducedPrefs.map(x => x._1 + " -> " + x._2.mkString(", ")).mkString("\n"))

    findPairings(reducedPrefs)
  }

  /**
    * Participants propose to each other, in a manner similar to that of the Gale-Shapley algorithm for the stable marriage problem.
    * Consider two participants, person and candidate. If candidate holds a proposal from person, then we remove from candidates's
    * list all participants x after person, and symmetrically, for each removed participant x, we remove candidate from x's list,
    * so that candidate is first in person's list; and person, last in candidates's.
    * The resulting reduced set of preference lists together is called the Phase 1 table.
    * In this table, if any reduced list is empty, then there is no stable matching. Otherwise, the Phase 1 table is a stable table.
    */
  private def findInitialPairings(preferences: RoommatePreferences): mutable.Map[String, List[String]] = {

    var pairings = new TreeMap[String, String]
    var freePeople = preferences.people
    val reducedPrefs = mutable.Map[String, List[String]]() ++= preferences.prefers

    while (freePeople.nonEmpty) {
      val person = freePeople.head
      freePeople = freePeople.tail
      val prefs = reducedPrefs(person)
      var done = false
      var i = 0
      while (!done && i < prefs.length) {
        val candidate = prefs(i)
        if (!pairings.contains(candidate)) {
          pairings += candidate -> person
          rejectSymmetrically(person, candidate, reducedPrefs)
          done = true
        }
        else {
          val currentMatch = pairings(candidate)
          val candidatePrefs = reducedPrefs(candidate)
          if (candidatePrefs.indexOf(person) < candidatePrefs.indexOf(currentMatch)) {
            pairings += candidate -> person
            freePeople +:= currentMatch
            rejectSymmetrically(person, candidate, reducedPrefs)
            done = true
          }
        }
        i += 1
      }
    }

    println("\ninitial pairings = " + pairings.map(x => x._1 + " -> " + x._2).mkString("\n"))
    reducedPrefs
  }

  // remove from candidates's list all participants x after person
  private def rejectSymmetrically(person: String, candidate: String,
                                  reducedPrefs: mutable.Map[String, List[String]]): Unit = {
    val candidatePrefs = reducedPrefs(candidate)
    val (keep, drop) = candidatePrefs.splitAt(candidatePrefs.indexOf(person) + 1)
    reducedPrefs.put(candidate, keep)
    drop.foreach(x => {
      reducedPrefs.put(x, reducedPrefs(x).filter(_ != candidate))
    })
  }

  /**
    * T = Phase 1 table;
    * while (true) {
    *      identify a rotation r in T;
    *      eliminate r from T;
    *      if some list in T becomes empty,
    *          return null; (no stable matching can exist)
    *      else if (each reduced list in T has size 1)
    *          return the matching M = {{x, y} | x and y are on each other's lists in T}; (this is a stable matching)
    *  }
    * @return final reduced preferences
    */
  private def reduceBasedOnCycles(reducedPrefs: mutable.Map[String, List[String]]) = {
    var done = false

    while (!done) {
      // find a person with more than 1 person in their preference list
      val firstPerson = reducedPrefs.find(x => x._2.length > 1).map(_._1)
      if (firstPerson.isDefined) {
        var lastPrefsList: List[String] = List(firstPerson.get)
        var secondPrefsList: List[String] = List()

        // Repeat this process until someone has an empty preference list (no stable pairing),
        // or until everyone has exactly one person in their list (stable)

        // repeat until same person appears twice in one of the two lists
        while (!done) {
          if (reducedPrefs.get(lastPrefsList.last).size > 1) {
            val secondPref = reducedPrefs.get(lastPrefsList.last).tail.head.toString()
            if (secondPrefsList.contains(secondPref)) done = true // dupe name
            secondPrefsList :+= secondPref
            val lastPref = reducedPrefs(secondPref).last
            if (lastPrefsList.contains(lastPref)) done = true // dupe name
            lastPrefsList :+= lastPref
          }
          done = true  // there was no second preference
        }

        if (done) {
          lastPrefsList = lastPrefsList.tail // get the 2 lists to align
          while (lastPrefsList.nonEmpty) {
            val p1 = lastPrefsList.head
            val p2 = secondPrefsList.head
            reducedPrefs(p1) = reducedPrefs(p1).filter(_ != p2)
            reducedPrefs(p2) = reducedPrefs(p2).filter(_ != p1)
            lastPrefsList = lastPrefsList.tail
            secondPrefsList = secondPrefsList.tail
          }
        }
      } else done = true
    }
  }

  def findPairings(reducedPrefs: mutable.Map[String, List[String]]): Map[String, String] = {
    val pairings = mutable.Map[String, String]()
    println("Reduced prefs keys = " + reducedPrefs.keys.mkString(", "))
    reducedPrefs.foreach(entry => {
      if (entry._2.isEmpty) throw new IllegalArgumentException("Cannot find a stable matching.")
      val favorite = entry._2.head
      println("The favorite for " + entry._1 + " is " + favorite +" among " + reducedPrefs.keys.mkString(", "))
      if (reducedPrefs.contains(favorite)) {
        val orig = reducedPrefs(favorite).head
        require (orig == entry._1, "Expected that " + entry._1 + " and the first element of " + favorite + "'s prefs: "
          + reducedPrefs(favorite).mkString(", ") + "  would be the same.")
        pairings.put(entry._1, favorite)
        println("about to remove " + favorite + "'s list = "+ reducedPrefs(favorite).mkString(", ") + " it should contain only "+ entry._1)
        reducedPrefs.remove(favorite)
      } else {
        throw new IllegalArgumentException("Cannot find a stable matching.")
      }
    })
    pairings.toMap
  }

}