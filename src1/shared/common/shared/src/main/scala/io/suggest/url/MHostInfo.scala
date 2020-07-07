package io.suggest.url

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.17 15:45
  * Description: Модель инфы по одному хосту в кластере s.io.
  */
object MHostInfo {

  object Fields {
    def NAME_PUBLIC_FN    = "he"
    def NAME_INTERNAL_FN  = "hi"
  }

  implicit def MHOST_INFO_FORMAT: OFormat[MHostInfo] = {
    val F = Fields
    (
      (__ \ F.NAME_INTERNAL_FN).format[String] and
      (__ \ F.NAME_PUBLIC_FN).format[String]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MHostInfo] = UnivEq.derive


  implicit final class HostInfoOpsExt( private val hostInfo: MHostInfo ) extends AnyVal {

    /** Все хостнеймы, содержащиеся в инстанса MHostInfo.. */
    def allHostNames: Set[String] =
      Set.empty + hostInfo.nameInt + hostInfo.namePublic

  }

}


/** Класс модели хостнеймов.
  *
  * @param nameInt Внутренний хостнейм, доступный только внутри локальной сети.
  * @param namePublic Хостнейм, доступный для интернета.
  */
case class MHostInfo(
                      nameInt      : String,
                      namePublic   : String,
                    )
