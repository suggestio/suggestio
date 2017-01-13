package controllers.sysctl.bill

import controllers.routes
import io.suggest.mbill2.m.balance.IMBalances
import models.msys.bill.{MPaymentFormResult, MPaymentTplArgs}
import models.req.INodeContractReq
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.mvc.Result
import util.FormUtil.{currencyOrDfltM, doubleM, toStrOptM}
import util.PlayMacroLogsI
import util.acl.IsSuNodeContract
import util.billing.IBill2UtilDi
import views.html.sys1.bill.contract.balance._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 14:48
  * Description: Аддон для [[controllers.SysBilling]]-контроллера, добавляющий поддержку взаимодействия с балансами.
  */
trait SbPayment
  extends IsSuNodeContract
  with PlayMacroLogsI
  with IMBalances
  with IBill2UtilDi
{

  import mCommonDi._

  /** Маппинг форма приёма произвольного платежа на баланс. */
  private def _paymentFormM: Form[MPaymentFormResult] = {
    import MPaymentFormResult._
    Form(
      mapping(
        AMOUNT_FN         -> doubleM,
        CURRENCY_CODE_FN  -> currencyOrDfltM,
        COMMENT_FN        -> toStrOptM( text(maxLength = 256) )
      )
      { MPaymentFormResult.apply }
      { MPaymentFormResult.unapply }
    )
  }

  /** Валюты, доступные юзеру в форме. */
  private def _currencies = List("RUB" -> "RUB")

  /**
    * Страницы с формой пополнения баланса.
    *
    * @param nodeId id узла, чей баланс пополняется.
    * @return 200 Ок со страницей-результатом.
    */
  def payment(nodeId: String) = IsSuNodeContractGet(nodeId).async { implicit request =>
    _payment(_paymentFormM, Ok)
  }

  private def _payment(bf: Form[MPaymentFormResult], rs: Status)(implicit request: INodeContractReq[_]): Future[Result] = {
    val args = MPaymentTplArgs(
      bf            = _paymentFormM,
      currencyOpts  = _currencies,
      mnode         = request.mnode,
      mcontract     = request.mcontract
    )
    rs(paymentTpl(args))
  }

  /**
    * Сабмит формы выполнения внутреннего платежа.
    *
    * @param nodeId id узла, на кошельках которого барабаним.
    * @return Редирект в биллинг узла, если всё ок.
    */
  def paymentSubmit(nodeId: String) = IsSuNodeContractPost(nodeId).async { implicit request =>
    _paymentFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"paymentSubmit($nodeId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        _payment(formWithErrors, NotAcceptable)
      },
      {res =>
        val mcId = request.mcontract.id.get
        val price = res.price
        val txnFut = slick.db.run {
          bill2Util.increaseBalanceSimple(mcId, price)
        }
        for (txn <- txnFut) yield {
          Redirect( routes.SysBilling.forNode(nodeId) )
            .flashing(FLASH.SUCCESS -> s"Баланс узла изменён на $price")
        }
      }
    )
  }

}
