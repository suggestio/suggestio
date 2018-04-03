package controllers

import javax.inject.Inject
import models.mproj.ICommonDi
import util.acl.IsSu
import util.adv.direct.AdvRcvrsUtil
import views.html.sys1.debug._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 16:16
 * Description: Sys-контроллер для отладки.
 */
class SysDebug @Inject() (
                           advRcvrsUtil                  : AdvRcvrsUtil,
                           isSu                          : IsSu,
                           override val mCommonDi        : ICommonDi
)
  extends SioControllerImpl
{

  import mCommonDi._

  /** Экшен для отображения индексной страницы. */
  def index = csrf.AddToken {
    isSu() { implicit request =>
      Ok( indexTpl() )
    }
  }

  /** Запуск поиска и ремонта неправильных ресиверов в карточках. */
  def resetAllRcvrs = csrf.Check {
    isSu().async { implicit request =>
      for (count <- advRcvrsUtil.resetAllReceivers()) yield {
        Ok(count + " ads updated.")
      }
    }
  }

}
