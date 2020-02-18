package io.suggest.plist

import javax.inject.Inject
import play.api.libs.json._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.02.2020 12:40
  * Description: Поддержка рендера Apple .plist манифестов на основе
  */
class ApplePlistUtil @Inject() () {

  /** Префиксы string-значений, для переопределения plist-типа. */
  object TypePrefixes {
    /** Префикс для date-типа. */
    val DATE = "}dAtE.PlIsT:"
    /** Префикс для Base64-типа. */
    val BASE64 = "}bAsE64.PlIsT:"
  }

  def plistPreamble: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
      |""".stripMargin

  def toPlistDocument( jsValue: JsValue ): String = {
    val plistTagName = "plist"
    Renderer()
      .openTag( plistTagName )
      .render( jsValue )
      .closeTag( plistTagName )
      .toString
  }


  case class Renderer(
                       sb: StringBuilder = new StringBuilder(4096, plistPreamble),
                     ) {

    def openTag(tagName: String): Renderer = {
      sb.append(s"<$tagName>")
      this
    }

    def closeTag(tagName: String): Renderer =
      openTag( "/" + tagName )

    def render(jsValue: JsValue): Renderer = {
      jsValue match {

        case JsString(str) =>
          val (tagName, str2) = if (str startsWith TypePrefixes.DATE) {
            ("date", str.substring( TypePrefixes.DATE.length ))
          } else if (str startsWith TypePrefixes.BASE64) {
            ("data", str.substring( TypePrefixes.BASE64.length ))
          } else {
            ("string", str)
          }

          openTag( tagName )
          sb.append( str2 )
          closeTag( tagName )

        case JsBoolean(value) =>
          sb.append(s"<$value/>")

        case JsNumber(biDi) =>
          val (tagName, renderTagContentF) = {
            Try(biDi.toIntExact).fold(
              {_ =>
                "real" -> { () =>
                  sb.append( biDi.toDouble )
                }
              },
              {int =>
                "integer" -> { () =>
                  sb.append( int )
                }
              }
            )
          }
          openTag( tagName )
          renderTagContentF()
          closeTag( tagName )

        case JsArray(elems) =>
          val tagName = "array"
          openTag(tagName)
          for (e <- elems)
            render(e)
          closeTag(tagName)

        case JsObject( dict ) =>
          val tagName = "dict"
          val keyTagName = "key"
          openTag( tagName )
          for ( (k,v) <- dict ) {
            openTag( keyTagName )
            sb.append( k )
            closeTag( keyTagName )
            render(v)
          }
          closeTag( tagName )

        case JsNull =>
          throw new IllegalArgumentException(s"$JsNull values inacceptable by Plist format")

      }

      this
    }

  }

}
