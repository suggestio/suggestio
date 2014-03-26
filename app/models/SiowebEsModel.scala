package models

import io.suggest.model.{EsModel, EsModelMinimalStaticT}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description: Дополнительная утиль для ES-моделей.
 */
object SiowebEsModel {

  /**
   * Список моделей, которые должны быть проинициалированы при старте.
   * @return Список EsModelMinimalStaticT.
   */
  def ES_MODELS: Seq[EsModelMinimalStaticT[_]] = {
    Seq(MBlog, MPerson, MozillaPersonaIdent, EmailPwIdent, EmailActivation, MMartCategory) ++
      EsModel.ES_MODELS
  }

  def putAllMappings(implicit ec: ExecutionContext, client: Client): Future[Boolean] = {
    val ignoreExist = current.configuration.getBoolean("es.mapping.model.ignore_exist") getOrElse false
    EsModel.putAllMappings(ES_MODELS, ignoreExist)
  }

}
