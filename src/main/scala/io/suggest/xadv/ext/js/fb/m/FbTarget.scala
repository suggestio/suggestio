package io.suggest.xadv.ext.js.fb.m

import java.net.URI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 13:48
 * Description: Модель представления цели размещения на основе ссылки.
 */
object FbTarget {

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

}


/** Интерфейс экземпляра модели с полем id fb-узла. */
trait IFbNodeId {

  /** Идентификатор узла графа Facebook. */
  def nodeId: String

}


/**
 * Инфа по запрошенному узлу фейсбука.
 * @param nodeId fb id узла.
 * @param nodeType Тип узла, если известен.
 */
case class FbTarget(nodeId: String, nodeType: Option[FbNodeType]) extends IFbNodeId

