package controllers.sc

import controllers.SioController
import models.mctx.Context
import models.req.IReq
import util.cdn.ICdnUtilDi
import util.di.{ILogoUtilDi, IScStatUtil}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description: Всякая базисная утиль для сборки Sc-контроллера.
 */
trait ScController
  extends SioController
  with ICdnUtilDi
  with ILogoUtilDi
  with IScStatUtil
{

  import mCommonDi.ec

  /** Быстренькое добавление поля lazy val ctx в код sc-логики. */
  protected trait LazyContext {

    implicit def _request: IReq[_]

    implicit lazy val ctx: Context = getContext2

  }


  /** Всякая расшаренная утиль для сборки sc-логик. */
  protected trait LogicCommonT extends LazyContext { logic =>

    /** Частичная реализация Stat2 под нужды sc-логик. */
    abstract class Stat2 extends scStatUtil.Stat2 {
      override def ctx: Context = logic.ctx
    }

    /** Контекстно-зависимая сборка логик. */
    def scStat: Future[Stat2]

    /** Сохранение подготовленной статистики обычно везде очень одинаковое. */
    def saveScStat(): Future[_] = {
      scStat
        .flatMap(scStatUtil.saveStat)
    }

  }

}