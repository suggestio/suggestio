package io.suggest

import io.suggest.i18n.MCommonReactCtx
import japgolly.scalajs.react.React

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 12:06
  * Description: DI-модуль без DI. Пока это чушь конечно, но пока будет так.
  */
object ReactCommonModule {

  /** Расшаренный контейнер react-контекста. */
  lazy val commonReactCtx: React.Context[MCommonReactCtx] =
    React.createContext( MCommonReactCtx.default )

}
