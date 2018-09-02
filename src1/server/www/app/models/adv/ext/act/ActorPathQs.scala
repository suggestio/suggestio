package models.adv.ext.act

import akka.actor.ActorPath
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import io.suggest.util.logs.MacroLogsDyn
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 16:57
 * Description: Бывает, что нужно передавать actor path в URL. Тут qsb-модель для этой задачи.
 */
object ActorPathQs extends MacroLogsDyn with SecretKeyInit {

  // Суффиксы имен qs-аргументов
  def PATH_FN  = "p"
  def SIGN_FN  = "sig"

  override def CONF_KEY = "qsb.actor.path.sign.key"

  /** qsb для извлечения нотариально заверенного путя из URL qs. */
  implicit def actorPathQsQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[ActorPathQs] = {
    new QueryStringBindableImpl[ActorPathQs] {

      /** Фасад для работы с секретным ключом подписи. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key$KEY_DELIM$SIGN_FN")

      /** Десериализация. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, ActorPathQs]] = {
        // Собираем результат
        val k = key1F(key)
        for {
          params    <- getQsbSigner(key)
            .signedOrNone(k(""), params0)
          maybePath <- strB.bind(k(PATH_FN), params)
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
        val qsRaw = strB.unbind(
          key   = key1(key, PATH_FN),
          value = value.path.toSerializationFormat
        )
        getQsbSigner(key)
          .mkSigned(key, qsRaw)
      }
    }
  }

}


/**
 * Экземпляр модели.
 * @param path Путь до актора.
 */
case class ActorPathQs(path: ActorPath)
