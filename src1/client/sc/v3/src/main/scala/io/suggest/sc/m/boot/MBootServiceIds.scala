package io.suggest.sc.m.boot

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.01.19 12:31
  * Description: Модель целей для загрузки.
  */
object MBootServiceIds extends Enum[MBootServiceId] {

  /** Запуск только начался, самая ранняя стадия, выставляемая по дефолту.
    * В случае cordova - даже кордова ещё не инициализирована.
    */
  case object JsRouter extends MBootServiceId

  /** Инициализация платформы и оборудования.
    * cordova - это ожидание platformReady.
    * браузер - ready сразу.
    */
  case object Platform extends MBootServiceId

  /** Инициализации карты ресиверов. */
  case object RcvrsMap extends MBootServiceId

  /** Dump current permissions states. */
  case object ReadPermissions extends MBootServiceId

  /** Сбор данных детектирования геолокации. */
  case object GeoLocDataAcc extends MBootServiceId

  /** Load first index, fill showcase with content. */
  case object InitShowcaseIndex extends MBootServiceId

  /** Open notifications dialog with permissions requests. */
  case object PermissionsGui extends MBootServiceId

  /** Need to initialize interface language, when router & platform will be ready. */
  case object Language extends MBootServiceId


  override def values = findValues

}


/** Класс для элементов модели стадии запуска. */
sealed abstract class MBootServiceId extends EnumEntry

object MBootServiceId {

  @inline implicit def univEq: UnivEq[MBootServiceId] = UnivEq.derive

}
