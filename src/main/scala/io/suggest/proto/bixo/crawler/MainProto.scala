package io.suggest.proto.bixo.crawler

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.13 18:10
 * Description: Тут сообщения для общения с main-кравлером.
 */

object MainProto {
  val NAME = "main"

  type MajorRebuildReply_t = Either[String, String]
}

case object MajorRebuildMsg extends Serializable

/**
 * Веб-морда перекидывает пачку награбленных referrer'ов в main-кравлер.
 * Кравлер должен проверять ссылки на принадлежность к dkeys самостоятельно.
 * @param urls Ссылки на страницы.
 */
case class ReferrersBulk(urls: List[String]) extends Serializable
