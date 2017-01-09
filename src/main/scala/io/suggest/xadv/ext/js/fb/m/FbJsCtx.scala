package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.{MJsCtxT, MJsCtx, MJsCtxWrapperT}

import scala.scalajs.js.Any

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.03.15 18:06
 * Description: Враппер над [[io.suggest.xadv.ext.js.runner.m.MJsCtx]], который предоставляет доступ
 * к fb-контекстам.
 */

object FbJsCtx {

  /** Можно вызывать только в handleTarget() фазе, т.к. fbCtx должен быть уже определен. */
  def apply(jsCtxUnderlying: MJsCtx): FbJsCtx = {
    FbJsCtx(
      jsCtxUnderlying = jsCtxUnderlying,
      fbCtx = jsCtxUnderlying.custom
        .map { FbCtx.fromJson }
        .get
    )
  }

}

trait FbJsCtxT extends MJsCtxWrapperT {

  override def jsCtxUnderlying: MJsCtxT

  def fbCtx: FbCtx

  override def svcTargets: Seq[FbExtTarget] = {
    super.svcTargets.map {
      FbExtTarget.fromTarget
    }
  }

  override def target: Option[FbExtTarget] = {
    super.target.map {
      FbExtTarget.fromTarget
    }
  }

  override def custom: Option[Any] = {
    Some( fbCtx.toJson )
  }
}

trait FbJsCtxWrapperT extends FbJsCtxT {
  override def jsCtxUnderlying: FbJsCtxT
  override def fbCtx = jsCtxUnderlying.fbCtx
}

case class FbJsCtx(jsCtxUnderlying: MJsCtx, fbCtx: FbCtx) extends FbJsCtxT {
  override lazy val target = super.target
  override lazy val svcTargets = super.svcTargets
}
