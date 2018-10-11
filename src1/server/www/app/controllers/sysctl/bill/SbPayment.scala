package controllers.sysctl.bill

import controllers.{SioController, routes}
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.mbill2.m.balance.IMBalances
import io.suggest.util.logs.IMacroLogs
import models.msys.bill.{MPaymentFormResult, MPaymentTplArgs}
import models.req.INodeContractReq
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.mvc.Result
import util.FormUtil.{currencyOrDfltM, toStrOptM, doubleM}
import util.acl.IIsSuNodeContract
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
  extends SioController
  with IIsSuNodeContract
  with IMacroLogs
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
      { (realAmount, currency, commentOpt) =>
        MPaymentFormResult(
          amount        = MPrice.realAmountToAmount(realAmount, currency),
          currencyCode  = currency,
          comment       = commentOpt
        )
      }
      {mpfr =>
        for {
          (amount, currency, commentOpt) <- MPaymentFormResult.unapply(mpfr)
        } yield {
          val realAmount = MPrice.amountToReal( amount, currency )
          (realAmount, currency, commentOpt)
        }
      }
    )
  }

  /** Валюты, доступные юзеру в форме. */
  private def _currencies = {
    val rub = MCurrencies.RUB
    List(
      rub.value -> rub.toString
    )
  }

  /**
    * Страницы с формой пополнения баланса.
    *
    * @param nodeId id узла, чей баланс пополняется.
    * @return 200 Ок со страницей-результатом.
    */
  def payment(nodeId: String) = csrf.AddToken {
    isSuNodeContract(nodeId).async { implicit request =>
      _payment(_paymentFormM, Ok)
    }
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
  def paymentSubmit(nodeId: String) = csrf.Check {
    def logPrefix = s"paymentSubmit($nodeId):"
    isSuNodeContract(nodeId).async { implicit request =>
      _paymentFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          _payment(formWithErrors, NotAcceptable)
        },
        {res =>
          val mcId = request.mcontract.id.get
          val price = res.price
          val txnFut = slick.db.run {
            bill2Util.increaseBalanceAsIncome(mcId, price)
          }
          for (txn <- txnFut) yield {
            LOGGER.trace(s"$logPrefix txn => $txn")
            Redirect( routes.SysBilling.forNode(nodeId) )
              .flashing(FLASH.SUCCESS -> s"Баланс узла изменён на $price")
          }
        }
      )
    }
  }

}
