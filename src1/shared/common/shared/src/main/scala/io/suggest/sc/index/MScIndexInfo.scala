package io.suggest.sc.index

import io.suggest.spa.SioPages
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.2020 11:28
  * Description: Контейнер инфы по одному пройденному индексу.
  */
object MScIndexInfo {

  object Fields {
    def INDEX_RESP = "i"
    def SC_STATE = "s"
  }


  implicit def indexInfoJson: OFormat[MScIndexInfo] = {
    val F = Fields
    (
      (__ \ F.INDEX_RESP).format[MSc3IndexResp] and
      (__ \ F.SC_STATE).format[SioPages.Sc3]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MScIndexInfo] = UnivEq.derive

}


/**
  *
  * @param indexResp Описание индекса. Может быть минифицировано (без welcome и прочих полей).
  *             Для рендера списка через NfListR нужен список инстансов MSc3IndexResp.
  * @param state Состояние выдачи в момент этого индекса.
  */
case class MScIndexInfo(
                         indexResp      : MSc3IndexResp,
                         state          : SioPages.Sc3,
                       )
