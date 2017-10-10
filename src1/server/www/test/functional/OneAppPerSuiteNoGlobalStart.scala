package functional

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 10:10
 * Description: Поддержка бесстартового запуска приложения для ускорения нестирования.
 */
trait OneAppPerSuiteNoGlobalStart extends GuiceOneAppPerSuite { this: TestSuite =>

  /**
    * Штатный Global производит долгую инициализацию, которая нам не нужна.
    * Нужен только доступ к конфигу. Ускоряем запуск:
    */
  override implicit lazy val app: Application = {
    GuiceApplicationBuilder()
      .in( Mode.Test )
      .build()
  }

}
