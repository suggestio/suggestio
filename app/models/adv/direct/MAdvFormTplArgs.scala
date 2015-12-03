package models.adv.direct

import models.adv.MAdvI


/** Интерфейс контейнера аргументов для формы размещения на узлах. */
trait IAdvFormTplArgs {
  def adId              : String
  def af                : DirectAdvFormM_t
  def busyAdvs          : Map[String, MAdvI]
  def cities            : Seq[MAdvFormCity]
  def adnId2formIndex   : Map[String, Int]
  def advPeriodsAvail   : Seq[String]
}


/** Аргументы для рендера страницы управления рекламной карточкой с формой размещения оной. */
case class MAdvFormTplArgs(
  override val adId              : String,
  override val af                : DirectAdvFormM_t,
  override val busyAdvs          : Map[String, MAdvI],
  override val cities            : Seq[MAdvFormCity],
  override val adnId2formIndex   : Map[String, Int],
  override val advPeriodsAvail   : Seq[String]
)
  extends IAdvFormTplArgs
