package util.lk

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.di.IExecutionContext
import models.blk.SzMult_t
import models.im.DevScreen
import models.im.make.{MakeArgs, Makers}
import models.{MNode, blk}
import util.blocks.{BlocksConf, BgImg}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 14:01
 * Description: Утиль для работы с рекламными карточками в личном кабинете.
 */
class LkAdUtil @Inject() (
  override implicit val ec: ExecutionContext
)
  extends IExecutionContext
{

  /** Коэфф масштабирования карточек в ЛК-таблицах. Можно 0.5F наверное выставить. */
  def TILE_SZ_MULT: SzMult_t = 1.0F

  /**
   * Генерация параметров рендера рекламной карточки.
   * @param mad Рекламная карточки.
   * @param devScreenOpt Инфа по скрину.
   * @return Фьючерс с контейнером аргументов для рендера блока.
   */
  def tiledAdBrArgs(mad: MNode, devScreenOpt: Option[DevScreen] = None): Future[blk.RenderArgs] = {
    val szMult = TILE_SZ_MULT

    val bgImgFutOpt = for {
      bm    <- mad.ad.blockMeta
      bgImg <- BgImg.getBgImg(mad)
    } yield {
      val wArgs = MakeArgs(
        img           = bgImg,
        blockMeta     = bm,
        szMult        = szMult,
        devScreenOpt  = devScreenOpt
      )
      Makers.Block.icompile(wArgs)
        .map(Some.apply)
    }

    val bgImgOptFut = FutureUtil.optFut2futOpt(bgImgFutOpt)(identity)

    val bc = BlocksConf.applyOrDefault( mad )

    for (bgImgOpt <- bgImgOptFut) yield {
      blk.RenderArgs(
        mad       = mad,
        bc        = bc,
        withEdit  = false,
        szMult    = szMult,
        bgImg     = bgImgOpt,
        isFocused = false
      )
    }
  }

}
