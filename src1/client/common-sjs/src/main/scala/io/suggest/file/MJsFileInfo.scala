package io.suggest.file

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.ueq.UnivEqJsUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import org.scalajs.dom.Blob
import org.scalajs.dom.raw.XMLHttpRequest

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
        (a.hash             ===* b.hash) &&
        (a.uploadProgress   ===* b.uploadProgress) &&
        (a.uploadXhr        ===* b.uploadXhr)
    }
  }

  implicit def univEq: UnivEq[MJsFileInfo] = UnivEq.derive

}


/** Класс-контейнер рантаймовой client-side инфы по файлу.
  *
  * @param blob Локально-доступный бинарь, который описывается моделью.
  * @param blobUrl Ссылка на блоб в памяти браузера, если есть.
  * @param fileName Название файла, если известно.
  * @param hash Результат поверхностного хеширования файла.
  * @param uploadProgress Прогресс заливки картинки на сервер, если есть.
  * @param uploadXhr XHR-реквест, производящий сейчас upload файла на сервер.
  */
case class MJsFileInfo(
                        blob              : Blob,
                        blobUrl           : Option[String]          = None,
                        fileName          : Option[String]          = None,
                        hash              : Option[Int]             = None,
                        uploadProgress    : Option[Int]             = None,
                        uploadXhr         : Option[XMLHttpRequest]  = None
                      )
  extends EmptyProduct
{

  def withBlob(blob: Blob)                                = copy(blob = blob)
  def withBlobUrl(blobUrl: Option[String])                = copy(blobUrl = blobUrl)
  def withFileName(fileName: Option[String])              = copy(fileName = fileName)
  def withHash(hash: Option[Int])                         = copy(hash = hash)
  def withUploadProgress(uploadProgress: Option[Int])     = copy(uploadProgress = uploadProgress)
  def withUploadXhr(uploadXhr: Option[XMLHttpRequest])    = copy(uploadXhr = uploadXhr)

}
