package util.lk

import models.blk.SzMult_t
import models.im.make.{Makers, MakeArgs}
import models.{blk, MAd}
import models.im.DevScreen
import util.blocks.BgImg

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 14:01
 * Description: Утиль для работы с рекламными карточками в личном кабинете.
 */
object LkAdUtil {

  /** Коэфф масштабирования карточек в ЛК-таблицах. Можно 0.5F наверное выставить. */
  def TILE_SZ_MULT: SzMult_t = 1.0F

  /**
   * Генерация параметров рендера рекламной карточки.
   * @param mad Рекламная карточки.
   * @param devScreenOpt Инфа по скрину.
   * @return Фьючерс с контейнером аргументов для рендера блока.
   */
  def tiledAdBrArgs(mad: MAd, devScreenOpt: Option[DevScreen] = None)
                   (implicit ec: ExecutionContext): Future[blk.RenderArgs] = {
    val szMult = TILE_SZ_MULT
    val bgImgOptFut = BgImg.getBgImg(mad) match {
      case Some(bgImgInfo) =>
        val wArgs = MakeArgs(
          img           = bgImgInfo,
          blockMeta     = mad.blockMeta,
          szMult        = szMult,
          devScreenOpt  = devScreenOpt
        )
        Makers.Block.icompile(wArgs)
          .map(Some.apply)

      case None =>
        Future successful None
    }
    bgImgOptFut.map { bgImgOpt =>
      blk.RenderArgs(
        mad       = mad,
        withEdit  = false,
        szMult    = szMult,
        bgImg     = bgImgOpt
      )
    }
  }

}
