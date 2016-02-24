package models.adv.direct

import io.suggest.mbill2.m.item.MItem


/** Интерфейс контейнера аргументов для формы размещения на узлах. */
trait IAdvFormTplArgs {

  /** id размещаемой рекламной карточки. */
  def adId              : String

  /** Маппинг формы размещения. */
  def af                : DirectAdvFormM_t

  /** Карта уже занятых нод. */
  def busyAdvs          : Map[String, MItem]

  /** Модели городов, содержат категории и узлы в оных. */
  def cities            : Seq[MAdvFormCity]

  /** Карта int-индексов для узлов. Нужна, потому что list-маппинг формы мыслит числовыми id, а нужны строковые. */
  def adnId2formIndex   : Map[String, Int]

  /** Доступные периоды размещения. */
  def advPeriodsAvail   : Seq[String]


  // Вспомогательное API

  /** Рассчитать кол-во доступных для размещения нод в указанной категории. */
  def totalAvailCat(cat: MAdvFormCityCat): Int = {
    cat.nodes
      .iterator
      .count { n => !n.node.id.exists(busyAdvs.contains) }
  }

  /** Рассчитать кол-во доступных для размещения нод в указанной категории,
    * но возвращать только положительные результаты. */
  def totalAvailOptCat(cat: MAdvFormCityCat): Option[Int] = {
    Option( totalAvailCat(cat) )
      .filter(_ > 0)
  }

}


/** Аргументы для рендера страницы управления рекламной карточкой с формой размещения оной. */
case class MAdvFormTplArgs(
  override val adId              : String,
  override val af                : DirectAdvFormM_t,
  override val busyAdvs          : Map[String, MItem],
  override val cities            : Seq[MAdvFormCity],
  override val adnId2formIndex   : Map[String, Int],
  override val advPeriodsAvail   : Seq[String]
)
  extends IAdvFormTplArgs
