package org.conbere.cocktail

import org.conbere.irc._
import org.conbere.markov._

import akka.actor._
import scala.io.{ Source, Codec }
import scala.collection.JavaConversions._

import com.typesafe.config._
import org.rogach.scallop._;

import java.io.File

import scala.language.reflectiveCalls


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

object Cocktail {
  def run(file:String) = {
    val reference = ConfigFactory.parseFile(new File("src/main/resources/reference.conf"))
    val conf = ConfigFactory.parseFile(new File(file))

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

    val nickName = conf.getString("bot.nickname") 
    val userName = conf.getString("bot.username") 
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

  def create(nickName:String,
             userName:String,
             password:String,
             realName:String,
             alertRegex:Option[String],
             corpus:String,
             rooms:List[String]) {
    CocktailTemplate.writeToStdOut(Map[String,Any](
      "nickname" -> nickName,
      "username" -> userName,
      "password" -> password,
      "realname" -> realName,
      "alertregex" -> alertRegex,
      "corpus" -> corpus,
      "rooms" -> rooms,
      "server" -> "irc.ny4dev.etsy.com",
      "port" -> "6667"
    ))
  }
}

object Main {
  def main(args:Array[String]) = {
    object Conf extends ScallopConf(args) {
      val run = new Subcommand("run") {
        val conf = trailArg[String]()
      }

      val create = new Subcommand("create") {
        val nickname = opt[String]("nickname", required=true)
        val username = opt[String]("username", required=true)
        val password = opt[String]("password", required=true)
        val realname = opt[String]("realname", required=true)
        val corpus = opt[String]("corpus", required=true)
        val alert = opt[String]("alert", required=false)
        val rooms = trailArg[List[String]](required=false)
      }
    }

    Conf.subcommand match {
      case Some(Conf.run) =>
        Cocktail.run(Conf.run.conf())
      case Some(Conf.create) =>
        val alert = if (Conf.create.alert.isDefined) {
          Some(Conf.create.alert())
        } else {
          None
        }

        val rooms = if (Conf.create.rooms.isDefined) {
          Conf.create.rooms()
        } else {
          List()
        }

        Cocktail.create(Conf.create.nickname(),
                        Conf.create.username(),
                        Conf.create.password(),
                        Conf.create.realname(),
                        alert,
                        Conf.create.corpus(),
                        rooms)
      case _ =>
        Conf.printHelp
    }
  }
}
