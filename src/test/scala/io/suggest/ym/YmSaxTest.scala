package io.suggest.ym

import org.scalatest._
import javax.xml.parsers.SAXParserFactory

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
      val factory = SAXParserFactory.newInstance()
      factory.setValidating(false)
      val parser = factory.newSAXParser()
      val cHandler = new YmlSax(null)
      parser.parse(is, cHandler)

    } finally {
      is.close()
    }
  }

}
