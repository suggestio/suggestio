package io.suggest.xadv.ext.js.fb.m

import java.net.URI

import io.suggest.model.LightEnumeration

import scala.util.matching.Regex

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
   * @return Экземпляр [[IFbTarget]].
   */
  def fromUrl(url: String): IFbTarget = {
    val uri = new URI(url)
    val path = uri.getPath + "?" + uri.getQuery
    // Быстрая разборка path по регэкспу
    def unapplyIdRe(re: String): Option[String] = {
      re.r.unapplySeq(path)
        .flatMap(_.lastOption)
    }
    // Попробовать разные варианты извлечения id из tg url path.
    val id = unapplyIdRe("/groups?/([^/?&]+).*")
      .orElse { unapplyIdRe("/events?/([^/?&]+).*") }
      .orElse { unapplyIdRe("/pages?/[^/]+/([^/?&]+).*") }
      .orElse { unapplyIdRe("/profiles?\\.php\\?(.+?&)?id=([0-9]+).*") }
      .orElse { unapplyIdRe("/([^/?&]+).*") }
      // TODO Нужен более гибкий обработчик ссылки на главную. Чтобы отлавливал / и ?
      .orElse { if (path == "/") Some("me") else None }
      // TODO Надо отрабатывать/возвращать ошибки, а не гасить их через подстановку "/me".
      .getOrElse("me")
    FbId(id = id)
  }

}


/** Интерфейс экземпляра модели. */
trait IFbTarget {
  /** Идентификатор объекта. */
  def id: String
}

/** Группа фейсбука и её id. */
case class FbId(id: String) extends IFbTarget
