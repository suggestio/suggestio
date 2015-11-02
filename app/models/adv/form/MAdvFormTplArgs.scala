package models.adv.form

import models.MAdvI
import play.api.data.Form


/** Интерфейс контейнера аргументов для формы размещения на узлах. */
trait IAdvFormTplArgs {
  def adId              : String
  def af                : Form[_]
  def busyAdvs          : Map[String, MAdvI]
  def cities            : Seq[MAdvFormCity]
  def adnId2formIndex   : Map[String, Int]
  def advPeriodsAvail   : List[(String, String)]
}


/** Аргументы для рендера страницы управления рекламной карточкой с формой размещения оной. */
case class MAdvFormTplArgs(
  override val adId              : String,
  override val af                : Form[_],
  override val busyAdvs          : Map[String, MAdvI],
  override val cities            : Seq[MAdvFormCity],
  override val adnId2formIndex   : Map[String, Int],
  override val advPeriodsAvail   : List[(String, String)]
)
  extends IAdvFormTplArgs
