package models.adv.ext.act

import akka.actor.ActorPath
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sec.m.SecretGetter
import io.suggest.util.logs.MacroLogsDyn
import play.api.mvc.QueryStringBindable
import util.qsb.QsbSigner

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 16:57
 * Description: Бывает, что нужно передавать actor path в URL. Тут qsb-модель для этой задачи.
 */
object ActorPathQs extends MacroLogsDyn {

  // Суффиксы имен qs-аргументов
  def PATH_FN  = "p"
  def SIGN_FN  = "sig"

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
