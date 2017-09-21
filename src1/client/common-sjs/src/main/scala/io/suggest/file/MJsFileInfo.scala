package io.suggest.file

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.ueq.UnivEqJsUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import org.scalajs.dom.File
import org.scalajs.dom.raw.XMLHttpRequest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 18:39
  * Description: Client-only модель рантаймовых данных по файлу, в частности -- по оригиналу картинки
  * (деривативщину за файлы не считаем для простоты).
  */
object MJsFileInfo {

  /** Поддержка FastEq для инстансов [[MJsFileInfo]]. */
  implicit object MJsFileInfoFastEq extends FastEq[MJsFileInfo] {
    override def eqv(a: MJsFileInfo, b: MJsFileInfo): Boolean = {
      (a.file ===* b.file) &&
        (a.blobUrl ===* b.blobUrl) &&
        (a.uploadProgress ===* b.uploadProgress) &&
        (a.uploadXhr ===* b.uploadXhr) &&
        (a.srvFileInfo ===* b.srvFileInfo)
    }
  }

  implicit def univEq: UnivEq[MJsFileInfo] = UnivEq.derive

}


/** Класс-контейнер рантаймовой client-side инфы по файлу.
  *
  * @param file Локально-доступный файл, который описывается моделью.
  * @param blobUrl Ссылка на блоб в памяти браузера, если есть.
  * @param uploadProgress Прогресс заливки картинки на сервер, если есть.
  * @param uploadXhr XHR-реквест, производящий сейчас upload файла на сервер.
  * @param srvFileInfo Инфа по файлу, который хранится на сервере.
  */
case class MJsFileInfo(
                        file              : Option[File]            = None,
                        blobUrl           : Option[String]          = None,
                        uploadProgress    : Option[Int]             = None,
                        uploadXhr         : Option[XMLHttpRequest]  = None,
                        srvFileInfo       : Option[MSrvFileInfo]    = None
                      )
  extends EmptyProduct
{

  def withFile(file: Option[File])                        = copy(file = file)
  def withBlobUrl(blobUrl: Option[String])                = copy(blobUrl = blobUrl)
  def withUploadProgress(uploadProgress : Option[Int])    = copy(uploadProgress = uploadProgress)
  def withUploadXhr(uploadXhr: Option[XMLHttpRequest])    = copy(uploadXhr = uploadXhr)
  def withSrvFileInfo(srvFileInfo: Option[MSrvFileInfo])  = copy(srvFileInfo = srvFileInfo)

}
