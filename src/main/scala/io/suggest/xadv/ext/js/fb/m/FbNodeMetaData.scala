package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.FromJsonT

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 17:54
 * Description: Модель метаданных fb-узла.
 */

object FbNodeMetaData extends FromJsonT {
  override type T = FbNodeMetaData

  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbNodeMetaData(
      nodeTypeStr = d.get("type").map(_.toString)
    )
  }
}

case class FbNodeMetaData(
  nodeTypeStr : Option[String]
) {

  def nodeType = nodeTypeStr.flatMap(FbNodeTypes.maybeWithName)

}
