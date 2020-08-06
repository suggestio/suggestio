package io.suggest.id.login.m

import diode.Effect
import io.suggest.proto.http.model.HttpClientConfig
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
trait LoginFormDiConf {

  def onClose(): Option[Callback]

  def onRedirect(onAction: ILoginFormAction, rdrUrl: String): Effect

  def httpClientConfig(): HttpClientConfig

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

    // Доп.костыли для csrf не требуются. CSRF уже вставлен в js-роутер.
    override def httpClientConfig() = HttpClientConfig.empty

  }

}
