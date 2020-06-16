package io.suggest.file

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.common.geom.d2.MSize2di
import io.suggest.n2.media.MFileMeta
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.up.MFileUploadS
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
      (a.blob             ===* b.blob) &&
      (a.fileMeta         ===* b.fileMeta) &&
      (a.blobUrl          ===* b.blobUrl) &&
      (a.fileName         ===* b.fileName) &&
      (a.whPx             ===* b.whPx) &&
      (a.upload           ===* b.upload)
    }
  }

  @inline implicit def univEq: UnivEq[MJsFileInfo] = UnivEq.derive

  def blob      = GenLens[MJsFileInfo](_.blob)
  def fileMeta  = GenLens[MJsFileInfo](_.fileMeta)
  def blobUrl   = GenLens[MJsFileInfo](_.blobUrl)
  def fileName  = GenLens[MJsFileInfo](_.fileName)
  def whPx      = GenLens[MJsFileInfo](_.whPx)
  def upload    = GenLens[MJsFileInfo](_.upload)

}


/** Класс-контейнер рантаймовой client-side инфы по файлу.
  *
  * @param blob Локально-доступный бинарь, который описывается моделью.
  * @param fileMeta Метаданные файла.
  * @param blobUrl Ссылка на блоб в памяти браузера, если есть.
  * @param fileName Название файла, если известно.
  * @param whPx Ширина и длина картинки.
  * @param upload Состояние заливки картинки на сервер, если есть.
  */
case class MJsFileInfo(
                        blob              : Blob,
                        fileMeta          : MFileMeta,
                        blobUrl           : Option[String]          = None,
                        fileName          : Option[String]          = None,
                        whPx              : Option[MSize2di]        = None,
                        upload            : MFileUploadS            = MFileUploadS.empty,
                      )
  extends EmptyProduct
