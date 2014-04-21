package controllers

import util.PlayMacroLogsImpl
import play.api.data._, Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._
import models._
import io.suggest.ym.model.common.{PeriodicalTariffBody, TariffBody}
import util.FormUtil._
import views.html.sys1.market.billing.tariff._
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.14 21:12
 * Description: Работа с тарифами в биллинге.
 */
object SysMarketBillingTariff extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Маппер тела тарифа абонплаты. */
  val periodicalTariffBodyM = mapping(
    "period" -> isoPeriodM,
    "price"  -> priceStrictNoraw
  )
  // apply()
  {(period, price) =>
    PeriodicalTariffBody(period, price.price, price.currency.getCurrencyCode)
  }
  {ptb =>
    Some((ptb.period, ptb.getPrice))
  }


  /** Генератор форм для различных тарифов. */
  def tariffFormM[T <: TariffBody](tBodyM: Mapping[T]) = Form(mapping(
    "name"    -> nonEmptyText(maxLength = 128),
    "ttype"   -> tariffTypeM,
    "enabled" -> boolean,
    "dateFirst" -> jodaDate,
    "tbody"   -> tBodyM
  )
  {(name, ttype, enabled, dateFirst, tbody) =>
    Tariff(
      id = -1,
      name = name,
      tType = ttype,
      isEnabled = enabled,
      dateFirst = dateFirst,
      tBody = tbody
    )
  }
  {tariff =>
    import tariff._
    Some((name, tType, isEnabled, dateFirst, tBody.asInstanceOf[T]))
  })

  val periodicalTariffFormM = tariffFormM(periodicalTariffBodyM)


  /** Отрендерить страницу с формой создания нового тарифа. */
  def addTariffForm(adnId: String) = IsSuperuserAdnNode(adnId).apply { implicit request =>
    Ok(addTariffFormTpl(request.adnNode, periodicalTariffFormM))
  }

  /** Сабмит формы добавления нового тарифа. */
  def addTariffFormSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    lazy val logPrefix = s"addTariffFormSubmit($adnId): "
    val formBinded = periodicalTariffFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(addTariffFormTpl(adnNode, formWithErrors))
      },
      {tariff =>
        // Добавляем новый тариф в adnNode
        tariff.id = adnNode.nextTariffId
        adnNode.tariffs ::= tariff
        adnNode.save.map { _ =>
          rdrFlashing(adnId, s"Тариф #${tariff.id} добавлен у узлу $adnId")
        }
      }
    )
  }


  /** Отрендерить страницу с формой редактирования существующего тарифа. */
  def editTariffForm(adnId: String, tariffId: Int) = IsSuperuserAdnNode(adnId).apply { implicit request =>
    import request.adnNode
    adnNode.getTariffById(tariffId) match {
      case Some(tariff) =>
        val form = periodicalTariffFormM.fill(tariff)
        Ok(editTariffFormTpl(adnNode, tariff, form))

      case None => NotFound(s"Tariff #$tariffId does not exist for node $adnId")
    }
  }

  /** Сабмит формы редактирования тарифа. */
  def editTariffFormSubmit(adnId: String, tariffId: Int) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    lazy val logPrefix = s"editTariffFormSubmit($adnId): "
    val tariff0 = adnNode.getTariffById(tariffId).get
    periodicalTariffFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(editTariffFormTpl(adnNode, tariff0, formWithErrors))
      },
      {tariff1 =>
        tariff0.name = tariff1.name
        tariff0.tType = tariff1.tType
        tariff0.tBody = tariff1.tBody
        val now = DateTime.now
        tariff0.dateModified = Some(now)
        if (tariff0.isEnabled != tariff1.isEnabled) {
          tariff0.dateStatus = now
          tariff0.isEnabled = tariff1.isEnabled
        }
        // Изменяемый тариф - записывать его внутрь adnNode не нужно: он уже там. Просто сохраняем обновлённый adnNode
        adnNode.save.map { _ =>
          rdrFlashing(adnId, s"Изменения в тарифе #$tariffId сохранены.")
        }
      }
    )
  }

  /** POST на удаление тарифа. */
  def deleteTariffSubmit(adnId: String, tariffId: Int) = IsSuperuserAdnNode(adnId).async { implicit request =>
    import request.adnNode
    adnNode.tariffs = adnNode.tariffs.filter(_.id != tariffId)
    adnNode.save.map { _ =>
      rdrFlashing(adnId, s"Тарифа #$tariffId удалён из узла.")
    }
  }


  /** Перенаправить юзера на страницу биллинга узла с flash-сообщением об успехе. */
  private def rdrFlashing(adnId: String, message: String) = {
    Redirect(routes.SysMarketBilling.billingFor(adnId))
      .flashing("success" -> message)
  }
}
