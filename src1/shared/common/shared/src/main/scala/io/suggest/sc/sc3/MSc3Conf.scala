package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.sc.MScApiVsn
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.maps.nodes.MRcvrsMapUrlArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 22:18
  * Description: Модель конфига выдачи. Константы как всегда задаются сервером.
  */
object MSc3Conf {

  object Fields {
    val LOGGED_IN_FN                = "l"
    val ABOUT_SIO_NODE_ID_FN        = "a"
    val RCVRS_MAP_FN                = "r"
    val API_VSN_FN                  = "v"
    val DEBUG_FN                    = "d"
    val SERVER_GENERATED_AT_FN      = "g"
    val CLIENT_UPDATED_AT_FN        = "u"
  }

  /** Поддержка play-json.
    * def, ведь на клиенте это нужно только один раз.
    */
  implicit def MSC3_CONF_FORMAT: OFormat[MSc3Conf] = {
    val F = Fields
    (
      (__ \ F.LOGGED_IN_FN).format[Boolean] and
      (__ \ F.ABOUT_SIO_NODE_ID_FN).format[String] and
      (__ \ F.API_VSN_FN).format[MScApiVsn] and
      (__ \ F.DEBUG_FN).formatNullable[Boolean]
        // Если очень надо, отладка может быть ВКЛючена по-умолчанию, если явно не задана в конфиге: .getOrElseTrue
        .inmap[Boolean]( _.getOrElseFalse, EmptyUtil.someF ) and
      (__ \ F.RCVRS_MAP_FN).format[MRcvrsMapUrlArgs] and
      (__ \ F.SERVER_GENERATED_AT_FN).format[Long] and
      (__ \ F.CLIENT_UPDATED_AT_FN).formatNullable[Long]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MSc3Conf] = UnivEq.derive

  def timestampSec() = System.currentTimeMillis() / 1000

}


/** Контейнер данных конфигурации, задаваемой на сервере.
  *
  * @param rcvrsMap Данные для выкачивания карты ресиверов.
  * @param serverCreatedAt Timestamp генерации сервером данных этого конфига.
  * @param clientUpdatedAt Timestamp сохранение данных на клиенте.
  */
case class MSc3Conf(
                     isLoggedIn         : Boolean,
                     aboutSioNodeId     : String,
                     apiVsn             : MScApiVsn,
                     debug              : Boolean,
                     rcvrsMap           : MRcvrsMapUrlArgs,
                     serverCreatedAt    : Long              = MSc3Conf.timestampSec(),
                     clientUpdatedAt    : Option[Long]      = None,
                   ) {

  def withRcvrsMap(rcvrsMap: MRcvrsMapUrlArgs) = copy( rcvrsMap = rcvrsMap )
  def withClientUpdatedAt(clientUpdatedAt: Option[Long]) = copy(clientUpdatedAt = clientUpdatedAt)

}
