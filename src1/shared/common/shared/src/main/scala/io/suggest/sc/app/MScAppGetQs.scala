package io.suggest.sc.app

import io.suggest.dev.MOsFamily
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 18:53
  * Description: Модель аргументов для реквеста на сервер для получения инфы
  * по скачиванию приложения (получения ссылки/метаданных на скачивание).
  */
object MScAppGetQs {

  object Fields {
    def OS_FAMILY = "o"
    def ON_NODE_ID = "n"
  }

  /** Поддержка play-json. */
  implicit def scAppGetQsJson: OFormat[MScAppGetQs] = {
    val F = Fields
    (
      (__ \ F.OS_FAMILY).format[MOsFamily] and
      (__ \ F.ON_NODE_ID).formatNullable[String]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScAppGetQs] = UnivEq.derive

}


/** Контейнер данных запроса ссылки доступа к приложению.
  *
  * @param onNodeId id узла, на котором запрошено приложение.
  *                 Нужна, чтобы понять, надо ли
  * @param osFamily Платформа дистрибуции.
  *                   None - означает локальную раздачу файлов.
  */
case class MScAppGetQs(
                        osFamily      : MOsFamily,
                        onNodeId      : Option[String]         = None,
                      )
