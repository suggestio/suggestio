package io.suggest.ym

import org.scalatest._
import javax.xml.parsers.SAXParserFactory
import cascading.tap.hadoop.Hfs
import cascading.scheme.hadoop.SequenceFile
import io.suggest.ym.model.{YmShopDatum, YmOfferDatum}
import cascading.flow.hadoop.HadoopFlowProcess
import io.suggest.util.SiobixFs.fs
import org.apache.hadoop.fs.Path
import scala.collection.JavaConversions._
import cascading.tap.SinkMode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.14 19:13
 * Description: Тесты для SAX-парсера парсера yml xml файлов.
 */
class YmlSaxTest extends FlatSpec with Matchers {

  "yml1.xml" should "success parsing via YmlSax" in {
    val is = getClass.getClassLoader.getResourceAsStream("ym/yml1.xml")
    val tmpPath = "/tmp/YmlSaxTest-" + System.currentTimeMillis() + "/"
    val outTap = new Hfs(new SequenceFile(YmOfferDatum.FIELDS), tmpPath, SinkMode.REPLACE)
    val hfp = new HadoopFlowProcess()
    try {
      val outCollector = outTap.openForWrite(hfp)
      try {
        val factory = YmlSax.getSaxFactory
        val parser = factory.newSAXParser()
        val cHandler = new YmlSax with YmSaxErrorLogger {
          override def priceShopId: Int = -1
          override val logPrefix: String = "test"
          override def handleShopDatum(shopDatum: YmShopDatum) {}
          override def handleOfferDatum(offerDatum: YmOfferDatum, currenShopDatum: YmShopDatum) {
            outCollector add offerDatum.getTuple
          }
        }
        parser.parse(is, cHandler)
      } finally {
        outCollector.close()
      }

      // Все данные прочесаны, пора прочитать и проверить результаты.
      val results = outTap.openForRead(hfp)
      try {
        results.foreach { te =>
          val datum = new YmOfferDatum(te)
          import datum._
          shopOfferIdOpt.get match {
            // simple-товар: Наручные часы casio
            case id @ "12340" =>
              offerType shouldEqual OfferTypes.Simple
              isAvailable shouldEqual true
              assert( url.exists(_ endsWith id) )
              price shouldBe 699.54F +- 0.50F
              currencyId shouldBe "USD"
              categoryIds should contain ("6")
              categoryIds.size shouldEqual 1
              pictures.head should endWith (id + ".jpg")
              isStore shouldBe false
              isPickup shouldBe true
              isDelivery shouldBe false
              localDeliveryCostOpt.get shouldEqual 300F +- 0.10F
              // TODO Дописать проверку остальных полей

            case _ => // TODO
          }
        }
      } finally {
        results.close()
      }

    } finally {
      is.close()
      //fs.delete(new Path(tmpPath), true)
    }
  }

}
