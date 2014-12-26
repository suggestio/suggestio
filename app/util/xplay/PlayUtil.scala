package util.xplay

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 11:11
 * Description: Утиль для связи с play.
 */
object PlayUtil {

  /** TCP-порт, используемый play для запуска http-сервера. */
  lazy val httpPort = Option(System.getProperty("http.port")).map(Integer.parseInt).getOrElse(9000)

}
