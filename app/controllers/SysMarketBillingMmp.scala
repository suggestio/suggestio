package controllers

import util.PlayMacroLogsImpl
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import play.api.db.DB
import play.api.Play.current
import views.html.sys1.market.billing.mmp.daily._
import play.api.data._, Forms._
import util.FormUtil._
import util.acl._
import play.api.mvc.AnyContent
import play.api.templates.HtmlFormat
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 12:07
 * Description: sys-контроллер для работы с mmp-тарификацией, т.е. когда тарификация настраивается по рекламным модулям.
 */
object SysMarketBillingMmp extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Биндинг формы для daly-тарификатора. */
  val mmpDailyFormM = Form(mapping(
    "currencyCode"  -> default(text(minLength = 3, maxLength = 3), "RUB"),
    "mmpWeekday"    -> floatM,
    "mmpWeekend"    -> floatM,
    "mmpPrimetime"  -> floatM
  )
  {(currencyCode, mmpWeekday, mmpWeekend, mmpPrimetime) =>
    MBillMmpDaily(
      contractId = -1,
      currencyCode = currencyCode,
      mmpWeekday = mmpWeekday,
      mmpWeekend = mmpWeekend,
      mmpPrimetime = mmpPrimetime
    )
  }
  {mbmd =>
    import mbmd._
    Some((currencyCode, mmpWeekday, mmpWeekend, mmpPrimetime))
  })


  def createMmpDaily(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    _createMmpDaily(mmpDailyFormM)
      .map(Ok(_))
  }

  private def _createMmpDaily(form: Form[MBillMmpDaily])(implicit request: ContractRequest[AnyContent]): Future[HtmlFormat.Appendable] = {
    MAdnNodeCache.getByIdCached(request.contract.adnId) map { adnNodeOpt =>
      createMppDailyTpl(request.contract, mmpDailyFormM, adnNodeOpt)
    }
  }

  def createMmpDailySubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    mmpDailyFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"createMmpDailySubmit($contractId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        _createMmpDaily(formWithErrors)
          .map(NotAcceptable(_))
      },
      {mbmd0 =>
        val mbmd = mbmd0.copy(contractId = contractId)
        val mbmd1 = DB.withConnection { implicit c =>
          mbmd.save
        }
        Redirect(routes.SysMarketBilling.billingFor(request.contract.adnId))
          .flashing("success" -> s"Создан посуточный тарификатор #${mbmd1.id.get} для договора ${request.contract.legalContractId}")
      }
    )
  }


  def editMmpDaily(mmpdId: Int) = IsSuperuser.async { implicit request =>
    _editMmpDaily(mmpdId, mmpDailyFormM)
      .map(Ok(_))
  }

  private def _editMmpDaily(mmpdId: Int, form: Form[MBillMmpDaily])(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[HtmlFormat.Appendable] = {
    val syncResult = DB.withConnection { implicit c =>
      val mbmd = MBillMmpDaily.getById(mmpdId).get
      val mbc  = MBillContract.getById(mbmd.contractId).get
      (mbmd, mbc)
    }
    val (mbmd, mbc) = syncResult
    val nodeOptFut = MAdnNodeCache.getByIdCached(mbc.adnId)
    val formBinded = mmpDailyFormM.fill(mbmd)
    nodeOptFut.map { nodeOpt =>
      editMppDailyTpl(mbmd, mbc, formBinded, nodeOpt)
    }
  }

  def editMmpDailySubmit(mmpdId: Int) = IsSuperuser.async { implicit request =>
    mmpDailyFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"editMmpDailySubmit($mmpdId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        _editMmpDaily(mmpdId, formWithErrors)
          .map(NotAcceptable(_))
      },
      {newMbmd =>
        val rdrToAdn = DB.withTransaction { implicit c =>
          val mbmd = MBillMmpDaily.getById(mmpdId).get
          val mbmd1 = newMbmd.copy(id = mbmd.id, contractId = mbmd.contractId)
          mbmd1.save
          // Вычисляем, куда нужно редиректить юзера после сохранения.
          val contract = MBillContract.getById(mbmd.contractId).get
          contract.adnId
        }
        Redirect(routes.SysMarketBilling.billingFor(rdrToAdn))
          .flashing("success" -> s"Изменения в тарифном плане #$mmpdId сохранены.")
      }
    )
  }

}
