package io.suggest.id.login.m

import diode.Effect
import io.suggest.proto.http.model.IMHttpClientConfig
import io.suggest.sjs.dom2.DomQuick
import japgolly.scalajs.react.Callback
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DoNothing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.08.2020 17:23
  * Description: Интерфейс для формы логина.
  */
trait LoginFormDiConf extends IMHttpClientConfig {

  def onClose(): Option[Callback]

  def onRedirect(onAction: ILoginFormAction, rdrUrl: String): Effect

}


object LoginFormDiConf {

  object Isolated extends LoginFormDiConf {

    /** Закрытие формы на собственной странице невозможно. */
    override def onClose() = None

    /** Сервер прислал редирект - выполнить этот самый редирект. */
    override def onRedirect(onAction: ILoginFormAction, rdrUrl: String): Effect = {
      Effect.action {
        DomQuick.goToLocation( rdrUrl )
        DoNothing
      }
    }

  }

}
