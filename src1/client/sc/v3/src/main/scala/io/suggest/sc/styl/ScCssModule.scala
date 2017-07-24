package io.suggest.sc.styl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 10:37
  * Description: DI-модуль для доступа к CSS-стилям выдачи.
  * Вся сложность заключается в том, что стиль варьирует своё состоянияе согласно модели в Sc3Circuit,
  * и очень интересен всем views.
  */

import com.softwaremill.macwire._
import io.suggest.sc.Sc3CircuitModule

/** Factory-модуль для сборки инстансов ScCss, который зависит от аргументов рендера,
  * но допускает использование как-то внешних зависимостей.
  */
class ScCssFactoryModule {

  /** Параметризованная сборка ScCss (здесь можно добавлять DI-зависимости). */
  def mkScCss(args: MScCssArgs): ScCss = {
    wire[ScCss]
  }

}


class ScCssModule( sc3CircuitModule: Sc3CircuitModule ) {

  /** Функция-геттер для получения текущего инстанса  */
  val getScCssF: GetScCssF = { () =>
    sc3CircuitModule.sc3Circuit.scCss()
  }

}
