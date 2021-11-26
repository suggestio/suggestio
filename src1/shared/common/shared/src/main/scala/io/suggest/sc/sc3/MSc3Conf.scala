package io.suggest.sc.sc3

import diode.FastEq
import io.suggest.common.empty.EmptyUtil
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.i18n.MLanguage
import io.suggest.maps.nodes.MRcvrsMapUrlArgs
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 22:18
  * Description: Модель конфига выдачи. Константы как всегда задаются сервером.
  */
object MSc3Conf {

  object Fields {
    def RCVRS_MAP_URL               = "r"
    def API_VSN                     = "v"
    def DEBUG_FLAG                  = "d"
    def SERVER_GENERATED_AT         = "g"
    def CLIENT_UPDATED_AT           = "u"
    def GEN                         = "e"
    def LANGUAGE                    = "lang"
  }

  /** Поддержка play-json.
    * def, ведь на клиенте это нужно только один раз.
    */
  implicit def MSC3_CONF_FORMAT: OFormat[MSc3Conf] = {
    val F = Fields
    (
      (__ \ F.API_VSN).format[MScApiVsn] and
      (__ \ F.DEBUG_FLAG).formatNullable[Boolean]
        // Если очень надо, отладка может быть ВКЛючена по-умолчанию, если явно не задана в конфиге: .getOrElseTrue
        .inmap[Boolean]( _.getOrElseFalse, EmptyUtil.someF ) and
      (__ \ F.RCVRS_MAP_URL).formatNullable[MRcvrsMapUrlArgs] and
      (__ \ F.SERVER_GENERATED_AT).format[Long] and
      (__ \ F.CLIENT_UPDATED_AT).formatNullable[Long] and
      (__ \ F.GEN).formatNullable[Long] and
      (__ \ F.LANGUAGE).formatNullable[MLanguage]
    )(apply, unlift(unapply))
  }

  implicit object MSc3ConfFastEq extends FastEq[MSc3Conf] {
    override def eqv(a: MSc3Conf, b: MSc3Conf): Boolean = {
      (a.rcvrsMapUrl ===* b.rcvrsMapUrl) &&
      (a.clientUpdatedAt ===* b.clientUpdatedAt)
    }
  }

  @inline implicit def univEq: UnivEq[MSc3Conf] = UnivEq.derive

  def timestampSec() = System.currentTimeMillis() / 1000

  def debug = GenLens[MSc3Conf]( _.debug )
  def rcvrsMapUrl = GenLens[MSc3Conf](_.rcvrsMapUrl)
  def clientUpdatedAt = GenLens[MSc3Conf](_.clientUpdatedAt)
  def gen = GenLens[MSc3Conf]( _.gen )
  def language = GenLens[MSc3Conf]( _.language )

}


/** Контейнер данных конфигурации, задаваемой на сервере.
  *
  * @param rcvrsMapUrl Данные для выкачивания карты ресиверов.
  * @param serverCreatedAt Timestamp генерации сервером данных этого конфига.
  * @param clientUpdatedAt Timestamp сохранение данных на клиенте.
  * @param debug Значение debug-флага в конфиге.
  * @param language Pre-configured language.
  *                 On server+browser: language detected from current session on server and forwarded via index.html.
  *                 On cordova: always None in index.html. May be set by user via ScSettings.
  */
final case class MSc3Conf(
                           apiVsn             : MScApiVsn         = MScApiVsns.unknownVsn,
                           debug              : Boolean           = false,
                           rcvrsMapUrl        : Option[MRcvrsMapUrlArgs] = None,
                           serverCreatedAt    : Long              = MSc3Conf.timestampSec(),
                           clientUpdatedAt    : Option[Long]      = None,
                           gen                : Option[Long]      = None,
                           language           : Option[MLanguage] = None,
                         )
