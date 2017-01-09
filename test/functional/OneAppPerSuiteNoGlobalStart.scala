package functional

import org.scalatest.Suite
import org.scalatestplus.play._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 10:10
 * Description: Поддержка бесстартового запуска приложения для ускорения нестирования.
 */
trait OneAppPerSuiteNoGlobalStart extends OneAppPerSuite { this: Suite =>

  /**
    * Штатный Global производит долгую инициализацию, которая нам не нужна.
    * Нужен только доступ к конфигу. Ускоряем запуск:
    */
  override implicit lazy val app: Application = {
    GuiceApplicationBuilder(global = None)
      .in( Mode.Test )
      .build()
  }

}
