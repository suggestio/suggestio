package io.suggest.sax

import org.scalatest._
import dispatch._, Defaults._
import java.io.{InputStream, ByteArrayInputStream}
import org.apache.tika.metadata.{TikaMetadataKeys, Metadata}
import org.apache.tika.parser.ParseContext
import java.util.concurrent.{TimeoutException, TimeUnit, FutureTask, Callable}
import scala.concurrent.Await
import scala.concurrent.duration._
import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig}
import org.xml.sax.helpers.AttributesImpl
import SioJsDetectorSAX.TAG_SCRIPT
import org.apache.tika.parser.html.{IdentityHtmlMapper, HtmlMapper, HtmlParser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.13 14:43
 * Description: Набор тестов для проверки работы детектора установленного js-кода на сайтах.
 */
class SioJsDetectorSAXTest extends FlatSpec with Matchers {

  val fetchTimeoutMs = 5000
  val parseTimeoutMs = 5000
  val awaitDuration = (parseTimeoutMs + 100) milliseconds

  lazy val httpClient = new Http {
    override val client = {
      val builder = new AsyncHttpClientConfig.Builder()
        .setCompressionEnabled(true)
        .setAllowPoolingConnection(true)
        .setRequestTimeoutInMs(parseTimeoutMs)
        .setMaximumConnectionsPerHost(2)
        .setMaximumConnectionsTotal(10)
        .setFollowRedirects(true)
        .setMaximumNumberOfRedirects(2)
        .setIdleConnectionTimeoutInMs(10000)
      new AsyncHttpClient(builder.build())
    }
  }


  def testUrl(_url: String): List[SioJsInfoT] = {
    val req = url(_url)
    val r = httpClient(req OK as.Bytes)
    val fut = r map { bytes =>
      val bais = new ByteArrayInputStream(bytes)
      val md = new Metadata
      md.add(TikaMetadataKeys.RESOURCE_NAME_KEY, _url)
      // TODO закинуть ContentType ответа в md.
      val c = new TikaCallable(md, bais)
      val task = new FutureTask(c)
      val t = new Thread(task)
      t.start()
      try {
        task.get(parseTimeoutMs, TimeUnit.MILLISECONDS)

      } catch {
        case ex:TimeoutException =>
          task.cancel(true)
          throw ex
      }
    }
    Await.result(fut, awaitDuration)
  }


  // Анализ вынесен в отдельный поток для возможности слежения за его выполнением и принудительной остановкой по таймауту.
  class TikaCallable(md:Metadata, input:InputStream) extends Callable[List[SioJsInfoT]] {
    /**
     * Запуск tika для парсинга запроса
     * @return
     */
    def call(): List[SioJsInfoT] = {
      val jsInstalledHandler = new SioJsDetectorSAX
      val parser = new HtmlParser()
      val parseContext = new ParseContext
      // Используем пустой маппер, чтобы протестить работу в самом страшном случае.
      parseContext.set(classOf[HtmlMapper], new IdentityHtmlMapper)
      parser.parse(input, jsInstalledHandler, md, parseContext)
      jsInstalledHandler.getSioJsInfo
    }
  }



  // Тесты как бы изнутри. Без html. Просто имитируется работа парсера.
  "SioJsDetectorSAX" should "demonstrate proper work on simple text with only one link" in {
    val d = new SioJsDetectorSAX
    d.startElement("", TAG_SCRIPT, "", new AttributesImpl)
    val arr = "https://suggest.io/js/v2/ldpr.ru/fbeCwnu6".toCharArray
    d.characters(arr, 0, arr.length)
    d.endElement("", TAG_SCRIPT, "")
    d.getSioJsInfo should equal (List(SioJsV2("ldpr.ru", "fbeCwnu6")))
  }

  it should "demonstate proper work on text parts, that contains one link" in {
    val d = new SioJsDetectorSAX
    d.startElement("", TAG_SCRIPT, "", new AttributesImpl)
    List("asda","sd asd ","asd h","t","tps:","//s","ugges","t.io/js/v2/ldp","r.ru/fbeCwnu6 awe fawefawefa sd") foreach { s =>
      val arr = s.toCharArray
      d.characters(arr, 0, arr.length)
    }
    d.endElement("", TAG_SCRIPT, "")
    d.getSioJsInfo should equal (List(SioJsV2("ldpr.ru", "fbeCwnu6")))
  }



  // Сами тесты сайтов начинаются тут
  "testUrl() detector" should "see v2 in LDPR.RU" in {
    testUrl("http://ldpr.ru/") should equal (List(SioJsV2("ldpr.ru", "fbeCwnu6")))
  }

  it should "see v2 in aversimage.ru" in {
    testUrl("http://aversimage.ru") should equal (List(SioJsV2("aversimage.ru", "H0PMq5L6")))
  }

  it should "see v1 in 7hitov.ru" in {
    testUrl("http://7hitov.ru/") should equal (List(SioJsV1))
  }

  // Тут надо тестировать при необходимости, ибо для тестов обычно используется локалхост, а скрипты меняются часто или исчезают.
  /*it should "v2 in test.sio.cbca.ru" in {
    testUrl("http://test.sio.cbca.ru/") should equal (List(SioJsV2("test.sio.cbca.ru", "tTAOv2R2")))
  }*/

}
