package util.showcase

import functional.OneAppPerSuiteNoGlobalStart
import io.suggest.ad.blk._
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.{MPxRatios, MScreen, MSzMult}
import io.suggest.jd.MJdConf
import models.blk.SzMult_t
import org.scalatestplus.play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.14 15:16
 * Description: Тесты для утили siom-выдачи.
 */
class ShowcaseUtilSpec extends PlaySpec with OneAppPerSuiteNoGlobalStart {

  private lazy val scUtil = app.injector.instanceOf[ShowcaseUtil]

  import scUtil._

  private def pxr15 = MPxRatios.HDPI  // Нужно lazy или def, иначе будет exception in initializer error.
  private def pxr30 = MPxRatios.DPR3

  private def jdConf(szMult: Float, gridColumnsCount: Int): MJdConf = {
    MJdConf(
      szMult = MSzMult.fromDouble( szMult ),
      isEdit = false,
      gridColumnsCount = gridColumnsCount,
    )
  }

  // Тестируем калькулятор szMult для блоков плитки выдачи.
  "getSzMult4tiles() for tile" must {
    def t(dscr: MScreen, res: MJdConf): Unit = {
      getTileArgs(dscr) mustBe res
    }

    "not scale on screen: 320x480" in {
      val scr = MScreen(MSize2di(width = 320, height = 480), pxr15)
      t(scr,  jdConf(0.969697F, 1))    // TODO Раньше было 1.0. Может это было правильнее?
    }

    "not scale on screen: 360x640" in {
      val scr = MScreen(MSize2di(width = 360, height = 640), pxr15)
      t(scr, jdConf(1.06F, 1))
    }

    "scale by x1.1 on screen: 768x1024" in {
      val scr = MScreen(MSize2di(width = 768, height = 640), pxr15)
      t(scr, jdConf(1.1F, 2))
    }

    "scale by x1.2 on screen: 800x1280" in {
      val scr = MScreen(MSize2di(width = 800, height = 1280), pxr15)
      t(scr, jdConf(1.2F, 2))
    }

    "not scale on screen: 980x1280" in {
      val scr = MScreen(MSize2di(width = 980, height = 1280), pxr15)
      t(scr, jdConf(1.0F, 3))
    }

    "scale by x1.3 on screen: 1280x600" in {
      val scr = MScreen(MSize2di(width = 1280, height = 600), pxr15)
      t(scr, jdConf(0.969697F, 4))
    }

  }


  // Тестим вычислитель szMult для открытой рекламной карточки.
  "fitBlockToScreen() for focused ad" must {
    def t(bw: BlockWidth, bh: BlockHeight, dscr: MScreen, res: SzMult_t): Unit = {
      val wh = MSize2di(
        height = bh.value,
        width  = bw.value
      )
      // TODO !isWide не соответствует действительности.
      fitBlockToScreen(wh, expandMode = None, dscr) mustBe res
    }

    "resize by x1.0 on 320x480 for 300x300 ad" in {
      val scr = MScreen(MSize2di(width = 320, height = 480), pxr15)
      t(BlockWidths.NORMAL, BlockHeights.H300, scr, 0.969697F)
    }

    "resize by x1.0 on 360x640 for 300x300 ad" in {
      val scr = MScreen(MSize2di(width = 360, height = 640), pxr15)
      t(BlockWidths.NORMAL, BlockHeights.H300, scr, 1.06F)
    }

    "resize by x1.1 on 380x640 for 300x300 ad" in {
      val scr = MScreen(MSize2di(width = 380, height = 640), pxr15)
      t(BlockWidths.NORMAL, BlockHeights.H300, scr, 1.1F)
    }

    "resize by ~x2.06 on wide screen for 300x300 ad" in {
      val scr = MScreen(MSize2di(width = 1600, height = 1200), pxr15)
      t(BlockWidths.NORMAL, BlockHeights.H300, scr, 620F/300F)
    }

    "resize by ~x2.06 on wide 3.0-screen for 300x300 ad" in {
      val scr = MScreen(MSize2di(width = 1600, height = 1200), pxr30)
      t(BlockWidths.NORMAL, BlockHeights.H300, scr, 620F/300F)
    }

    // TODO протестить неквадратные карточки.

  }

}
