package util.di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.10.15 22:11
 * Description: Доступ к контроллеру обработчика ошибок.
 */
trait IErrorHandler {
  def errorHandler    : controllers.ErrorHandler
}
