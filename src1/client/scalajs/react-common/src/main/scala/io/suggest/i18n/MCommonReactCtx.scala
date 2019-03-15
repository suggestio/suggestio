package io.suggest.i18n

import io.suggest.msg.Messages
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 11:52
  * Description: Общий sjs-react контекст для возможности смены языков.
  */
object MCommonReactCtx {

  @inline implicit def univEq: UnivEq[MCommonReactCtx] = UnivEq.derive

  lazy val default = MCommonReactCtx(
    messages = Messages
  )

}


/** Контейнер данных react-контекста.
  *
  * @param messages Доступ к i18n.
  */
case class MCommonReactCtx(
                            messages      : Messages,
                          )
