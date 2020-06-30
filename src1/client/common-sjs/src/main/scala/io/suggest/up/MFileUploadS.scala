package io.suggest.up

import diode.FastEq
import diode.data.Pot
import io.suggest.proto.http.model.{HttpRespHolder, MTransferProgressInfo}
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
      (a.reqHolder ===* b.reqHolder) &&
      (a.prepareReq ===* b.prepareReq) &&
      (a.uploadReq ===* b.uploadReq) &&
      (a.progress ===* b.progress)
    }
  }

  @inline implicit def univEq: UnivEq[MFileUploadS] = UnivEq.derive

  def reqHolder = GenLens[MFileUploadS](_.reqHolder)
  def prepareReq = GenLens[MFileUploadS](_.prepareReq)
  def uploadReq = GenLens[MFileUploadS](_.uploadReq)
  def progress = GenLens[MFileUploadS](_.progress)


  implicit class FileUploadOpsExt( private val fUp: MFileUploadS ) extends AnyVal {

    /** Выбрать текущий реквест из двух.
      *
      * @return
      */
    def currentReq: Pot[MUploadResp] = {
      if (fUp.uploadReq ===* Pot.empty)
        fUp.prepareReq
      else
        fUp.uploadReq
    }

  }

}


/** Класс модели состояния аплоада файла на сервер.
  *
  * @param reqHolder XHR-инстанс происходящего сейчас реквеста (для возможности отмены).
  *            С учётом FetctAPI, инстанса может и не быть.
  * @param prepareReq Pot реквеста подготовки к аплода.
  * @param uploadReq Pot реквеста аплоада файла.
  * @param progress Сколько даннных уже отправлено на сервер.
  */
case class MFileUploadS(
                         reqHolder    : Option[HttpRespHolder]     = None,
                         // Может быть, надо объеденить оба реквеста?
                         prepareReq   : Pot[MUploadResp]           = Pot.empty,
                         uploadReq    : Pot[MUploadResp]           = Pot.empty,
                         progress     : Option[MTransferProgressInfo] = None
                       )

