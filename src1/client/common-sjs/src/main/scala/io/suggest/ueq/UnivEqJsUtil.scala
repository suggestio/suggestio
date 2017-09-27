package io.suggest.ueq

import japgolly.univeq.UnivEq
import org.scalajs.dom.{Blob, File}
import org.scalajs.dom.raw.XMLHttpRequest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 12:58
  * Description: Утиль для UnivEq в контексте использования на стороне JS.
  * Изначально, тут жили implicit-костыли для DOM.
  */
object UnivEqJsUtil {

  implicit def fileUe: UnivEq[File] = UnivEq.force

  implicit def xhrUe: UnivEq[XMLHttpRequest] = UnivEq.force

  implicit def blobUe: UnivEq[Blob] = UnivEq.force

}
