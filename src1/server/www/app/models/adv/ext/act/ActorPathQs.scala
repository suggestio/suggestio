package models.adv.ext.act

import akka.actor.ActorPath
import io.suggest.common.qs.QsConstants
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import io.suggest.util.logs.MacroLogsDyn
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.mvc.QueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 16:57
 * Description: Бывает, что нужно передавать actor path в URL. Тут qsb-модель для этой задачи.
 */
object ActorPathQs extends MacroLogsDyn with SecretKeyInit {

  private var SIGN_SECRET: String = _

  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  // Суффиксы имен qs-аргументов
  def PATH_FN  = "p"
  def SIGN_FN  = "sig"

  override def CONF_KEY = "qsb.actor.path.sign.key"

  /** qsb для извлечения нотариально заверенного путя из URL qs. */
  implicit def actorPathQsQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[ActorPathQs] = {
    new AbstractQueryStringBindable[ActorPathQs] {

      /** Фасад для работы с секретным ключом подписи. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key${QsConstants.KEY_PARTS_DELIM_STR}$SIGN_FN")

      /** Десериализация. */
      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, ActorPathQs]] = {
        // Собираем результат
        val k = key1F(key)
        for {
          params    <- getQsbSigner(key)
            .signedOrNone(k(""), params0)
          maybePath <- strB.bind(k(PATH_FN), params)
        } yield {
          maybePath.flatMap { path =>
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
