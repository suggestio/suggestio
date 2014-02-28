package models

import io.suggest.model.{EsModel, EsModelMinimalStaticT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.14 17:43
 * Description:
 */
object SiowebEsModel {

  def ES_MODELS: Seq[EsModelMinimalStaticT[_]] = {
    Seq(MBlog, MMartCategory) ++ EsModel.ES_MODELS
  }

}
