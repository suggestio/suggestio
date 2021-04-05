package io.suggest.up

import io.suggest.common.empty.EmptyUtil
import io.suggest.file.MSrvFileInfo
import io.suggest.i18n.MMessage
import io.suggest.jd.MJdEdge
import io.suggest.url.MHostUrl
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 16:08
  * Description: Модель ответа сервера на запрос подготовки к аплоаду.
  *
  * Разделение вариантов ответа происходит на уровне полей единого класса:
  * - Ссылка для выполнения аплоада, если всё ок.
  * - Инфа по уже загруженному файлу, если файл уже загружен.
  * - Сообщения об ошибках, если всё совсем плохо.
  */
object MUploadResp {

  /** Поддержка play-json для инстансов [[MUploadResp]]. */
  implicit val MUPLOAD_RESP_FORMAT: OFormat[MUploadResp] = (
    (__ \ "u").formatNullable[List[MHostUrl]]
      .inmap[List[MHostUrl]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { urls => if (urls.isEmpty) None else Some(urls) }
      ) and
    (__ \ "x").formatNullable[MJdEdge] and
    (__ \ "e").formatNullable[Seq[MMessage]]
      .inmap[Seq[MMessage]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        { msgs => if (msgs.isEmpty) None else Some(msgs) }
      ) and
    (__ \ "t").formatNullable[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MUploadResp] = {
    UnivEq.derive
  }

}


/** Класс модели ответа сервера по поводу аплоада.
  *
  * @param upUrls Ссылка для произведения аплоада.
  * @param fileExist Инфа об уже существующем файле на сервере.
  *                  Используется MJdEdge, т.к. nodeId живёт вне MSrvFileInfo.
  * @param errors Список сообщений об ошибках, из-за которых продолжение не очень возможно.
  * @param extra Какая-то прочая информация.
  *              Используется для проброса MEdge/MNodeEdgeIdQs в sys-edge-edit.
  */
case class MUploadResp(
                        upUrls       : List[MHostUrl]        = Nil,
                        fileExist    : Option[MJdEdge]       = None,
                        errors       : Seq[MMessage]         = Nil,
                        extra        : Option[String]        = None,
                      )

