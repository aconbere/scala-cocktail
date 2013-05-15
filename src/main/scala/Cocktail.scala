package org.conbere.cocktail

import org.conbere.irc._
import org.conbere.markov._

import akka.actor._
import scala.io.{ Source, Codec }
import scala.collection.JavaConversions._

import com.typesafe.config._

import java.io.File

class Cocktail( val serverName:String
              , val nickName:String
              , val userName:String
              , val password:String
              , val realName:String
              , val alertRegex:Option[String]
              , val rooms:List[Room]
              , val markov:MarkovChain[String]
              )
extends ClassicBot {
  import Tokens._
  import Messages._

  val toMePattern = "^(" + nickName + "[:| ].*)"

  val alertPattern = alertRegex match {
    case Some(alert) =>
      (toMePattern + "|(" + alertRegex + ")").r
    case None =>
      toMePattern.r
  }

  def respond(from:String) =
    Some(PrivMsg(from, markov.generate(30).mkString(" ")))

  val respondTo = defaultResponse.orElse[Message,Option[Response]] {
    case PrivMsg(from, `nickName`, _) =>
      respond(from)
    case PrivMsg(from, channel, alertPattern(_match)) =>
      respond(channel)
  }
}

object Main {
  val usage = """usage: java -jar cocktail.jar <conf>"""

  def main(args:Array[String]) = {
    if (args.length < 1) {
      println(usage)
      System.exit(1)
    }

    val reference = ConfigFactory.parseFile(new File("src/main/resources/reference.conf"))
    val conf = ConfigFactory.parseFile(new File(args(0)))

    val server = conf.getString("irc.server")
    val port = conf.getInt("irc.port")
    val rooms = conf.getStringList("irc.rooms").toList.flatMap {
      r:String =>
        r.split(":").toList match {
          case List(room, p) =>
            Some(Room(room, Some(p)))
          case List(room) =>
            Some(Room(room, None))
          case _ =>
            None
        }
    }

    val corpusFile = conf.getString("markov.corpus")

    val nickName = conf.getString("bot.nick") 
    val userName = conf.getString("bot.user") 
    val password = conf.getString("bot.password") 
    val realName = conf.getString("bot.realname") 
    val alertRegex = conf.getString("bot.alert-regex") match {
      case "" => None
      case s:String => Some(s)
    }

    val whitespace = """\s+""".r

    val markov = Source.fromFile(corpusFile)(Codec("UTF-8"))
                       .getLines
                       .foldLeft(new MarkovChain[String]("START", "STOP"))((acc, s) =>
                         acc.insert(whitespace.split(s).toList)
                       )

    val bot = new Cocktail( server
                          , nickName
                          , userName
                          , password
                          , realName
                          , alertRegex
                          , rooms
                          , markov)

    val actor = Client.start(server, port, bot)
  }
}
