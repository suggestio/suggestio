package controllers

import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import play.api.db.DB
import play.api.Play.current
import views.html.sys1.market.billing._
import play.api.data._, Forms._
import util.FormUtil._
import org.joda.time.DateTime
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 12:39
 * Description: Контроллер управления биллинга для операторов sio-market.
 */
object SysMarketBilling extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Маппинг для формы добавления/редактирования контракта. */
  val contractFormM = Form(mapping(
    "adnId"         -> esIdM,
    "dateContract"  -> localDate
      .transform[DateTime](_.toDateTimeAtStartOfDay, _.toLocalDate)
    ,
    "suffix"        -> optional(
      text(maxLength = 16)
        .transform(strTrimSanitizeF, strIdentityF)
    ),
    "isActive"      -> boolean,
    "hiddenInfo"    -> text(maxLength = 4096)
      .transform(strFmtTrimF, strIdentityF)
      .transform[Option[String]](
        {Some(_).filter(!_.isEmpty)},
        {_ getOrElse ""}
      )
  )
  // apply()
  {(adnId, dateContract, suffix, isActive, hiddenInfo) =>
    MBillContract(
      adnId = adnId,
      contractDate = dateContract,
      suffix = suffix,
      hiddenInfo = hiddenInfo,
      isActive = isActive
    )
  }
  // unapply()
  {mbc =>
    import mbc._
    Some((adnId, contractDate, suffix, isActive, hiddenInfo))
  })


  /** Страница с информацией по биллингу. */
  def billingFor(adnId: String) = IsSuperuser.async { implicit request =>
    val adnNodeOptFut = MAdnNode.getById(adnId)
    // Синхронные модели
    val syncResult = DB.withConnection { implicit c =>
      val balanceOpt = MBillBalance.getByAdnId(adnId)
      val contracts = MBillContract.findForAdn(adnId)
      val contractIds = contracts.map(_.id.get)
      val txns = MBillTxn.findForContracts(contractIds)
      (balanceOpt, contracts, txns)
    }
    adnNodeOptFut map {
      case Some(adnNode) =>
        val (balanceOpt, contracts, txns) = syncResult
        Ok(adnNodeBillingTpl(adnNode, balanceOpt, contracts, txns))
      case None =>
        adnNodeNotFound(adnId)
    }
  }

  /** Форма создания нового контракта (договора). */
  def createContractForm(adnId: String) = IsSuperuser.async { implicit request =>
    getNodeAndSupAsync(adnId) map {
      case Some((adnNode, supOpt)) =>
        Ok(createContractFormTpl(adnNode, supOpt, contractFormM))

      case None => adnNodeNotFound(adnId)
    }
  }

  /** Сабмит формы создания нового контакта (договора). */
  def createContractFormSubmit = IsSuperuser.async(parse.urlFormEncoded) { implicit request =>
    contractFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("createContractFormSubmit(): Form bind failed: " + formatFormErrors(formWithErrors))
        val adnId = request.body("adnId").head
        getNodeAndSupAsync(adnId) map {
          case Some((adnNode, supOpt)) =>
            NotAcceptable(createContractFormTpl(adnNode, supOpt, formWithErrors))
          case None =>
            adnNodeNotFound(adnId)
        }
      },
      {mbc =>
        val mbc1 = DB.withConnection { implicit c =>
          mbc.save
        }
        Redirect(routes.SysMarketBilling.billingFor(mbc1.adnId))
          .flashing("success" -> s"Создан договор #${mbc1.legalContractId}.")
      }
    )
  }


  /** Запрос страницы редактирования контракта. */
  def editContractForm(contractId: Int) = IsSuperuser.async { implicit request =>
    val mbcOpt = DB.withConnection { implicit c =>
      MBillContract.getById(contractId)
    }
    mbcOpt match {
      case Some(mbc) =>
        val afut = getNodeAndSupAsync(mbc.adnId)
        val formFilled = contractFormM.fill(mbc)
        afut map {
          case Some((adnNode, supOpt)) =>
            Ok(editContractFormTpl(adnNode, supOpt, mbc, formFilled))
          case None =>
            adnNodeNotFound(mbc.adnId)
        }

      case None => NotFound("No such contract: " + contractId)
    }
  }

  /** Самбит формы редактирования договора. */
  def editContractFormSubmit(contractId: Int) = IsSuperuser.async { implicit request =>
    val mbc0 = DB.withConnection { implicit c =>
      MBillContract.getById(contractId).get
    }
    contractFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("editContractFormSubmit(): Form bind failed: " + formatFormErrors(formWithErrors))
        val adnId = mbc0.adnId
        getNodeAndSupAsync(adnId) map {
          case Some((adnNode, supOpt)) =>
            NotAcceptable(editContractFormTpl(adnNode, supOpt, mbc0, formWithErrors))
          case None =>
            adnNodeNotFound(adnId)
        }
      },
      {mbc1 =>
        mbc0.contractDate = mbc1.contractDate
        mbc0.hiddenInfo   = mbc1.hiddenInfo
        mbc0.isActive     = mbc1.isActive
        mbc0.suffix       = mbc1.suffix
        DB.withConnection { implicit c =>
          mbc0.save
        }
        Redirect(routes.SysMarketBilling.billingFor(mbc1.adnId))
          .flashing("success" -> s"Изменения сохранены: #${mbc1.legalContractId}.")
      }
    )
  }


  /** Что рисовать в браузере, если не найден запрошенный узел рекламной сети. */
  private def adnNodeNotFound(adnId: String) = {
    NotFound("Adn node " + adnId + " does not exists.")
  }

  private def getNodeAndSupAsync(adnId: String): Future[Option[(MAdnNode, Option[MAdnNode])]] = {
    MAdnNodeCache.getByIdCached(adnId) flatMap {
      case Some(adnNode) =>
        MAdnNodeCache.maybeGetByIdCached(adnNode.adn.supId) map { supOpt =>
          Some(adnNode -> supOpt)
        }
      case None => Future successful None
    }
  }

}
