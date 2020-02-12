package io.suggest.sc.app

import io.suggest.ext.svc.MExtService
import io.suggest.n2.edge.MPredicate
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.02.2020 17:42
  * Description: Контейнер инфы по приложению под одну ось.
  */
object MScAppDlInfo {

  object Fields {
    def PREDICATE     = "p"
    def URL           = "u"
    def EXT_SERVICE   = "s"
    def FILE_NAME     = "f"
    def FILE_SIZE_B   = "b"
  }

  implicit def scAppDlInfoJson: OFormat[MScAppDlInfo] = {
    val F = Fields
    (
      (__ \ F.PREDICATE).format[MPredicate] and
      (__ \ F.URL).format[String] and
      (__ \ F.EXT_SERVICE).formatNullable[MExtService] and
      (__ \ F.FILE_NAME).formatNullable[String] and
      (__ \ F.FILE_SIZE_B).formatNullable[Long]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScAppDlInfo] = UnivEq.derive

}


/** Инфа по варианту скачивания приложения под одну платформу.
  *
  * @param predicate Предикат.
  * @param url Ссылка для скачивания/редиректа.
  * @param extSvc Внешний сервис.
  * @param fileName Имя файла.
  * @param fileSizeB Размер файла в байтах.
  */
case class MScAppDlInfo(
                         predicate   : MPredicate,
                         url         : String,
                         extSvc      : Option[MExtService] = None,
                         fileName    : Option[String] = None,
                         fileSizeB   : Option[Long]   = None,
                       )
