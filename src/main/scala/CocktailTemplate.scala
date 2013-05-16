package org.conbere.cocktail

import com.github.mustachejava._

import java.io.{ OutputStreamWriter, FileOutputStream, OutputStream, StringReader}

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
