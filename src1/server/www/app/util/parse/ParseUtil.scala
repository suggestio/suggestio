package util.parse

import javax.xml.parsers.SAXParserFactory

import org.apache.tika.metadata.{Metadata, TikaMetadataKeys}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.14 11:23
 * Description: Всякая статическая утиль для парсинга.
 */
object SaxParseUtil {

  /** Собрать и настроить sax parser factory для парсеров, используемых в работе по разбору кривых xml-файлов. */
  def getSaxFactoryTolerant: SAXParserFactory = {
    val saxfac = SAXParserFactory.newInstance()
    saxfac.setValidating(false)
    saxfac.setFeature("http://xml.org/sax/features/validation", false)
    saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false)
    saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    saxfac
  }

}
