package io.suggest.file

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.common.geom.d2.MSize2di
import io.suggest.crypto.hash.HashesHex
import io.suggest.file.up.MFileUploadS
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import org.scalajs.dom.Blob

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 18:39
  * Description: Client-only модель рантаймовых данных по файлу, в частности -- по оригиналу картинки
  * (всякую деривативщину за допустимые файлы не считаем, для простоты).
  *
  * JS-файл описывается блобом этого файла, остальные поля необязательны.
  * File: имя файла хранится в отдельном опциональном поле.
  */
object MJsFileInfo {

  /** Поддержка FastEq для инстансов [[MJsFileInfo]]. */
  implicit object MJsFileInfoFastEq extends FastEq[MJsFileInfo] {
    override def eqv(a: MJsFileInfo, b: MJsFileInfo): Boolean = {
      (a.blob               ===* b.blob) &&
        (a.blobUrl          ===* b.blobUrl) &&
        (a.fileName         ===* b.fileName) &&
        (a.hashesHex        ===* b.hashesHex) &&
        (a.whPx             ===* b.whPx) &&
        (a.upload           ===* b.upload)
    }
  }

  @inline implicit def univEq: UnivEq[MJsFileInfo] = UnivEq.derive

  val blob      = GenLens[MJsFileInfo](_.blob)
  val blobUrl   = GenLens[MJsFileInfo](_.blobUrl)
  val fileName  = GenLens[MJsFileInfo](_.fileName)
  val hashesHex = GenLens[MJsFileInfo](_.hashesHex)
  val whPx      = GenLens[MJsFileInfo](_.whPx)
  val upload    = GenLens[MJsFileInfo](_.whPx)

}


/** Класс-контейнер рантаймовой client-side инфы по файлу.
  *
  * @param blob Локально-доступный бинарь, который описывается моделью.
  * @param blobUrl Ссылка на блоб в памяти браузера, если есть.
  * @param fileName Название файла, если известно.
  * @param hashesHex Мапа с хэшами текущего файла (блоба).
  * @param whPx Ширина и длина картинки.
  * @param upload Состояние заливки картинки на сервер, если есть.
  */
case class MJsFileInfo(
                        blob              : Blob,
                        blobUrl           : Option[String]          = None,
                        fileName          : Option[String]          = None,
                        hashesHex         : HashesHex               = Map.empty,
                        whPx              : Option[MSize2di]        = None,
                        upload            : Option[MFileUploadS]    = None
                      )
  extends EmptyProduct
{

  def withBlob(blob: Blob)                                = copy(blob = blob)
  def withBlobUrl(blobUrl: Option[String])                = copy(blobUrl = blobUrl)
  def withFileName(fileName: Option[String])              = copy(fileName = fileName)
  def withHashesHex(hashesHex: HashesHex)                 = copy(hashesHex = hashesHex)
  def withWhPx(whPx: Option[MSize2di])                    = copy(whPx = whPx)
  def withUpload(upload: Option[MFileUploadS])            = copy(upload = upload)

}
