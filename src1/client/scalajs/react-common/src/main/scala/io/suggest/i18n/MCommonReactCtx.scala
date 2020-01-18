package io.suggest.i18n

import io.suggest.msg.Messages
import japgolly.scalajs.react.React
import japgolly.univeq.UnivEq
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

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


  implicit class CrCtxOpsExt( private val crCtxProv: React.Context[MCommonReactCtx] ) extends AnyVal {

    /** Короткий wrapper для рендера одного текстового сообщения.
      *
      * @param msgCode Код сообщеня.
      * @param args Аргументы сообщения.
      * @return VdomElement.
      */
    def message(msgCode: String, args: js.Any*): VdomElement = {
      crCtxProv.consume { crCtx =>
        crCtx.messages(msgCode, args: _*)
      }
    }

  }

}


/** Контейнер данных react-контекста.
  *
  * @param messages Доступ к i18n.
  */
case class MCommonReactCtx(
                            messages      : Messages,
                          )
