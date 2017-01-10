package io.suggest.adv.geo

import io.suggest.geo.MGeoPoint
import boopickle.Default._
import io.suggest.dt.MAdvPeriod

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:53
  * Description: Модели состояния формы георазмещения, расшаренные между клиентом и сервером.
  *
  * Для передачи модели между клиентом и сервером используется бинарный boopickle + BASE64 (RFC4648_URLSAFE желательно).
  * Бинарщина быстрая и обфусцированная даже на предмет типов и имён полей.
  *
  * 2016.12.28: Было решено, что эта модель должна быть более эфемерна, чем было изначально.
  * Т.е. должна быть js-only модель MRoot, с полями, которые соответствуют компонентам,
  * MRoot при необходимости может экспортировать одноразовые инстансы со своими кусками в виде вот этой
  * клиент-серверной модели. Это поможет выкинуть поле adId, сгуппировать разные разбросанные модели по полям.
  */

object MFormS {

  // TODO Opt Можно проверсти тюнинг boopickle на предмет скорости.
  // https://github.com/ochrons/boopickle#optimizations-strategies

  implicit val pickler: Pickler[MFormS] = {
    implicit val dateIntervalPickler = MAdvPeriod.pickler
    implicit val mmapsP = MMapS.pickler
    implicit val rpsP = MRcvrPopupState.pickler
    implicit val circleP = MCircleS.pickler
    implicit val a4fsP = MAdv4Free.pickler
    generatePickler[MFormS]
  }

}


/** Корневая модель состояния формы георазмещения на базе diode circuit.
  * Этот класс пошарен между сервером и клиентом, поэтому
  *
  * @param mapState id георазмещаемой рекламной карточки.
  * @param locationFound Состояние геолокации и реакции на неё:
  *                      true уже карта была отцентрована по обнаруженной геолокации.
  *                      false началась геолокация, нужно отцентровать карту по опредённым координатам.
  *                      None Нет ни геолокации, ничего.
  * param existCircles Текущие кружки, если есть.
  */
case class MFormS(
                   adId          : String,
                   mapState      : MMapS,
                   onMainScreen  : Boolean                 = true,
                   adv4free      : Option[MAdv4Free]       = None,
                   // rcvrPopup : Option[MRcvrPopupState]  // -> MRoot.rcvr.popupState
                   locationFound : Option[Boolean]          = None
                   // rcvrsMap    : RcvrsMap_t            // -> MRoot.rcvr.rcvrMap
                   //tagsEdit    : MTagsEditProps         // -> MRoot.tagsEditState.props
                   //datePeriod    : MAdvPeriod           // -> MRoot.datePeriod
                   // TODO existCircles  : List[MCircleInfo + id exist-размещения + цвет]       = Nil
                   // TODO exist circle popup Option[...]
                   // TODO price, currency?
) {

  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)
  def withOnMainScreen(oms2: Boolean) = copy(onMainScreen = oms2)
  def withMapState(ms2: MMapS) = copy(mapState = ms2)

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
object MAdv4FreeProps {
  implicit val pickler: Pickler[MAdv4FreeProps] = generatePickler[MAdv4FreeProps]
}


/** Модель diode-состояния суперюзерской формочки.
  * @param checked Состояние галочки: true/false.
  */
case class MAdv4Free(
  static  : MAdv4FreeProps,
  checked : Boolean         = true
) {
  def withChecked(checked2: Boolean) = copy(checked = checked2)
}
object MAdv4Free {
  implicit val pickler: Pickler[MAdv4Free] = {
    implicit val propsP = MAdv4FreeProps.pickler
    generatePickler[MAdv4Free]
  }
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
object MMapS {
  implicit val pickler: Pickler[MMapS] = {
    implicit val mgpPickler = MGeoPoint.pickler
    generatePickler[MMapS]
  }
}


/** Ключ в карте текущих ресиверов. */
case class RcvrKey(from: String, to: String, groupId: Option[String]) {
  override def toString = from + "." + to + "." + groupId.getOrElse("")
}


/** Состояние попапа над ресивером на карте. */
case class MRcvrPopupState(
  nodeId  : String,
  latLng  : MGeoPoint
)
object MRcvrPopupState {
  implicit val pickler: Pickler[MRcvrPopupState] = {
    implicit val mgpPickler = MGeoPoint.pickler
    generatePickler[MRcvrPopupState]
  }
}


/** Состояние круга. */
case class MCircleS(center: MGeoPoint, radiusM: Double = 3000) {
  def withCenter(center2: MGeoPoint) = copy(center = center2)
  def withRadius(radius2: Double) = copy(radiusM = radius2)
}
object MCircleS {
  implicit val pickler: Pickler[MCircleS] = {
    implicit val mgpPickler = MGeoPoint.pickler
    generatePickler[MCircleS]
  }
}
