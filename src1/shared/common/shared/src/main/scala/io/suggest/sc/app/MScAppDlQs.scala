package io.suggest.sc.app

import io.suggest.dev.MOs
import io.suggest.pwa.manifest.MAppDistributor
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 18:53
  * Description: Реквест для скачивания (получения ссылки/метаданных на скачивание).
  */
object MScAppDlQs {

  object Fields {
    def OS = "o"
    def DISTRIBUTOR = "d"
  }

  /** Поддержка play-json. */
  implicit def scAppDlReqJson: OFormat[MScAppDlQs] = {
    val F = Fields
    (
      (__ \ F.OS).formatNullable[MOs] and
      (__ \ F.DISTRIBUTOR).formatNullable[MAppDistributor]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScAppDlQs] = UnivEq.derive

}


/** Контейнер данных запроса ссылки доступа к приложению.
  *
  * @param os Ось девайса.
  * @param distributor Платформа дистрибуции.
  */
case class MScAppDlQs(
                       os              : Option[MOs],
                       distributor     : Option[MAppDistributor],
                     )
