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

class InvalidCorpusException(msg:String) extends Exception(msg)

class CocktailTemplateScope(
  val server:String,
  val port:Int,
  val nickname:String,
  val username:String,
  val password:String,
  val realname:String,
  val alertRegex:String,
  val tickInitialDelay:String,
  val tickDuration:String,
  val corpus:File,
  val _rooms:List[String]
) {
  val x = _rooms.toBuffer
  val rooms:java.util.List[String] = x
}


class Cocktail( val serverName:String
              , val nickName:String
              , val userName:String
              , val password:String
              , val realName:String
              , val alertRegex:Option[String]
              , val tickInitialDelay:Option[Int]
              , val tickDuration:Option[Int]
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

  override val tickConfig = Some(new TickConfig(tickInitialDelay milliseconds,
                                                tickDuration milliseconds))

  override def tick() = markov.generate(30)
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
    val tickInitialDelay = conf.getInt("bot.tick-initial-delay") 
    val tickDuration = conf.getInt("bot.tick-duration") 

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
                          , tickInitialDelay
                          , tickDuration
                          , rooms
                          , markov)

    val actor = Client.start(server, port, bot)
  }
}

object Main {
  def main(args:Array[String]) = {
    object Conf extends ScallopConf(args) {
      version("cocktail 0.0.1") 
      banner("cocktail <command> [Option]")

      val fileConverter = singleArgConverter[File]{ a:String =>
        val f = new File(a)
        if (!f.exists() || !f.isFile()) {
          throw new InvalidCorpusException("file not found")
        }
        f
      }

      val run = new Subcommand("run") {
        val conf = trailArg[String]()
      }

      val create = new Subcommand("create") {
        val server = opt[String]("server", required=true)
        val port = opt[Int]("port", required=true)
        val nickname = opt[String]("nickname", required=true)
        val username = opt[String]("username", required=true)
        val password = opt[String]("password", required=true)
        val realname = opt[String]("realname", required=true)
        val corpus = opt[File]("corpus", required=true)(fileConverter)
        val alert = opt[String]("alert", required=false)
        val tickInitialDelay = opt[String]("tick-initial-delay")
        val tickDuration = opt[String]("tick-duration")
        val rooms = trailArg[List[String]](required=false)
      }
    }

    Conf.subcommand match {
      case Some(Conf.run) =>
        Cocktail.run(Conf.run.conf())
      case Some(Conf.create) =>
        val _alert = if (Conf.create.alert.isDefined) {
          Conf.create.alert()
        } else {
          ""
        }

        val _rooms = if (Conf.create.rooms.isDefined) {
          Conf.create.rooms()
        } else {
          List()
        }

        CocktailTemplate.writeToStdOut(new CocktailTemplateScope(
          Conf.create.server(),
          Conf.create.port(),
          Conf.create.nickname(),
          Conf.create.username(),
          Conf.create.password(),
          Conf.create.realname(),
          _alert,
          Conf.create.tickInitialDelay(),
          Conf.create.tickDuration(),
          Conf.create.corpus(),
          _rooms
        ))

      case _ =>
        Conf.printHelp
    }
  }
}
