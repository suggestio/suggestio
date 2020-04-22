package io.suggest.ueq

import diode.{ModelR, ModelRO}
import diode.data.Pot
import japgolly.univeq.UnivEq
import org.scalajs.dom.experimental.permissions.{PermissionName, PermissionState}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.{Blob, File, WebSocket}
import org.scalajs.dom.raw.XMLHttpRequest

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.17 12:58
  * Description: Утиль для UnivEq в контексте использования на стороне JS.
  * Изначально, тут жили implicit-костыли для DOM.
  */
object JsUnivEqUtil {

  @inline implicit def fileUe           : UnivEq[File]              = UnivEq.force

  @inline implicit def xhrUe            : UnivEq[XMLHttpRequest]    = UnivEq.force

  @inline implicit def blobUe           : UnivEq[Blob]              = UnivEq.force

  @inline implicit def potUe[T]         : UnivEq[Pot[T]]            = UnivEq.force

  @inline implicit def webSocketUe      : UnivEq[WebSocket]         = UnivEq.force

  @inline implicit def jsArrayUe[T: UnivEq]: UnivEq[js.Array[T]]    = UnivEq.force

  @inline implicit def ajaxInputDataUe  : UnivEq[Ajax.InputData]    = UnivEq.force

  @inline implicit def jsAnyUe          : UnivEq[js.Any]            = UnivEq.force

  @inline implicit def permissionStatusUe: UnivEq[PermissionState]  = UnivEq.force
  @inline implicit def permissionNameUe : UnivEq[PermissionName]    = UnivEq.force

  @inline implicit def modelRoUe[T: UnivEq]: UnivEq[ModelRO[T]]     = UnivEq.force
  @inline implicit def modelRUe[M: UnivEq, T: UnivEq]: UnivEq[ModelR[M,T]] = UnivEq.force

  @inline implicit def jsFunUnivEq: UnivEq[js.Function] = UnivEq.force

}
