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

  /** Маппинг формы для daly-тарификатора. */
  val mmpDailyFormM = {
    val floatGreaterThan1 = floatM.verifying(_ > 1.0F)
    Form(mapping(
      "currencyCode"  -> currencyCodeOrDfltM,
      "mmpWeekday"    -> floatM,
      "mmpWeekend"    -> floatM,
      "mmpPrimetime"  -> floatM,
      "onStartPage"   -> floatGreaterThan1,
      "onRcvrCat"     -> floatGreaterThan1,
      "weekendCalId"  -> esIdM,
      "primeCalId"    -> esIdM
    )
    {(currencyCode, mmpWeekday, mmpWeekend, mmpPrimetime, onStartPage, onRcvrCat, weekendCalId, primeCalId) =>
      MBillMmpDaily(
        contractId    = -1,
        currencyCode  = currencyCode,
        mmpWeekday    = mmpWeekday,
        mmpWeekend    = mmpWeekend,
        mmpPrimetime  = mmpPrimetime,
        onStartPage   = onStartPage,
        onRcvrCat     = onRcvrCat,
        weekendCalId  = weekendCalId,
        primeCalId    = primeCalId
      )
    }
    {mbmd =>
      import mbmd._
      Some((currencyCode, mmpWeekday, mmpWeekend, mmpPrimetime, onStartPage, onRcvrCat, weekendCalId, primeCalId))
    })
  }


  /** Рендер страницы создания нового посуточного mmp-тарификтора.
    * @param contractId номер договора.
    */
  def createMmpDaily(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    _createMmpDaily(mmpDailyFormM)
      .map(Ok(_))
  }

  private def _createMmpDaily(form: Form[MBillMmpDaily])(implicit request: ContractRequest[AnyContent]): Future[HtmlFormat.Appendable] = {
    val mcalsFut = MCalendar.getAll()
    for {
      adnNodeOpt <- MAdnNodeCache.getById(request.contract.adnId)
      mcals      <- mcalsFut
    } yield {
      createMppDailyTpl(request.contract, mmpDailyFormM, adnNodeOpt, mcals)
    }
  }

  /**
   * Сабмит формы создания нового посуточного mmp-тарифного плана.
   * @param contractId номер договора.
   */
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


  /**
   * Рендер страницы с формой редактирования посуточного mmp-тарифного плана.
   * @param mmpdId id mmp-тарифного плана.
   */
  def editMmpDaily(mmpdId: Int) = IsSuperuser.async { implicit request =>
    _editMmpDaily(mmpdId, mmpDailyFormM)
      .map(Ok(_))
  }

  private def _editMmpDaily(mmpdId: Int, form: Form[MBillMmpDaily])(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[HtmlFormat.Appendable] = {
    val mcalsFut = MCalendar.getAll()
    val syncResult = DB.withConnection { implicit c =>
      val mbmd = MBillMmpDaily.getById(mmpdId).get
      val mbc  = MBillContract.getById(mbmd.contractId).get
      (mbmd, mbc)
    }
    val (mbmd, mbc) = syncResult
    val nodeOptFut = MAdnNodeCache.getById(mbc.adnId)
    val formBinded = mmpDailyFormM.fill(mbmd)
    for {
      nodeOpt <- nodeOptFut
      mcals   <- mcalsFut
    } yield {
      editMppDailyTpl(mbmd, mbc, formBinded, nodeOpt, mcals)
    }
  }

  /**
   * Сабмит формы редактирования mmp-тарифного плана.
   * @param mmpdId id mmp-тарифного плана.
   */
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
