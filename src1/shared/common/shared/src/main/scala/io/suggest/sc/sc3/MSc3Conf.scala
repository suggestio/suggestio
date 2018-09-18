package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.sc.MScApiVsn
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptOps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 22:18
  * Description: Модель конфига выдачи. Константы как всегда задаются сервером.
  */
object MSc3Conf {

  /** Поддержка play-json.
    * def, ведь на клиенте это нужно только один раз.
    */
  implicit def MSC3_CONF_FORMAT: OFormat[MSc3Conf] = (
    (__ \ "r").format[String] and
    (__ \ "l").format[Boolean] and
    (__ \ "a").format[String] and
    (__ \ "v").format[MScApiVsn] and
    (__ \ "d").formatNullable[Boolean]
      // Отладка включена по-умолчанию, если явно не задана в конфиге!
      .inmap[Boolean]( _.getOrElseTrue, EmptyUtil.someF )
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MSc3Conf] = UnivEq.derive

}


/** Контейнер данных конфигурации, задаваемой на сервере.
  *
  * @param rcvrsMapUrl Ссылка на данные карты ресиверов.
  */
case class MSc3Conf(
                     rcvrsMapUrl    : String,
                     isLoggedIn     : Boolean,
                     aboutSioNodeId : String,
                     apiVsn         : MScApiVsn,
                     debug          : Boolean,
                   )
