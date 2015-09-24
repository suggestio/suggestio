package io.suggest.model.n2.node.meta

import org.scalatest._, Matchers._
import play.api.libs.json.Json

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 10:15
 * Description: Тесты для модели [[MNodeMeta]].
 */
class MNodeMetaSpec extends FlatSpec {

  private def t(mnm: MNodeMeta): Unit = {
    val jsv = Json.toJson(mnm)
    jsv.as[MNodeMeta] shouldBe mnm
  }

  "JSON" should "work on empty MNodeMeta" in {
    t(MNodeMeta.empty)
  }

  it should "work on full-filled MNodeMeta" in {
    t(MNodeMeta(
      nameOpt       = Some("Сервис япредлагаю.com"),
      nameShortOpt  = Some("ЯПредлагаю"),
      description   = Some("Сервис позволяет грабить корованы.\n\n\r\t 12 <123213123>"),
      town          = Some("Санкт-Петроград"),
      address       = Some("пр.Ленина, д.1, корп.2, кв.33"),
      phone         = Some("+7 999 666 555 44"),
      floor         = Some("Этаж четвертый"),
      siteUrl       = Some("http://suggest.io/"),
      audienceDescr = Some("Очень много людей сюда приходит, вообще очень ок всё."),
      humanTrafficAvg = Some(10000),
      info          = Some("Поле info содержит какую-то информацию."),
      color         = Some("000001"),
      fgColor       = Some("FFFFFE"),
      welcomeAdId   = Some("fajn8f9a4wfjmafoimaewrfwa")
    ))
  }

}
