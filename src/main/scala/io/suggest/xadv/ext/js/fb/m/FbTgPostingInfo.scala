package io.suggest.xadv.ext.js.fb.m

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 18:46
 * Description: Модель важной инфы для постинга в facebook.
 */

trait IFbPostingInfo extends IFbAccessTokenOpt with IFbNodeId

case class FbTgPostingInfo(
  nodeId      : String,
  accessToken : Option[String]
) extends IFbPostingInfo
