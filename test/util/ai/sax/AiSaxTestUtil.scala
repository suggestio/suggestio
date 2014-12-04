package util.ai.sax

import java.net.URL

import functional.WithInputStream
import models.ai.MAiParserCtxT
import org.joda.time.DateTimeZone
import org.scalatestplus.play._
import org.xml.sax.helpers.DefaultHandler
import util.ai.AiContentHandler

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.14 15:12
 * Description: Вспомогательное барахло для написания тестов, проверяющие работу AiSax handler'ов.
 */
abstract class AiSaxPlaySpec extends PlaySpec with WithInputStream {

  /** Точный тип тестируемого ContentHandler'а. */
  type H <: DefaultHandler with AiContentHandler

  /** Тип возвращаемых результатов от тестируемого ContentHandler'а. */
  type T
  
  /** Инфа по парсингу одного экземпляра готового rss. */
  protected case class Info(url: String, fileNameOpt: Option[String], contentType: String) {
    def stream = {
      if (fileNameOpt.isDefined)
        getResourceAsStream(fileNameOpt.get)
      else
        new URL(url).openStream()
    }
  }

  /** Создать новый инстанс используемого ContentHandler'а. */
  def getHandler(ctx: MAiParserCtxT): H

  /** Извлечь результат из тестируемого экземпляра ContentHandler'а. */
  def getResult(handler: H): T

  /** Запустить парсинг по упрощенной схеме. */
  protected def doParse(info: Info): T = {
    // Сборка цепочки парсинга
    val ctx = new MAiParserCtxT {
      override def urlOpt = Some(info.url)
      override def respHeaders = Map(
        "Content-Type" -> Seq("text/html; charset=UTF-8")
      )
      override def openInputStream = info.stream
      override def name = getClass.getSimpleName
      override def tz = DateTimeZone.getDefault
      override def id = None
    }
    val saxHandler = getHandler(ctx)
    saxHandler
      .sourceParser
      .parseFromStream(saxHandler, ctx)
    getResult(saxHandler)
  }
  
}
