package io.suggest.maps.nodes

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.18 11:55
  * Description: Модель данных с сервера для сборки ссылки на клиенте.
  */
object MRcvrsMapUrlArgs {

  /** Поддержка play-json. */
  implicit def mRcvrsMapUrlArgsFormat: OFormat[MRcvrsMapUrlArgs] = (
    (__ \ "c").format[String] and
    (__ \ "h").format[Int]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MRcvrsMapUrlArgs] = UnivEq.derive


  import boopickle.Default._

  implicit val rcvrsMapUrlArgsP: Pickler[MRcvrsMapUrlArgs] = {
    generatePickler[MRcvrsMapUrlArgs]
  }

}


/** Контейнер данных для сборки ссылки.
  *
  * @param cdnHost Хост-порт.
  * @param hashSum Контрольная сумма.
  */
case class MRcvrsMapUrlArgs(
                             cdnHost    : String,
                             hashSum    : Int
                           )