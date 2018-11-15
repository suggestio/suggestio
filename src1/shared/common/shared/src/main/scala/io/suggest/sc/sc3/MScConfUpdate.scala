package io.suggest.sc.sc3

import io.suggest.maps.nodes.MRcvrsMapUrlArgs
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.18 14:31
  * Description: Sc-экшен для апдейта конфига выдачи.
  * Сервер может поставлять какие-то дополнительные данные, переопределяя данные в текущем конфиге выдачи.
  */
object MScConfUpdate {

  /** Поддержка play-json. */
  implicit def scConfUpdateFormat: OFormat[MScConfUpdate] = {
    val F = MSc3Conf.Fields
    (__ \ F.RCVRS_MAP_FN).formatNullable[MRcvrsMapUrlArgs]
      .inmap[MScConfUpdate](apply, _.rcvrsMap)
  }

  @inline implicit def univEq: UnivEq[MScConfUpdate] = UnivEq.derive

}


/** Модель-контейнер данных обновления конфигурации выдачи.
  *
  * @param rcvrsMap Обновлённые данные для карты ресиверов.
  */
case class MScConfUpdate(
                          rcvrsMap        : Option[MRcvrsMapUrlArgs]      = None,
                        )
