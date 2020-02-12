package io.suggest.sc.app

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.01.2020 18:39
  * Description: Модель JSON-ответа на запрос ссылки на скачивание.
  */
object MScAppGetResp {

  object Fields {
    def DOWNLOAD_VARIANTS = "d"
  }

  implicit def scAppDlRespJson: OFormat[MScAppGetResp] = {
    (__ \ Fields.DOWNLOAD_VARIANTS).format[Seq[MScAppDlInfo]]
      .inmap[MScAppGetResp]( apply, _.dlInfos )
  }

  @inline implicit def univEq: UnivEq[MScAppGetResp] = UnivEq.derive

}


/** Контейнер данных ответа по запросу download'а приложения.
  *
  * @param dlInfos Варианты для скачивания приложения.
  */
case class MScAppGetResp(
                          dlInfos       : Seq[MScAppDlInfo],
                        )
