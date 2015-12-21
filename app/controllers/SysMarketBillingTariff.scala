package controllers

import com.google.inject.Inject
import io.suggest.ym.parsers.Price
import models.mbill.{MContract, MTariff, MTariffFee}
import models.mproj.ICommonDi
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.PlayMacroLogsImpl
import util.acl._
import views.html.sys1.market.billing.tariff._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 21:12
 * Description: Работа с fee- и stat-тарифами в биллинге.
 */
class SysMarketBillingTariff @Inject() (
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsSuperuserContract
  with IsSuperuserFeeTariffContract
{

  import LOGGER._
  import mCommonDi._

  private def nameKM      = "name"      -> nonEmptyText(maxLength = 128)
  private def enabledKM   = "enabled"   -> boolean
  private def dateFirstKM = "dateFirst" -> jodaDate("dd.MM.yyyy HH:mm")

  /** Генератор форм для различных тарифов абонплаты. */
  private def feeTariffFormM(contractId: Int) = Form(mapping(
    nameKM,
    enabledKM,
    dateFirstKM,
    "tinterval" -> pgIntervalM,
    "price"     -> priceStrictNoraw
  )
  {(name, enabled, dateFirst, tinterval, price) =>
    MTariffFee(
      contractId  = contractId,
      name        = name,
      isEnabled   = enabled,
      dateFirst   = dateFirst,
      fee         = price.price,
      feeCC       = price.currency.getCurrencyCode,
      tinterval   = tinterval
    )
  }
  {tariff =>
    import tariff._
    Some((name, isEnabled, dateFirst, tinterval, Price(fee, feeCurrency)))
  })


  /** Отрендерить страницу с формой создания нового тарифа. */
  def addFeeTariffForm(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    import request.mcontract
    mNodeCache.getById(mcontract.adnId) map { adnNodeOpt =>
      Ok(addTariffFormTpl(adnNodeOpt.get, mcontract, feeTariffFormM(contractId)))
    }
  }

  /** Сабмит формы добавления нового тарифа. */
  def addFeeTariffFormSubmit(contractId: Int) = IsSuperuserContract(contractId).async { implicit request =>
    lazy val logPrefix = s"addFeeTariffFormSubmit($contractId): "
    import request.mcontract
    val formBinded = feeTariffFormM(contractId).bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        mNodeCache.getById(mcontract.adnId) map { adnNodeOpt =>
          NotAcceptable(addTariffFormTpl(adnNodeOpt.get, mcontract, formWithErrors))
        }
      },
      {tariff =>
        // Добавляем новый тариф в adnNode
        val tariffSaved = db.withConnection { implicit c =>
          tariff.save
        }
        val flashMsg = s"Тариф #${tariffSaved.id.get} добавлен к договору ${mcontract.legalContractId}."
        rdrFlashing(mcontract.adnId, flashMsg)
      }
    )
  }


  /** Отрендерить страницу с формой редактирования существующего тарифа. */
  def editFeeTariffForm(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    import request.{mTariffFee, mcontract}
    mNodeCache.getById(mcontract.adnId) map { adnNodeOpt =>
      val form = feeTariffFormM(mcontract.id.get).fill(mTariffFee)
      Ok(editTariffFormTpl(adnNodeOpt.get, mcontract, mTariffFee, form))
    }
  }

  /** Сабмит формы редактирования тарифа. */
  def editFeeTariffFormSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).async { implicit request =>
    lazy val logPrefix = s"editFeeTariffFormSubmit($tariffId): "
    import request.{mTariffFee => tariff0, mcontract}
    feeTariffFormM(mcontract.id.get).bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        mNodeCache.getById(mcontract.adnId).map { adnNodeOpt =>
          NotAcceptable(editTariffFormTpl(adnNodeOpt.get, mcontract, tariff0, formWithErrors))
        }
      },
      {tariff1 =>
        val now = DateTime.now
        val tariff2 = tariff0.copy(
          name        = tariff1.name,
          tinterval   = tariff1.tinterval,
          fee         = tariff1.fee,
          feeCC       = tariff1.feeCC,
          dateModified = Some(now),
          dateFirst   = tariff1.dateFirst,
          isEnabled   = tariff1.isEnabled,
          dateStatus  = if (tariff0.isEnabled != tariff1.isEnabled) now else tariff0.dateStatus
        )
        val tariffSaved = db.withConnection { implicit c =>
          tariff2.save
        }
        val flashMsg = editSaveFlashMsg(tariffSaved, mcontract)
        rdrFlashing(mcontract.adnId, flashMsg)
      }
    )
  }

  /** POST на удаление тарифа. */
  def deleteFeeTariffSubmit(tariffId: Int) = IsSuperuserFeeTariffContract(tariffId).apply { implicit request =>
    import request.{mTariffFee, mcontract}
    val rowsDeleted = db.withConnection { implicit c =>
      mTariffFee.delete
    }
    val flashMsg = deleteFlashMsg(tariffId, rowsDeleted, mcontract)
    rdrFlashing(mcontract.adnId, flashMsg)
  }


  /** Перенаправить юзера на страницу биллинга узла с flash-сообщением об успехе. */
  private def rdrFlashing(adnId: String, message: String) = {
    Redirect(routes.SysMarketBilling.billingFor(adnId))
      .flashing(FLASH.SUCCESS -> message)
  }

  private def editSaveFlashMsg(tariff: MTariff, contract: MContract): String = {
    s"Изменения в тарифе #${tariff.id.get} сохранены (договор ${contract.legalContractId})."
  }

  private def deleteFlashMsg(tariffId: Int, rowsDeleted: Int, contract: MContract): String = {
    s"Тариф #$tariffId удалён: ${rowsDeleted > 0}. Договор ${contract.legalContractId}."
  }

}
