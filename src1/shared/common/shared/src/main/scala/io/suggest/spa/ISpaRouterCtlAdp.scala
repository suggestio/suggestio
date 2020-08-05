package io.suggest.spa

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.08.2020 14:22
  * Description: Бывает глубокое встраивание форм друг в друга, когда проблематично обойтись одним только
  * scalajs-react-роутером. При этом, все компоненты нижележащего модуля должны быть в состоянии взаимодействия
  * с роутером или каким-то его подобием.
  */
trait ISpaRouterCtlAdp[T <: SioPages] {

  /** Выставление указанной страницы в роутер. */
  def setPage(page: T): Unit

  /** Сборка ссылки для указанной страницы. */
  def urlFor(page: T): String

}
