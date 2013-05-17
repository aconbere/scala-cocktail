package org.conbere.cocktail

import com.github.mustachejava._
import scala.collection.JavaConversions._

import java.io.{ OutputStreamWriter, FileOutputStream, OutputStream, StringReader}
import java.io.File

class CocktailTemplateScope(
  val server:String,
  val port:Int,
  val nickname:String,
  val username:String,
  val password:String,
  val realname:String,
  val alertRegex:String,
  val tickInterval:String,
  val corpus:File,
  val _rooms:List[String]
) {
  val x = _rooms.toBuffer
  val rooms:java.util.List[String] = x
}

object CocktailTemplate {
  val mf = new DefaultMustacheFactory()

  def writeToFile(path:String, scope:CocktailTemplateScope) =
    execute(new FileOutputStream(path), scope)

  def writeToStdOut(scope:CocktailTemplateScope) = {
    execute(System.out, scope)
  }

  def execute(stream:OutputStream, scope:CocktailTemplateScope) = {
    val writer = new OutputStreamWriter(stream)
    template.execute(writer, scope).flush()
  }

  val template = mf.compile("conf.mustache")
}
