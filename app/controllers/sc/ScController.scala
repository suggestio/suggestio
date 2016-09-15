package controllers.sc

import controllers.SioController
import models.mctx.Context
import models.req.IReq
import util.cdn.ICdnUtilDi
import util.di.ILogoUtilDi

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
{

  /** Быстренькое добавление поля lazy val ctx в код sc-логики. */
  protected trait LazyContext {

    implicit def _request: IReq[_]

    implicit lazy val ctx: Context = getContext2

  }

}