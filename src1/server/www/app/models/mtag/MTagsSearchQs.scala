package models.mtag

import io.suggest.model.play.qsb.{QsbUtil, QueryStringBindableImpl}
import io.suggest.tags.MTagsSearchQs
import io.suggest.tags.TagSearchConstants.Req._
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.15 17:10
 * Description: Модель аргументов поискового запроса тегов, например в поисковой выдаче.
 * Поиск происходит по узлам графа N2, где теги -- лишь частный случай вершин.
 */
object MTagsSearchQsJvm {

  /** Поддержка интеграции с play-роутером в области URL Query string. */
  implicit def mLkTagsSearchQsQsb(implicit
                                  strB       : QueryStringBindable[String],
                                  intOptB    : QueryStringBindable[Option[Int]]
                                 ): QueryStringBindable[MTagsSearchQs] = {

    new QueryStringBindableImpl[MTagsSearchQs] {

      /** Биндинг значения [[MTagsSearchQs]] из URL qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MTagsSearchQs]] = {
        val k = key1F(key)
        for {
          tagsQueryE           <- strB.bind    (k(FACE_FTS_QUERY_FN),  params)
          limitOptE            <- intOptB.bind (k(LIMIT_FN),           params)
          offsetOptE           <- intOptB.bind (k(OFFSET_FN),          params)
        } yield {
          // TODO Нужно избегать пустого критерия поиска, т.е. возвращать None, когда нет параметров для поиска.
          for {
            _tagsQuery      <- QsbUtil.ensureStrLen(tagsQueryE, 1, TAGS_QUERY_MAXLEN)
            _limitOpt       <- QsbUtil.ensureIntOptRange(limitOptE, 1, LIMIT_MAX)
            _offsetOpt      <- QsbUtil.ensureIntOptRange(offsetOptE, 0, OFFSET_MAX)
          } yield {
            MTagsSearchQs(
              faceFts = _tagsQuery,
              limit   = _limitOpt,
              offset  = _offsetOpt
            )
          }
        }
      }

      /** Разбиндивание значения [[MTagsSearchQs]] в URL qs. */
      override def unbind(key: String, value: MTagsSearchQs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          strB.unbind     (k(FACE_FTS_QUERY_FN),  value.faceFts ),
          intOptB.unbind  (k(LIMIT_FN),           value.limit  ),
          intOptB.unbind  (k(OFFSET_FN),          value.offset )
        )
      }
    }
  }

}
