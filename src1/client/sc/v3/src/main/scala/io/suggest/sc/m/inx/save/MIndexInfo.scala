package io.suggest.sc.m.inx.save

import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.2020 11:28
  * Description: Контейнер инфы по одному пройденному индексу.
  */
object MIndexInfo {

  object Fields {
    def INDEX_RESP = "i"
    def SC_STATE = "s"
  }


  implicit def indexInfoJson: OFormat[MIndexInfo] = {
    val F = Fields
    (
      (__ \ F.INDEX_RESP).format[MSc3IndexResp] and
      (__ \ F.SC_STATE).format[MainScreen]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MIndexInfo] = UnivEq.derive

}


/**
  *
  * @param indexResp Описание индекса. Может быть минифицировано (без welcome и прочих полей).
  *             Для рендера списка через NfListR нужен список инстансов MSc3IndexResp.
  * @param state Состояние выдачи в момент этого индекса.
  */
case class MIndexInfo(
                       indexResp      : MSc3IndexResp,
                       state          : MainScreen,
                     )
