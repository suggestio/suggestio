package util.lk

import io.suggest.di.IExecutionContext
import io.suggest.model.n2.node.MNode
import javax.inject.Inject
import models.blk
import models.blk.SzMult_t
import models.im.DevScreen
import models.im.make.MakeResult
import play.api.inject.Injector
import util.adv.AdvUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 14:01
 * Description: Утиль для работы с рекламными карточками в личном кабинете.
 */
class LkAdUtil @Inject() (
                           injector                     : Injector,
                           advUtil                      : AdvUtil,
                           override implicit val ec     : ExecutionContext
                         )
  extends IExecutionContext
{

  /** Коэфф масштабирования карточек в ЛК-таблицах. Можно 0.5F наверное выставить. */
  def TILE_SZ_MULT: SzMult_t = 1.0F

  /**
   * Генерация параметров рендера рекламной карточки.
    *
    * @param mad Рекламная карточки.
   * @param devScreenOpt Инфа по скрину.
   * @return Фьючерс с контейнером аргументов для рендера блока.
   */
  def tiledAdBrArgs(mad: MNode, devScreenOpt: Option[DevScreen] = None): Future[blk.RenderArgs] = {
    val szMult = TILE_SZ_MULT

    // TODO mads2 Тут выпилен код вообще.
    val bgImgOptFut = Future.successful( Option.empty[MakeResult] )

    for (bgImgOpt <- bgImgOptFut) yield {
      blk.RenderArgs(
        mad       = mad,
        withEdit  = false,
        szMult    = szMult,
        bgImg     = bgImgOpt,
        isFocused = false
      )
    }
  }

}


trait ILkAdUtilDi {
  def lkAdUtil: LkAdUtil
}
