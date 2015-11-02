package models.msys

import models._

/** У шаблона [[views.html.sys1.market.billing.adnNodeBillingTpl]] очень много параметров со сложными типам.
  * Тут удобный контейнер для всей кучи параметров шаблона. */

trait ISysAdnNodeBillingArgs {
  def balanceOpt        : Option[MBillBalance]
  def contracts         : Seq[MBillContract]
  def txns              : Seq[MBillTxn]
  def feeTariffsMap     : collection.Map[Int, Seq[MBillTariffFee]]
  def statTariffsMap    : collection.Map[Int, Seq[MBillTariffStat]]
  def dailyMmpsMap      : collection.Map[Int, Seq[MBillMmpDaily]]
  def sinkComissionMap  : collection.Map[Int, Seq[MSinkComission]]
}


case class MSysAdnNodeBillingArgs(
  override val balanceOpt        : Option[MBillBalance],
  override val contracts         : Seq[MBillContract],
  override val txns              : Seq[MBillTxn],
  override val feeTariffsMap     : collection.Map[Int, Seq[MBillTariffFee]],
  override val statTariffsMap    : collection.Map[Int, Seq[MBillTariffStat]],
  override val dailyMmpsMap      : collection.Map[Int, Seq[MBillMmpDaily]],
  override val sinkComissionMap  : collection.Map[Int, Seq[MSinkComission]]
)
  extends ISysAdnNodeBillingArgs
