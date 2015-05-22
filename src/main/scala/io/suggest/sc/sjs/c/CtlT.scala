package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mv.{VCtx, IVCtx}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 11:28
 * Description: Абстрактный контроллер задается здесь.
 */
trait CtlT {

  /** Поддержка автоматической сборки контекста для view'ов. */
  implicit def vctx: IVCtx = new VCtx

}
