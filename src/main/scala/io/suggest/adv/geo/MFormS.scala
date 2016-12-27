package io.suggest.adv.geo

import io.suggest.geo.MGeoPoint
import boopickle.Default._
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.dt.MAdvPeriod

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:53
  * Description: Модели состояния формы георазмещения, расшаренные между клиентом и сервером.
  *
  * Для передачи модели между клиентом и сервером используется бинарный boopickle + BASE64 (RFC4648_URLSAFE желательно).
  * Бинарщина быстрая и обфусцированная даже на предмет типов и имён полей.
  */

object MFormS {

  // TODO Opt Можно проверсти тюнинг boopickle на предмет скорости.
  // https://github.com/ochrons/boopickle#optimizations-strategies

  implicit val pickler: Pickler[MFormS] = {
    implicit val dateIntervalPickler = MAdvPeriod.pickler
    generatePickler[MFormS]
  }

  /** Сериализация модели. */
  def pickle(mroot: MFormS) = Pickle.intoBytes(mroot)

}


/** Корневая модель состояния формы георазмещения на базе diode circuit.
  * Этот класс пошарен между сервером и клиентом, поэтому
  *
  * @param mapState id георазмещаемой рекламной карточки.
  * @param geoAreas Состояние кружков на карте.
  * @param locationFound Состояние геолокации и реакции на неё:
  *                      true уже карта была отцентрована по обнаруженной геолокации.
  *                      false началась геолокация, нужно отцентровать карту по опредённым координатам.
  *                      None Нет ни геолокации, ничего.
  * @param rcvrPopup Состояние попапа на ресивере.
  * param existCircles Текущие кружки, если есть.
  */
case class MFormS(
                   adId          : String,
                   mapState      : MMapS,
                   onMainScreen  : Boolean                  = true,
                   adv4free      : Option[MAdv4FreeS]       = None,
                   geoAreas      : Option[MCircleInfo]      = None,
                   rcvrPopup     : Option[MRcvrPopupState]  = None,
                   locationFound : Option[Boolean]          = None,
                   rcvrsMap      : RcvrsMap_t               = Map.empty,
                   tags          : MTagsEditProps           = MTagsEditProps(),
                   datePeriod    : MAdvPeriod               = MAdvPeriod()
                   // TODO existCircles  : List[MCircleInfo + id exist-размещения + цвет]       = Nil
                   // TODO exist circle popup Option[...]
                   // TODO price, currency?
) {

  def withRcvrMap(rcvrsMap2: RcvrsMap_t) = copy(rcvrsMap = rcvrsMap2)
  def withAdv4Free(a4fOpt: Option[MAdv4FreeS]) = copy(adv4free = a4fOpt)
  def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
  def withTags(tags2: MTagsEditProps) = copy(tags = tags2)
  def withMapState(ms2: MMapS) = copy(mapState = ms2)
  def withRcvrPopup(rcvrPopup2: Option[MRcvrPopupState]) = copy(rcvrPopup = rcvrPopup2)
  def withDatePeriod(ivl: MAdvPeriod) = copy(datePeriod = ivl)

}


/** Статическая инфа по бесплатному размещению для суперюзеров.
  *
  * @param fn Имя form-поля: "freeAdv"
  * @param title Текст галочки: "Размещать бесплатно, без подтверждения?"
  */
case class MAdv4FreeProps(
  fn      : String,
  title   : String
)


/** Модель diode-состояния суперюзерской формочки.
  * @param checked Состояние галочки: true/false.
  */
case class MAdv4FreeS(
  static  : MAdv4FreeProps,
  checked : Boolean         = true
) {
  def withChecked(checked2: Boolean) = copy(checked = checked2)
}


/** Интерфейс модели состояния карты.
  * @param center Текущий центр карты.
  * @param zoom Текущий зум карты.
  */
case class MMapS(
  center  : MGeoPoint,
  zoom    : Int
) {
  def withCenter(center2: MGeoPoint) = copy(center = center2)
}


/** Модели описания состояния одного круга на географической карте.
  * @param center Координаты центра круга.
  * @param radius Радиус круга.
  */
case class MCircleInfo(
  center  : MGeoPoint,
  radius  : Double
)


/** Ключ в карте текущих ресиверов. */
case class RcvrKey(from: String, to: String, groupId: Option[String]) {
  override def toString = from + "." + to + "." + groupId.getOrElse("")
}


/** Состояние попапа над ресивером на карте. */
case class MRcvrPopupState(
  nodeId  : String,
  latLng  : MGeoPoint
)
