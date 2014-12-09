package util.showcase

import models.blk.SzMult_t
import models.im.{DevScreenT, DevPixelRatios, DevScreen}
import org.scalatestplus.play._
import play.api.GlobalSettings
import play.api.test.FakeApplication

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.14 15:16
 * Description: Тесты для утили siom-выдачи.
 */
class ShowcaseUtilSpec extends PlaySpec with OneAppPerSuite {
  import ShowcaseUtil._

  "ShowcaseUtil.getSzMult4tiles() for tiling purposes" must {
    lazy val pxRatioOpt = Some(DevPixelRatios.HDPI)  // Нужно lazy или def, иначе будет exception in initializer error.
    def t(scr: DevScreenT, res: SzMult_t): Unit = {
      getSzMult4tilesScr(
        szMults = ShowcaseUtil.TILES_SZ_MULTS,
        dscr = scr
      ) mustBe res
    }

    "not scale on screen: 320x480" in {
      val scr = DevScreen(width = 320, height = 480, pxRatioOpt)
      t(scr,  1.0F)
    }

    "not scale on screen: 360x640" in {
      val scr = DevScreen(width = 360, height = 640, pxRatioOpt)
      t(scr, 1.0F)
    }

    "scale by x1.1 on screen: 768x1024" in {
      val scr = DevScreen(width = 768, height = 640, pxRatioOpt)
      t(scr, 1.1F)
    }

    "scale by x1.2 on screen: 800x1280" in {
      val scr = DevScreen(width = 800, height = 1280, pxRatioOpt)
      t(scr, 1.2F)
    }

    "not scale on screen: 980x1280" in {
      val scr = DevScreen(width = 980, height = 1280, pxRatioOpt)
      t(scr, 1.0F)
    }

    "scale by x1.3 on screen: 1280x600" in {
      val scr = DevScreen(width = 1280, height = 600, pxRatioOpt)
      t(scr, 1.3F)
    }

  }


  /** Штатный Global производит долгую инициализацию, которая нам не нужен.
    * Нужен только доступ к конфигу. Ускоряем запуск: */
  override implicit lazy val app = FakeApplication(
    withGlobal = Some(new GlobalSettings() {
      override def onStart(app: play.api.Application) {
        super.onStart(app)
        println("Started dummy fake application, without Global.onStart() initialization.")
      }
    })
  )

}
