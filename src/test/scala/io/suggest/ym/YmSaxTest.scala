package io.suggest.ym

import org.scalatest._
import org.apache.tika.metadata.{HttpHeaders, Metadata}
import org.apache.tika.parser.xml.XMLParser
import org.apache.tika.parser.ParseContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.14 19:13
 * Description: Тесты для SAX-парсера парсера yml xml файлов.
 */
class YmlSaxTest extends FlatSpec with Matchers {

  "yml1.xml" should "success parsing via YmlSax" in {
    val is = getClass.getClassLoader.getResourceAsStream("ym/yml1.xml")
    try {
      val metadata = new Metadata
      metadata.add(HttpHeaders.CONTENT_TYPE, "text/xml")
      val cHandler = new YmlSax(null)
      val parser = new XMLParser
      val parseContext = new ParseContext
      parser.parse(is, cHandler, metadata, parseContext)

    } finally {
      is.close()
    }
  }

}
