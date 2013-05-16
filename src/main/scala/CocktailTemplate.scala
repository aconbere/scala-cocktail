package org.conbere.cocktail

import com.github.mustachejava._

import java.io.{ OutputStreamWriter, FileOutputStream, OutputStream, StringReader}

object CocktailTemplate {
  val mf = new DefaultMustacheFactory()

  def writeToFile(path:String, scopes:Map[String,Any]) =
    execute(new FileOutputStream(path), scopes)

  def writeToStdOut(scopes:Map[String,Any]) =
    execute(System.out, scopes)

  def execute(stream:OutputStream, scopes:Map[String,Any]) = {
    println(scopes)
    val writer = new OutputStreamWriter(stream)
    template.execute(writer, scopes).flush()
  }


  val template = mf.compile(new StringReader("""irc {
  server="{{server}}"
  port="{{port}}"
  rooms=[
  {{#rooms}}
    "{{room}}",
  {{/rooms}}
  ]
}

markov {
  corpus="{{corpus}}"
}

bot {
  nickname="{{nickname}}"
  username="{{username}}"
  password="{{password}}"
  realname="{{realname}}"
  alert-regex="{{alertregex}}"
}
"""), "conf")
}
