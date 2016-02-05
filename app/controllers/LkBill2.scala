package controllers

import com.google.inject.Inject
import controllers.cbill.LkBill2Cart
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.billing.Bill2Util
import views.html.lk.billing.ThanksForBuyTpl


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 19:17
  * Description: Контроллер биллинга поколения биллинга в личном кабинете.
  */
class LkBill2 @Inject() (
  override val bill2Util      : Bill2Util,
  override val mCommonDi      : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with LkBill2Cart
{

  /**
    * Страница "спасибо за покупку". Финиш.
    *
    * @param onNodeId В рамках ЛК какой ноды происходит движуха.
    * @return Страница "спасибо за покупку".
    */
  def thanksForBuy(onNodeId: String) = IsAdnNodeAdmin(onNodeId) { implicit request =>
    Ok(ThanksForBuyTpl(request.mnode))
  }

}
