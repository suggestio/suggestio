package io.suggest.routes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.18 21:55
  * Description: Интерфейс type-class'а для извлечения основных данных http-запроса: URL и Method,
  * а также для сборки абсолютных URL на основе какого-либо js-роутера.
  */
trait HttpRouteExtractor[T] {

  /** Прочитать ссылку. */
  def url(t: T): String

  /** Прочитать http-метод. */
  def method(t: T): String

  /** Сборка абсолютной ссылки. */
  def absoluteUrl(t: T, secure: Boolean): String

  /** Сборка абсолютной ссылки для websocket. */
  def webSocketUrl(t: T, secure: Boolean): String

}
