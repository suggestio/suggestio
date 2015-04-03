package models.adv.ext.act

import akka.actor.ActorPath
import play.api.mvc.QueryStringBindable
import util.PlayMacroLogsDyn
import util.qsb.QsbSigner
import util.secure.SecretGetter

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 16:57
 * Description: Бывает, что нужно передавать actor path в URL. Тут qsb-модель для этой задачи.
 */
object ActorPathQs extends PlayMacroLogsDyn {

  // Суффиксы имен qs-аргументов
  def PATH_SUF = ".p"
  def SIGN_SUF = ".sig"

  /** Статический секретный ключ для подписывания запросов к dyn-картинкам. */
  private[models] val SIGN_SECRET: String = {
    val sg = new SecretGetter {
      override val confKey = "qsb.actor.path.sign.key"
      override def LOGGER = ActorPathQs.LOGGER
    }
    sg()
  }


  /** qsb для извлечения нотариально заверенного путя из URL qs. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[ActorPathQs] = {
    new QueryStringBindable[ActorPathQs] {

      /** Фасад для работы с секретным ключом подписи. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key$SIGN_SUF")

      /** Десериализация. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, ActorPathQs]] = {
        // Собираем результат
        val keyDotted = if (!key.isEmpty) s"$key." else key
        for {
          params    <- getQsbSigner(key).signedOrNone(keyDotted, params0)
          maybePath <- strB.bind(key + PATH_SUF, params)
        } yield {
          maybePath.right.flatMap { path =>
            try {
              val apath = ActorPath.fromString(path)
              Right(ActorPathQs(apath))
            } catch {
              case ex: Throwable =>
                LOGGER.error("Failed to extract actorPath from qs", ex)
                Left(ex.getMessage)
            }
          }
        }
      }

      /** Сериализация. */
      override def unbind(key: String, value: ActorPathQs): String = {
        val qsRaw = strB.unbind(key + PATH_SUF, value.path.toStringWithoutAddress)
        getQsbSigner(key).mkSigned(key, qsRaw)
      }
    }
  }

}


/**
 * Экземпляр модели.
 * @param path Путь до актора.
 */
case class ActorPathQs(path: ActorPath)
