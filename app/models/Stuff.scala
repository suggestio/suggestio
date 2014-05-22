package models

import java.util.Currency
import play.api.i18n.Messages

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.04.14 17:09
 * Description: Моделе-подобное барахло, которые в основном нужно для шаблонов.
 */

object CurrencyCodeOpt {
  val CURRENCY_CODE_DFLT = "RUB"
}

/** Опциональное поле currencyCode, подразумевающее дефолтовую валюту. */
trait CurrencyCodeOpt {
  def currencyCodeOpt : Option[String]

  def currencyCode = currencyCodeOpt getOrElse CurrencyCodeOpt.CURRENCY_CODE_DFLT
  def currency = Currency.getInstance(currencyCode)
}


/** У шаблона [[views.html.sys1.market.billing.adnNodeBillingTpl]] очень много параметров со сложными типам.
  * Тут удобный контейнер для всей кучи параметров шаблона. */
case class SysAdnNodeBillingArgs(
  balanceOpt: Option[MBillBalance],
  contracts: Seq[MBillContract],
  txns: Seq[MBillTxn],
  feeTariffsMap: collection.Map[Int, Seq[MBillTariffFee]],
  statTariffsMap: collection.Map[Int, Seq[MBillTariffStat]]
)


/** Статическая утиль для шаблонов, работающих со экшенами статистики. */
object AdStatActionsTpl {

  def adStatActionI18N(asa: AdStatAction): String = {
    "ad.stat.action." + asa.toString
  }

  /** Фунция для генерации списка пар (String, String), которые описывают  */
  def adStatActionsSeq(implicit ctx: Context): Seq[(AdStatAction, String)] = {
    import ctx._
    AdStatActions.values.toSeq.map { v =>
      val i18n = adStatActionI18N(v)
      v -> Messages(i18n)
    }
  }

  def adStatActionsSeqStr(implicit ctx: Context): Seq[(String, String)] = {
    import ctx._
    AdStatActions.values.toSeq.map { v =>
      val i18n = adStatActionI18N(v)
      v.toString -> Messages(i18n)
    }
  }

}



/** Выбор высоты шрифта влияет на высоту линии (интерлиньяж) и возможно иные параметры.
  * В любом случае, ключом является кегль шрифта. */
case class FontSize(size: Int, lineHeight: Int)
