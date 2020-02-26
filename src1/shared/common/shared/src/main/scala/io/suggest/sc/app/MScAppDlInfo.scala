package io.suggest.sc.app

import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.ext.svc.MExtService
import io.suggest.n2.edge.MPredicate
import io.suggest.n2.media.MFileMeta
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
    def OS_FAMILY     = "o"
    def PREDICATE     = "p"
    def URL           = "u"
    def EXT_SERVICE   = "s"
    def FILE_NAME     = "f"
    def FROM_NODE_ID  = "n"
    def FILE_META     = "h"
  }

  implicit def scAppDlInfoJson: OFormat[MScAppDlInfo] = {
    val F = Fields
    (
      (__ \ F.PREDICATE).format[MPredicate] and
      (__ \ F.URL).format[String] and
      (__ \ F.OS_FAMILY).formatNullable[MOsFamily] and
      (__ \ F.EXT_SERVICE).formatNullable[MExtService] and
      (__ \ F.FILE_NAME).formatNullable[String] and
      (__ \ F.FROM_NODE_ID).formatNullable[String] and
      (__ \ F.FILE_META).formatNullable[MFileMeta]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScAppDlInfo] = UnivEq.derive


  implicit class ScAppDlInfoOpsExt( val dlInfo: MScAppDlInfo ) extends AnyVal {

    /** @return Это прямая закачка под Apple iOS? */
    def isIosFileDl: Boolean = {
      (dlInfo.osFamily contains[MOsFamily] MOsFamilies.Apple_iOS) &&
      dlInfo.extSvc.isEmpty
    }

  }

}


/** Инфа по варианту скачивания приложения под одну платформу (семейство ОС).
  *
  * @param predicate Предикат.
  * @param url Ссылка для скачивания/редиректа.
  * @param osFamily Семейство ОС, для которых скомпилировано приложение.
  *                 Сделан сразу опциональным, просто на всякий случай (вдруг будут какие-то приложения без привязки к ОС).
  * @param extSvc Внешний сервис.
  * @param fileName Имя файла.
  * @param fromNodeIdOpt С какого узла получена инфа по данному вопросу.
  * @param fileMeta Инфа о файле.
  */
case class MScAppDlInfo(
                         predicate        : MPredicate,
                         url              : String,
                         osFamily         : Option[MOsFamily],
                         extSvc           : Option[MExtService] = None,
                         fileName         : Option[String] = None,
                         fromNodeIdOpt    : Option[String] = None,
                         fileMeta         : Option[MFileMeta] = None,
                       )
