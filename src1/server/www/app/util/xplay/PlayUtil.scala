package util.xplay

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 11:11
 * Description: Утиль для связи с play.
 */
final class PlayUtil {

  /** TCP-порт, используемый play для запуска http-сервера. */
  def httpPort = {
    Option( System.getProperty("http.port") )
      .fold(9000)(Integer.parseInt)
  }

}
