package models.msys

import models.mbill._

/** У шаблона [[views.html.sys1.market.billing.adnNodeBillingTpl]] очень много параметров со сложными типам.
  * Тут удобный контейнер для всей кучи параметров шаблона. */

trait ISysAdnNodeBillingArgs {
  def balanceOpt        : Option[MBalance]
  def contracts         : Seq[MContract]
  def txns              : Seq[MTxn]
  def feeTariffsMap     : collection.Map[Int, Seq[MTariffFee]]
  def dailyMmpsMap      : collection.Map[Int, Seq[MTariffDaily]]
  def sinkComissionMap  : collection.Map[Int, Seq[MSinkComission]]
}


case class MSysAdnNodeBillingArgs(
  override val balanceOpt        : Option[MBalance],
  override val contracts         : Seq[MContract],
  override val txns              : Seq[MTxn],
  override val feeTariffsMap     : collection.Map[Int, Seq[MTariffFee]],
  override val dailyMmpsMap      : collection.Map[Int, Seq[MTariffDaily]],
  override val sinkComissionMap  : collection.Map[Int, Seq[MSinkComission]]
)
  extends ISysAdnNodeBillingArgs
