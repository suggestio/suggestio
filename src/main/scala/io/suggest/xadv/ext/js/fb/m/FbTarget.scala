package io.suggest.xadv.ext.js.fb.m

import java.net.URI

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 13:48
 * Description: Модель представления цели размещения на основе ссылки.
 */
object FbTarget extends FromJsonT {

  /** Название поля значения nodeId в JSON-представлении. */
  def NODE_ID_FN   = "i"

  /** Название необязательного поля типа узла fb-графа. */
  def NODE_TYPE_FN = "t"

  override type T = FbTarget

  /**
   * Десериализовать из URL.
   * @param url Ссылка.
   * @return None если ссылка непонятна совсем.
   *         Some([[FbTarget]]), если удалось извлечь какую-то инфу из ссылки.
   */
  def fromUrl(url: String): Option[FbTarget] = {
    val uri = new URI(url)
    val path = uri.getPath + "?" + uri.getQuery   // TODO Должен же быть getFile() какой-нить для URI!
    // Быстрая разборка path по регэкспу
    def unapplyIdRe(re: String): Option[String] = {
      re.r.unapplySeq(path)
        .flatMap(_.lastOption)
    }
    // Попробовать разные варианты извлечения id из tg url path.
    // начинаем с группы, хотя TODO наверное лучше бы со страницы или профиля юзера
    unapplyIdRe("/groups?/([^/?&]+).*")
      .map { FbTarget(_, Some(FbNodeTypes.Group)) }
      .orElse {
        unapplyIdRe("/events?/([^/?&]+).*")
          .map { FbTarget(_, Some(FbNodeTypes.Event)) }
      }
      // Тестируем на страницу
      .orElse {
        unapplyIdRe("/pages?/[^/]+/([^/?&]+).*")
          .map { FbTarget(_, Some(FbNodeTypes.Page)) }
      }
      // Тестируем на юзера
      .orElse {
        unapplyIdRe("/profiles?\\.php\\?(.+?&)?id=([0-9]+).*")
          .orElse { if (path matches "^/?([?#].*)?") Some("me") else None }
          .map { FbTarget(_, Some(FbNodeTypes.User)) }
      }
      // Тестируем на наличие короткого имени узла в пути.
      .orElse {
        unapplyIdRe("/([^/?&]+).*")
          .map { FbTarget(_, None) }
      }
  }

  /** Десериализация из переносимого JSON-представления. */
  override def fromJson(raw: Any): T = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    FbTarget(
      nodeId    = d(NODE_ID_FN).toString,
      nodeType  = d.get(NODE_TYPE_FN)
        .map(_.toString)
        .flatMap(FbNodeTypes.maybeWithName)
    )
  }

}


/** Интерфейс экземпляра модели с полем id fb-узла. */
trait IFbNodeId {

  /** Идентификатор узла графа Facebook. */
  def nodeId: String

}


import FbTarget._


/**
 * Инфа по запрошенному узлу фейсбука.
 * @param nodeId fb id узла.
 * @param nodeType Тип узла, если известен.
 */
case class FbTarget(
  nodeId    : String,
  nodeType  : Option[FbNodeType]
) extends IFbNodeId with IToJsonDict {

  /** Сериализация в переносимое JSON-представление. */
  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any](
      NODE_ID_FN -> nodeId
    )
    if (nodeType.isDefined)
      d.update(NODE_TYPE_FN, nodeType.get.mdType)
    d
  }
}

