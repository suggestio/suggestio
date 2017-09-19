package io.suggest.ad.edit.m.edit

import diode.FastEq
import japgolly.univeq.UnivEq
import org.scalajs.dom.File

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.17 18:39
  * Description: Client-only модель каких-то чисто-рантаймовых данных по файлу,
  * в частности -- по картинке.
  *
  * Изнаально жила внутри карты, где ключ -- это id jd-эджа.
  * В самой же организации модели её цикл жизни не затрагивается.
  */
object MFileInfo {

  /** Поддержка FastEq для инстансов [[MFileInfo]]. */
  implicit object MPictureInfoFastEq extends FastEq[MFileInfo] {
    override def eqv(a: MFileInfo, b: MFileInfo): Boolean = {
      (a.file eq b.file) &&
        (a.blobUrl eq b.blobUrl) &&
        (a.uploadProgress eq b.uploadProgress)
    }
  }

  implicit def univEq: UnivEq[MFileInfo] = {
    import io.suggest.dom.DomUnivEq._
    UnivEq.derive
  }

}


/** Класс-контейнер рантаймовой client-side инфы по файлу.
  *
  * @param file Файл, который описывается.
  * @param blobUrl Ссылка на блоб в памяти браузера, если есть.
  * @param uploadProgress Прогресс заливки картинки на сервер, если есть.
  */
case class MFileInfo(
                      file              : File,
                      blobUrl           : Option[String]    = None,
                      uploadProgress    : Option[Int]       = None
                    ) {

  def withBlobUrl(blobUrl: Option[String]) = copy(blobUrl = blobUrl)
  def withUploadProgress(uploadProgress : Option[Int]) = copy(uploadProgress = uploadProgress)

}
