package controllers

import play.twirl.api.HtmlFormat
import util.PlayMacroLogsImpl
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import play.api.db.DB
import play.api.Play.{current, configuration}
import views.html.sys1.market.billing.mmp.daily._
import play.api.data._, Forms._
import util.FormUtil._
import util.acl._
import play.api.mvc.AnyContent
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 12:07
 * Description: sys-контроллер для работы с mmp-тарификацией, т.е. когда тарификация настраивается по рекламным модулям.
 */
object SysMarketBillingMmp extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  // Дефолтовые значения для формы создания нового mmp-тарификатора.
  lazy val MMP_CURRENCY_CODE_DFLT = configuration.getString("sys.mmp.daily.currency.code.dflt")
    .fold(CurrencyCodeOpt.CURRENCY_CODE_DFLT)(_.toUpperCase)
  lazy val MMP_WEEKDAY_DFLT     = configuration.getDouble("sys.mmp.daily.weekday.dflt").fold(1.0F)(_.toFloat)
  lazy val MMP_WEEKEND_DFLT     = configuration.getDouble("sys.mmp.daily.weekend.dflt").fold(1.5F)(_.toFloat)
  lazy val MMP_PRIME_DFLT       = configuration.getDouble("sys.mmp.daily.prime.dflt").fold(2.0F)(_.toFloat)
  lazy val MMP_ON_START_PAGE_DFLT = configuration.getDouble("sys.mmp.daily.on.startPage.dflt").fold(4.0F)(_.toFloat)
  lazy val MMP_ON_RCVR_CAT_DFLT = configuration.getDouble("sys.mmp.daily.on.rcvrCat.dflt").fold(2.0F)(_.toFloat)
  lazy val MMP_CAL_ID_WEEKEND   = configuration.getString("sys.mmp.daily.calId.weekend") getOrElse ""
  lazy val MMP_CAL_ID_PRIME     = configuration.getString("sys.mmp.daily.calId.prime") getOrElse MMP_CAL_ID_WEEKEND


  /** Маппинг формы для daly-тарификатора. */
  val mmpDailyFormM = {
    val floatGreaterThan1 = floatM.verifying(_ >= 1.0F)
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
    val mmpStub = MBillMmpDaily(
      contractId    = -1,
      currencyCode  = MMP_CURRENCY_CODE_DFLT,
      mmpWeekday    = MMP_WEEKDAY_DFLT,
      mmpWeekend    = MMP_WEEKEND_DFLT,
      mmpPrimetime  = MMP_PRIME_DFLT,
      onRcvrCat     = MMP_ON_RCVR_CAT_DFLT,
      onStartPage   = MMP_ON_START_PAGE_DFLT,
      weekendCalId  = MMP_CAL_ID_WEEKEND,
      primeCalId    = MMP_CAL_ID_PRIME
    )
    val formM = mmpDailyFormM fill mmpStub
    _createMmpDaily(formM)
      .map(Ok(_))
  }

  private def _createMmpDaily(formM: Form[MBillMmpDaily])(implicit request: ContractRequest[AnyContent]): Future[HtmlFormat.Appendable] = {
    val mcalsFut = MCalendar.getAll()
    for {
      adnNodeOpt <- MAdnNodeCache.getById(request.contract.adnId)
      mcals      <- mcalsFut
    } yield {
      createMppDailyTpl(request.contract, formM, adnNodeOpt, mcals)
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


  /** Сабмит удаления mmp-тарификатора. */
  def deleteMmpDailySubmit(mmpId: Int) = IsSuperuser { implicit request =>
    // Нужно узнать adnId на который редиректить (из контракта) и удалить mmp-тарификатор.
    val result = DB.withConnection { implicit c =>
      val adnIdOpt = MBillMmpDaily.getById(mmpId).flatMap(_.contract).map(_.adnId)
      val rowsDeleted = MBillMmpDaily.deleteById(mmpId)
      (adnIdOpt, rowsDeleted)
    }
    val (adnIdOpt, rowsDeleted) = result
    val flashInfo: (String, String) = rowsDeleted match {
      case 1  =>  "success" -> s"Mmp-тарификатор #$mmpId удалён."
      case 0  =>  "error"   -> s"Mmp-тарификатор #$mmpId НЕ НАЙДЕН."
      case _  =>  "error"   -> s"Неизвестный результат операции для mmp#$mmpId: $rowsDeleted. Ожидалось 1 или 0."
    }
    val rdrCall = adnIdOpt.fold(routes.SysMarket.index()) { routes.SysMarketBilling.billingFor }
    Redirect(rdrCall)
      .flashing(flashInfo)
  }

}
