package org.conbere.cocktail

import org.rogach.scallop._;
import java.io.File

import scala.language.reflectiveCalls

class InvalidCorpusException(msg:String) extends Exception(msg)

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
        val tickInterval = opt[String]("tick-interval")
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
          Conf.create.tickInterval(),
          Conf.create.corpus(),
          _rooms
        ))

      case _ =>
        Conf.printHelp
    }
  }
}

