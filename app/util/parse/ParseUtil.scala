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



object TikaParseUtil {

  /**
   * Привести HTTP-хидеры ответа к метаданным tika.
   * @param respHeaders Заголовки ответа, если есть.
   * @param urlOpt Исходная ссылка, если есть.
   * @param meta Необязательный исходный аккамулятор метаданных tika.
   * @return Экземпляр Metadata.
   */
  def httpHeaders2meta(respHeaders: Map[String, Seq[String]], urlOpt: Option[String], meta: Metadata = new Metadata): Metadata = {
    respHeaders
      .iterator
      .flatMap { case (k, vs) => vs.iterator.map(v => (k, v)) }
      // TODO Выверять названия хидеров. Они могут приходить в нижнем регистре.
      .foreach { case (k, v) => meta.add(k, v) }
    if (urlOpt.isDefined)
      meta.add(TikaMetadataKeys.RESOURCE_NAME_KEY, urlOpt.get)
    meta
  }

}
