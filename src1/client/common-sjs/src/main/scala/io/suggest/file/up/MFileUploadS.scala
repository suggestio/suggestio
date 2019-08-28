package io.suggest.file.up

import diode.FastEq
import diode.data.Pot
import org.scalajs.dom.raw.XMLHttpRequest
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 12:57
  * Description: js-модель состояния процесса аплоада файла на сервер.
  */
object MFileUploadS {

  def empty = MFileUploadS()

  /** Поддержка FastEq для инстансов [[MFileUploadS]]. */
  implicit object MFileUploadSFastEq extends FastEq[MFileUploadS] {
    override def eqv(a: MFileUploadS, b: MFileUploadS): Boolean = {
      (a.xhr ===* b.xhr) &&
        (a.prepareReq ===* b.prepareReq) &&
        (a.uploadReq ===* b.uploadReq) &&
        (a.progress ===* b.progress)
    }
  }

  @inline implicit def univEq: UnivEq[MFileUploadS] = UnivEq.derive

  val xhr = GenLens[MFileUploadS](_.xhr)
  val prepareReq = GenLens[MFileUploadS](_.prepareReq)
  //val uploadReq = GenLens[MFileUploadS](_.uploadReq)

}


/** Класс модели состояния аплоада файла на сервер.
  *
  * @param xhr XHR-инстанс происходящего сейчас реквеста (для возможности отмены).
  *            С учётом FetctAPI, инстанса может и не быть.
  * @param prepareReq Pot реквеста подготовки к аплода.
  * @param uploadReq Pot реквеста аплоада файла.
  * @param progress Сколько процентов файла уже отправлено пройдено.
  */
case class MFileUploadS(
                         xhr          : Option[XMLHttpRequest]     = None,
                         prepareReq   : Pot[MUploadResp]           = Pot.empty,
                         uploadReq    : Pot[MUploadResp]           = Pot.empty,
                         progress     : Option[Int]                = None
                       )

