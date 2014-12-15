package util.blocks

import org.scalatestplus.play._
import play.api.GlobalSettings
import play.api.test.FakeApplication

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.10.14 14:32
 * Description: Тесты для утили, относящейся к поддержке фоновых картинок блоков.
 */
class BgImgSpec extends PlaySpec with OneAppPerSuite {

  "centerNearestLineSeg1D()" must {
    import BgImg.centerNearestLineSeg1D

    "handle small left-centered segments" in {
      centerNearestLineSeg1D(centerCoord = 0,  segLen = 10, axLen = 100)   mustBe  0
      centerNearestLineSeg1D(centerCoord = 10, segLen = 10, axLen = 100)   mustBe  5
      centerNearestLineSeg1D(centerCoord = 15, segLen = 10, axLen = 100)   mustBe  10
      centerNearestLineSeg1D(centerCoord = 33, segLen = 10, axLen = 100)   mustBe  28
    }

    "handle long left-centered segments" in {
      centerNearestLineSeg1D(centerCoord = 25, segLen = 70, axLen = 100)   mustBe  0
      centerNearestLineSeg1D(centerCoord = 49, segLen = 99, axLen = 100)   mustBe  0
      centerNearestLineSeg1D(centerCoord = 49, segLen = 100, axLen = 100)  mustBe  0
    }


    "handle small right-centered segments" in {
      centerNearestLineSeg1D(centerCoord = 90, segLen = 10, axLen = 100)   mustBe 85
      centerNearestLineSeg1D(centerCoord = 55, segLen = 10, axLen = 100)   mustBe 50
      centerNearestLineSeg1D(centerCoord = 70, segLen = 10, axLen = 100)   mustBe 65
    }

    "handle long right-centered segments" in {
      centerNearestLineSeg1D(centerCoord = 85, segLen = 70, axLen = 100)   mustBe 30
      centerNearestLineSeg1D(centerCoord = 95, segLen = 70, axLen = 100)   mustBe 30
      centerNearestLineSeg1D(centerCoord = 51, segLen = 100, axLen = 100)  mustBe 0
    }


    "handle centered segments" in {
      centerNearestLineSeg1D(centerCoord = 50, segLen = 30, axLen = 100)   mustBe 35
      centerNearestLineSeg1D(centerCoord = 50, segLen = 50, axLen = 100)   mustBe 25
      centerNearestLineSeg1D(centerCoord = 50, segLen = 100, axLen = 100)  mustBe 0
    }
  }


  /** Штатный Global производит долгую инициализацию, которая нам не нужна. Ускоряем запуск: */
  override implicit lazy val app = FakeApplication(
    withGlobal = Some(new GlobalSettings() {
      override def onStart(app: play.api.Application) {
        super.onStart(app)
        println("Started dummy fake application, without Global.onStart() initialization.")
      }
    })
  )

}
